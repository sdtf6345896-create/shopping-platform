package com.example.shopping.member.service;

import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.common.enums.Role;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
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
import com.example.shopping.member.mail.EmailVerificationMailSender;
import com.example.shopping.member.mail.PasswordResetMailSender;
import com.example.shopping.member.repository.EmailVerificationTokenRepository;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.member.repository.PasswordResetTokenRepository;
import com.example.shopping.member.repository.RefreshTokenRepository;
import com.example.shopping.security.JwtTokenProvider;
import com.example.shopping.security.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private PasswordResetMailSender passwordResetMailSender;
    @Mock
    private EmailVerificationMailSender emailVerificationMailSender;

    private MemberServiceImpl memberService;

    private Member activeMember;

    @BeforeEach
    void setUp() {
        // 建構子帶有 String/long 這類非 mock 參數,Mockito 的 @InjectMocks 無法可靠處理,改手動 new
        memberService = new MemberServiceImpl(memberRepository, passwordResetTokenRepository,
                refreshTokenRepository, emailVerificationTokenRepository, passwordEncoder, jwtTokenProvider,
                passwordResetMailSender, emailVerificationMailSender,
                new LoginAttemptService(3, 15), "http://localhost:5173", 1209600000L);

        activeMember = new Member();
        activeMember.setId(1L);
        activeMember.setEmail("test@example.com");
        activeMember.setPassword("encoded-password");
        activeMember.setName("測試會員");
        activeMember.setStatus(AccountStatus.ACTIVE);
        activeMember.setEmailVerified(true);
    }

    @Test
    void register_savesEncodedPassword_whenEmailNotTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("new@example.com");
        request.setPassword("password123");
        request.setName("新會員");

        when(memberRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashed");
        when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

        MemberResponse response = memberService.register(request);

        assertThat(response.getEmail()).isEqualTo("new@example.com");
        verify(memberRepository).save(argThatPasswordEquals("hashed"));
        verify(emailVerificationTokenRepository).save(any(EmailVerificationToken.class));
        verify(emailVerificationMailSender).sendVerificationLink(
                eq("new@example.com"), argThat(link -> link.startsWith("http://localhost:5173/verify-email?token=")));
    }

    @Test
    void register_throws_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");
        request.setName("重複會員");

        when(memberRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> memberService.register(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已被註冊");
        verify(memberRepository, never()).save(any());
    }

    @Test
    void login_returnsToken_whenCredentialsValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeMember));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "test@example.com", Role.MEMBER)).thenReturn("token-abc");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        LoginResponse response = memberService.login(request);

        assertThat(response.getToken()).isEqualTo("token-abc");
        assertThat(response.getMemberId()).isEqualTo(1L);
        assertThat(response.getRefreshToken()).isNotBlank();
    }

    @Test
    void login_throws_whenPasswordWrong() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("wrong");

        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeMember));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("帳號或密碼錯誤");
    }

    @Test
    void login_locksAccount_afterRepeatedFailures_evenWithCorrectPassword() {
        LoginRequest wrong = new LoginRequest();
        wrong.setEmail("test@example.com");
        wrong.setPassword("wrong");
        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeMember));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        // setUp 設定 3 次失敗就鎖定
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> memberService.login(wrong)).hasMessageContaining("帳號或密碼錯誤");
        }

        LoginRequest correct = new LoginRequest();
        correct.setEmail("TEST@example.com");
        correct.setPassword("password123");
        assertThatThrownBy(() -> memberService.login(correct))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("暫時鎖定")
                .extracting(ex -> ((BusinessException) ex).getStatus())
                .isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        verify(passwordEncoder, never()).matches(eq("password123"), any());
    }

    @Test
    void login_countsFailures_forUnknownEmail() {
        LoginRequest request = new LoginRequest();
        request.setEmail("nobody@example.com");
        request.setPassword("whatever");
        when(memberRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> memberService.login(request)).hasMessageContaining("帳號或密碼錯誤");
        }

        assertThatThrownBy(() -> memberService.login(request)).hasMessageContaining("暫時鎖定");
    }

    @Test
    void login_throws_whenAccountDisabled() {
        // 回歸測試:登入原本沒檢查 status,停用帳號還是能登入,後來才補上這個檢查
        activeMember.setStatus(AccountStatus.DISABLED);
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeMember));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已被停用");
        verify(jwtTokenProvider, never()).generateToken(any(), any(), any());
    }

    @Test
    void login_throws_whenEmailNotVerified() {
        activeMember.setEmailVerified(false);
        LoginRequest request = new LoginRequest();
        request.setEmail("test@example.com");
        request.setPassword("password123");

        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeMember));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> memberService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email 驗證");
        verify(jwtTokenProvider, never()).generateToken(any(), any(), any());
    }

    @Test
    void verifyEmail_marksMemberVerified_whenTokenValid() {
        activeMember.setEmailVerified(false);
        EmailVerificationToken token = new EmailVerificationToken();
        token.setMemberId(1L);
        token.setToken("valid-verify-token");
        token.setExpiresAt(LocalDateTime.now().plusHours(1));

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken("valid-verify-token");

        when(emailVerificationTokenRepository.findByToken("valid-verify-token")).thenReturn(Optional.of(token));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember));

        memberService.verifyEmail(request);

        assertThat(activeMember.isEmailVerified()).isTrue();
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    void verifyEmail_throws_whenTokenNotFound() {
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken("missing-token");

        when(emailVerificationTokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.verifyEmail(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("無效或已過期");
    }

    @Test
    void verifyEmail_throws_whenTokenExpired() {
        EmailVerificationToken token = new EmailVerificationToken();
        token.setMemberId(1L);
        token.setToken("expired-token");
        token.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken("expired-token");

        when(emailVerificationTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(token));

        assertThatThrownBy(() -> memberService.verifyEmail(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("無效或已過期");
        verify(memberRepository, never()).findById(any());
    }

    @Test
    void resendVerification_sendsEmail_whenNotVerified() {
        activeMember.setEmailVerified(false);
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("test@example.com");

        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeMember));

        memberService.resendVerification(request);

        verify(emailVerificationMailSender).sendVerificationLink(eq("test@example.com"), any());
    }

    @Test
    void resendVerification_doesNothing_whenAlreadyVerified() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("test@example.com");

        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeMember));

        memberService.resendVerification(request);

        verify(emailVerificationMailSender, never()).sendVerificationLink(any(), any());
    }

    @Test
    void resendVerification_doesNothing_whenEmailNotFound() {
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("unknown@example.com");

        when(memberRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        memberService.resendVerification(request);

        verify(emailVerificationMailSender, never()).sendVerificationLink(any(), any());
    }

    @Test
    void refresh_returnsNewTokenPair_whenValid() {
        RefreshToken storedToken = new RefreshToken();
        storedToken.setMemberId(1L);
        storedToken.setToken("valid-refresh");
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(1));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh");

        when(refreshTokenRepository.findByToken("valid-refresh")).thenReturn(Optional.of(storedToken));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember));
        when(jwtTokenProvider.generateToken(1L, "test@example.com", Role.MEMBER)).thenReturn("new-access-token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(inv -> inv.getArgument(0));

        LoginResponse response = memberService.refresh(request);

        assertThat(response.getToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isNotBlank().isNotEqualTo("valid-refresh");
        assertThat(storedToken.isRevoked()).isTrue();
    }

    @Test
    void refresh_throws_whenTokenNotFound() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("missing-refresh");

        when(refreshTokenRepository.findByToken("missing-refresh")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重新登入");
    }

    @Test
    void refresh_throws_whenTokenExpired() {
        RefreshToken storedToken = new RefreshToken();
        storedToken.setMemberId(1L);
        storedToken.setToken("expired-refresh");
        storedToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired-refresh");

        when(refreshTokenRepository.findByToken("expired-refresh")).thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> memberService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重新登入");
        verify(memberRepository, never()).findById(any());
    }

    @Test
    void refresh_throws_whenTokenRevoked() {
        RefreshToken storedToken = new RefreshToken();
        storedToken.setMemberId(1L);
        storedToken.setToken("revoked-refresh");
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(1));
        storedToken.setRevoked(true);

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("revoked-refresh");

        when(refreshTokenRepository.findByToken("revoked-refresh")).thenReturn(Optional.of(storedToken));

        assertThatThrownBy(() -> memberService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("重新登入");
    }

    @Test
    void refresh_throws_whenAccountDisabled() {
        activeMember.setStatus(AccountStatus.DISABLED);
        RefreshToken storedToken = new RefreshToken();
        storedToken.setMemberId(1L);
        storedToken.setToken("valid-refresh");
        storedToken.setExpiresAt(LocalDateTime.now().plusDays(1));

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh");

        when(refreshTokenRepository.findByToken("valid-refresh")).thenReturn(Optional.of(storedToken));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember));

        assertThatThrownBy(() -> memberService.refresh(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已被停用");
        verify(jwtTokenProvider, never()).generateToken(any(), any(), any());
    }

    @Test
    void logout_revokesToken_whenFound() {
        RefreshToken storedToken = new RefreshToken();
        storedToken.setMemberId(1L);
        storedToken.setToken("active-refresh");

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("active-refresh");

        when(refreshTokenRepository.findByToken("active-refresh")).thenReturn(Optional.of(storedToken));

        memberService.logout(request);

        assertThat(storedToken.isRevoked()).isTrue();
    }

    @Test
    void logout_doesNothing_whenTokenNotFound() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("missing-refresh");

        when(refreshTokenRepository.findByToken("missing-refresh")).thenReturn(Optional.empty());

        memberService.logout(request);
    }

    @Test
    void forgotPassword_savesToken_whenEmailExists() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");

        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeMember));

        memberService.forgotPassword(request);

        verify(passwordResetTokenRepository).save(argThat(t -> t.getMemberId().equals(1L) && t.getToken() != null));
        verify(passwordResetMailSender).sendResetLink(
                eq("test@example.com"), argThat(link -> link.startsWith("http://localhost:5173/reset-password?token=")));
    }

    @Test
    void forgotPassword_doesNothing_whenEmailNotFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("unknown@example.com");

        when(memberRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        memberService.forgotPassword(request);

        verify(passwordResetTokenRepository, never()).save(any());
        verify(passwordResetMailSender, never()).sendResetLink(any(), any());
    }

    @Test
    void resetPassword_updatesPassword_whenTokenValid() {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setMemberId(1L);
        resetToken.setToken("valid-token");
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("newPassword123");

        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(resetToken));
        when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember));
        when(passwordEncoder.encode("newPassword123")).thenReturn("new-encoded-password");

        memberService.resetPassword(request);

        assertThat(activeMember.getPassword()).isEqualTo("new-encoded-password");
        assertThat(resetToken.isUsed()).isTrue();
    }

    @Test
    void resetPassword_throws_whenTokenNotFound() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("missing-token");
        request.setNewPassword("newPassword123");

        when(passwordResetTokenRepository.findByToken("missing-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> memberService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("無效或已過期");
    }

    @Test
    void resetPassword_throws_whenTokenExpired() {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setMemberId(1L);
        resetToken.setToken("expired-token");
        resetToken.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");
        request.setNewPassword("newPassword123");

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> memberService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("無效或已過期");
        verify(memberRepository, never()).findById(any());
    }

    @Test
    void resetPassword_throws_whenTokenAlreadyUsed() {
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setMemberId(1L);
        resetToken.setToken("used-token");
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        resetToken.setUsed(true);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("used-token");
        request.setNewPassword("newPassword123");

        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(resetToken));

        assertThatThrownBy(() -> memberService.resetPassword(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("無效或已過期");
    }

    @Test
    void updateProfile_updatesNameAndPhone() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember));
        MemberUpdateRequest request = new MemberUpdateRequest();
        request.setName("改名後");
        request.setPhone("0999888777");

        MemberResponse response = memberService.updateProfile(1L, request);

        assertThat(response.getName()).isEqualTo("改名後");
        assertThat(response.getPhone()).isEqualTo("0999888777");
    }

    @Test
    void updateProfile_throws_whenMemberNotFound() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());
        MemberUpdateRequest request = new MemberUpdateRequest();

        assertThatThrownBy(() -> memberService.updateProfile(99L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateStatus_changesAccountStatus() {
        when(memberRepository.findById(1L)).thenReturn(Optional.of(activeMember));
        MemberStatusRequest request = new MemberStatusRequest();
        request.setStatus(AccountStatus.DISABLED);

        MemberResponse response = memberService.updateStatus(1L, request);

        assertThat(response.getStatus()).isEqualTo(AccountStatus.DISABLED);
    }

    private static Member argThatPasswordEquals(String expected) {
        return org.mockito.ArgumentMatchers.argThat(m -> m != null && expected.equals(m.getPassword()));
    }
}
