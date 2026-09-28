package com.example.shopping.member.service;

import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.common.enums.Role;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.dto.request.ChangePasswordRequest;
import com.example.shopping.member.dto.request.ForgotPasswordRequest;
import com.example.shopping.member.dto.request.LoginRequest;
import com.example.shopping.member.dto.request.MemberStatusRequest;
import com.example.shopping.member.dto.request.MemberUpdateRequest;
import com.example.shopping.member.dto.request.RefreshTokenRequest;
import com.example.shopping.member.dto.request.RegisterRequest;
import com.example.shopping.member.dto.request.ResendVerificationRequest;
import com.example.shopping.member.dto.request.ResetPasswordRequest;
import com.example.shopping.member.dto.request.VerifyEmailRequest;
import com.example.shopping.member.dto.response.LoginResponse;
import com.example.shopping.member.dto.response.MemberResponse;
import com.example.shopping.member.entity.EmailVerificationToken;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.entity.PasswordResetToken;
import com.example.shopping.member.entity.RefreshToken;
import com.example.shopping.member.mail.EmailSendLimiter;
import com.example.shopping.member.mail.EmailVerificationMailSender;
import com.example.shopping.member.mail.PasswordResetMailSender;
import com.example.shopping.member.repository.EmailVerificationTokenRepository;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.member.repository.PasswordResetTokenRepository;
import com.example.shopping.member.repository.RefreshTokenRepository;
import com.example.shopping.security.JwtTokenProvider;
import com.example.shopping.security.LoginAttemptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;

import static com.example.shopping.member.repository.MemberSpecifications.hasStatus;
import static com.example.shopping.member.repository.MemberSpecifications.keywordMatches;

@Service
@Transactional
public class MemberServiceImpl implements MemberService {

    private static final int RESET_TOKEN_EXPIRY_MINUTES = 30;
    private static final int EMAIL_VERIFICATION_EXPIRY_HOURS = 24;

    private static final Logger log = LoggerFactory.getLogger(MemberServiceImpl.class);
    private static final String LOGIN_SCOPE = "member";

    private final MemberRepository memberRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordResetMailSender passwordResetMailSender;
    private final EmailVerificationMailSender emailVerificationMailSender;
    private final LoginAttemptService loginAttemptService;
    private final EmailSendLimiter emailSendLimiter;
    private final String frontendOrigin;
    private final long refreshExpirationMs;

    public MemberServiceImpl(MemberRepository memberRepository,
                              PasswordResetTokenRepository passwordResetTokenRepository,
                              RefreshTokenRepository refreshTokenRepository,
                              EmailVerificationTokenRepository emailVerificationTokenRepository,
                              PasswordEncoder passwordEncoder,
                              JwtTokenProvider jwtTokenProvider,
                              PasswordResetMailSender passwordResetMailSender,
                              EmailVerificationMailSender emailVerificationMailSender,
                              LoginAttemptService loginAttemptService,
                              EmailSendLimiter emailSendLimiter,
                              @Value("${app.cors.allowed-origins}") String frontendOrigin,
                              @Value("${jwt.refresh-expiration-ms}") long refreshExpirationMs) {
        this.memberRepository = memberRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordResetMailSender = passwordResetMailSender;
        this.emailVerificationMailSender = emailVerificationMailSender;
        this.loginAttemptService = loginAttemptService;
        this.emailSendLimiter = emailSendLimiter;
        this.frontendOrigin = frontendOrigin;
        this.refreshExpirationMs = refreshExpirationMs;
    }

    @Override
    public MemberResponse register(RegisterRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("此 Email 已被註冊");
        }

        Member member = new Member();
        member.setEmail(request.getEmail());
        member.setPassword(passwordEncoder.encode(request.getPassword()));
        member.setName(request.getName());
        member.setPhone(request.getPhone());
        member.setReferredById(resolveReferrer(request.getReferralCode()));

        Member saved = memberRepository.save(member);
        sendVerificationEmail(saved);
        return MemberResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        loginAttemptService.checkNotLocked(LOGIN_SCOPE, request.getEmail());

        Member member = memberRepository.findByEmail(request.getEmail()).orElse(null);
        if (member == null || !passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            loginAttemptService.recordFailure(LOGIN_SCOPE, request.getEmail());
            throw new BusinessException("帳號或密碼錯誤");
        }
        loginAttemptService.recordSuccess(LOGIN_SCOPE, request.getEmail());

        if (member.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("帳號已被停用,請聯繫客服");
        }
        if (!member.isEmailVerified()) {
            throw new BusinessException("請先完成 Email 驗證,請查看您的收件匣");
        }

        String token = jwtTokenProvider.generateToken(member.getId(), member.getEmail(), Role.MEMBER);
        String refreshToken = issueRefreshToken(member.getId());
        return LoginResponse.of(token, refreshToken, member.getId(), member.getName(), member.getEmail());
    }

    @Override
    public LoginResponse refresh(RefreshTokenRequest request) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new BusinessException("請重新登入", HttpStatus.UNAUTHORIZED));

        if (storedToken.isRevoked() || storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("請重新登入", HttpStatus.UNAUTHORIZED);
        }

        Member member = findMemberOrThrow(storedToken.getMemberId());
        if (member.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("帳號已被停用,請聯繫客服", HttpStatus.UNAUTHORIZED);
        }

        // 輪替:換發新 refresh token 前先讓舊的失效,降低外洩後被重複利用的風險
        storedToken.setRevoked(true);
        String newRefreshToken = issueRefreshToken(member.getId());
        String newAccessToken = jwtTokenProvider.generateToken(member.getId(), member.getEmail(), Role.MEMBER);
        return LoginResponse.of(newAccessToken, newRefreshToken, member.getId(), member.getName(), member.getEmail());
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        refreshTokenRepository.findByToken(request.getRefreshToken())
                .ifPresent(storedToken -> storedToken.setRevoked(true));
    }

    @Override
    public void verifyEmail(VerifyEmailRequest request) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BusinessException("驗證連結無效或已過期"));

        if (verificationToken.isUsed() || verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("驗證連結無效或已過期");
        }

        Member member = findMemberOrThrow(verificationToken.getMemberId());
        member.setEmailVerified(true);
        verificationToken.setUsed(true);
    }

    @Override
    public void resendVerification(ResendVerificationRequest request) {
        // 不論 email 是否存在、是否已驗證都視為成功,避免被用來探測已註冊帳號;
        // 超過寄送頻率時同樣回成功但不寄信,不給攻擊者任何回饋
        if (!emailSendLimiter.tryAcquire("verification", request.getEmail())) {
            log.warn("重寄驗證信過於頻繁,已略過:{}", request.getEmail());
            return;
        }
        memberRepository.findByEmail(request.getEmail())
                .filter(member -> !member.isEmailVerified())
                .ifPresent(this::sendVerificationEmail);
    }

    private void sendVerificationEmail(Member member) {
        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setMemberId(member.getId());
        verificationToken.setToken(UUID.randomUUID().toString());
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(EMAIL_VERIFICATION_EXPIRY_HOURS));
        emailVerificationTokenRepository.save(verificationToken);

        String verifyLink = frontendOrigin + "/verify-email?token=" + verificationToken.getToken();
        emailVerificationMailSender.sendVerificationLink(member.getEmail(), verifyLink);
    }

    private String issueRefreshToken(Long memberId) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setMemberId(memberId);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshExpirationMs)));
        return refreshTokenRepository.save(refreshToken).getToken();
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        // 不論 email 是否存在都視為成功,避免被用來探測已註冊帳號;
        // 超過寄送頻率時同樣回成功但不寄信
        if (!emailSendLimiter.tryAcquire("password-reset", request.getEmail())) {
            log.warn("忘記密碼信件過於頻繁,已略過:{}", request.getEmail());
            return;
        }
        memberRepository.findByEmail(request.getEmail()).ifPresent(member -> {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setMemberId(member.getId());
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES));
            passwordResetTokenRepository.save(resetToken);

            String resetLink = frontendOrigin + "/reset-password?token=" + resetToken.getToken();
            passwordResetMailSender.sendResetLink(member.getEmail(), resetLink);
        });
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new BusinessException("重設密碼連結無效或已過期"));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("重設密碼連結無效或已過期");
        }

        Member member = findMemberOrThrow(resetToken.getMemberId());
        member.setPassword(passwordEncoder.encode(request.getNewPassword()));
        resetToken.setUsed(true);
        // 重設密碼常是帳號疑似被盜的補救,舊的登入狀態一律作廢
        refreshTokenRepository.revokeAllByMemberId(member.getId());
    }

    @Override
    public void changePassword(Long memberId, ChangePasswordRequest request) {
        Member member = findMemberOrThrow(memberId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), member.getPassword())) {
            throw new BusinessException("目前密碼不正確");
        }
        if (passwordEncoder.matches(request.getNewPassword(), member.getPassword())) {
            throw new BusinessException("新密碼不可與目前密碼相同");
        }
        member.setPassword(passwordEncoder.encode(request.getNewPassword()));
        refreshTokenRepository.revokeAllByMemberId(memberId);
        passwordResetMailSender.sendPasswordChangedNotice(member.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponse getProfile(Long memberId) {
        return MemberResponse.from(findMemberOrThrow(memberId));
    }

    @Override
    public MemberResponse updateProfile(Long memberId, MemberUpdateRequest request) {
        Member member = findMemberOrThrow(memberId);
        member.setName(request.getName());
        member.setPhone(request.getPhone());
        if (request.getBirthday() != null) {
            applyBirthday(member, request.getBirthday(), LocalDate.now());
        }
        return MemberResponse.from(member);
    }

    /** 邀請碼不分大小寫;填了但找不到(或邀請人已停用)就擋下,避免會員以為有綁定成功 */
    private Long resolveReferrer(String referralCode) {
        if (referralCode == null || referralCode.isBlank()) {
            return null;
        }
        return memberRepository.findActiveByReferralCode(referralCode.trim().toUpperCase(Locale.ROOT))
                .map(Member::getId)
                .orElseThrow(() -> new BusinessException("邀請碼無效,請確認後再試,或留空直接註冊"));
    }

    /**
     * 生日只能設定一次。在生日當月才設定的,今年的生日禮視為已領(明年起才發),
     * 避免註冊新帳號、把生日填成本月就能立刻領購物金。
     */
    void applyBirthday(Member member, LocalDate birthday, LocalDate today) {
        if (member.getBirthday() != null) {
            if (!member.getBirthday().equals(birthday)) {
                throw new BusinessException("生日設定後無法修改,如需更正請聯絡客服");
            }
            return;
        }
        if (birthday.getYear() < 1900 || !birthday.isBefore(today)) {
            throw new BusinessException("生日日期不正確");
        }
        member.setBirthday(birthday);
        if (birthday.getMonth() == today.getMonth()) {
            memberRepository.claimBirthdayReward(member.getId(), today.getYear());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<MemberResponse> listAdmin(AccountStatus status, String keyword, Pageable pageable) {
        Specification<Member> spec = Specification.where(hasStatus(status)).and(keywordMatches(keyword));
        return memberRepository.findAll(spec, pageable).map(MemberResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public MemberResponse getAdminDetail(Long memberId) {
        return MemberResponse.from(findMemberOrThrow(memberId));
    }

    @Override
    public MemberResponse updateStatus(Long memberId, MemberStatusRequest request) {
        Member member = findMemberOrThrow(memberId);
        member.setStatus(request.getStatus());
        return MemberResponse.from(member);
    }

    private Member findMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("會員不存在"));
    }
}
