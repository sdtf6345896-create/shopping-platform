package com.example.shopping.member.service;

import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.common.enums.Role;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.dto.request.ForgotPasswordRequest;
import com.example.shopping.member.dto.request.LoginRequest;
import com.example.shopping.member.dto.request.MemberStatusRequest;
import com.example.shopping.member.dto.request.MemberUpdateRequest;
import com.example.shopping.member.dto.request.RegisterRequest;
import com.example.shopping.member.dto.request.ResetPasswordRequest;
import com.example.shopping.member.dto.response.LoginResponse;
import com.example.shopping.member.dto.response.MemberResponse;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.entity.PasswordResetToken;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.member.repository.PasswordResetTokenRepository;
import com.example.shopping.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.example.shopping.member.repository.MemberSpecifications.hasStatus;
import static com.example.shopping.member.repository.MemberSpecifications.keywordMatches;

@Service
@Transactional
public class MemberServiceImpl implements MemberService {

    private static final Logger log = LoggerFactory.getLogger(MemberServiceImpl.class);
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 30;

    private final MemberRepository memberRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final String frontendOrigin;

    public MemberServiceImpl(MemberRepository memberRepository,
                              PasswordResetTokenRepository passwordResetTokenRepository,
                              PasswordEncoder passwordEncoder,
                              JwtTokenProvider jwtTokenProvider,
                              @Value("${app.cors.allowed-origins}") String frontendOrigin) {
        this.memberRepository = memberRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.frontendOrigin = frontendOrigin;
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

        return MemberResponse.from(memberRepository.save(member));
    }

    @Override
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Member member = memberRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException("帳號或密碼錯誤"));

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BusinessException("帳號或密碼錯誤");
        }
        if (member.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("帳號已被停用,請聯繫客服");
        }

        String token = jwtTokenProvider.generateToken(member.getId(), member.getEmail(), Role.MEMBER);
        return LoginResponse.of(token, member.getId(), member.getName(), member.getEmail());
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        // 不論 email 是否存在都視為成功,避免被用來探測已註冊帳號
        memberRepository.findByEmail(request.getEmail()).ifPresent(member -> {
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setMemberId(member.getId());
            resetToken.setToken(UUID.randomUUID().toString());
            resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES));
            passwordResetTokenRepository.save(resetToken);

            // 未串接真實 SMTP,以 log 模擬寄出重設密碼信(比照付款流程的模擬方式)
            log.info("[模擬寄信] 寄送密碼重設連結給 {}:{}/reset-password?token={}",
                    member.getEmail(), frontendOrigin, resetToken.getToken());
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
        return MemberResponse.from(member);
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
