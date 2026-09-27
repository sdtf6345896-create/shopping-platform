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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
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
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private MemberServiceImpl memberService;

    private Member activeMember;

    @BeforeEach
    void setUp() {
        activeMember = new Member();
        activeMember.setId(1L);
        activeMember.setEmail("test@example.com");
        activeMember.setPassword("encoded-password");
        activeMember.setName("測試會員");
        activeMember.setStatus(AccountStatus.ACTIVE);

        ReflectionTestUtils.setField(memberService, "frontendOrigin", "http://localhost:5173");
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

        LoginResponse response = memberService.login(request);

        assertThat(response.getToken()).isEqualTo("token-abc");
        assertThat(response.getMemberId()).isEqualTo(1L);
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
    void forgotPassword_savesToken_whenEmailExists() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");

        when(memberRepository.findByEmail("test@example.com")).thenReturn(Optional.of(activeMember));

        memberService.forgotPassword(request);

        verify(passwordResetTokenRepository).save(argThat(t -> t.getMemberId().equals(1L) && t.getToken() != null));
    }

    @Test
    void forgotPassword_doesNothing_whenEmailNotFound() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("unknown@example.com");

        when(memberRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        memberService.forgotPassword(request);

        verify(passwordResetTokenRepository, never()).save(any());
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
