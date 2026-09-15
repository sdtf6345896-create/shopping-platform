package com.example.shopping.member.service;

import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.common.enums.Role;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.dto.request.LoginRequest;
import com.example.shopping.member.dto.request.MemberStatusRequest;
import com.example.shopping.member.dto.request.MemberUpdateRequest;
import com.example.shopping.member.dto.request.RegisterRequest;
import com.example.shopping.member.dto.response.LoginResponse;
import com.example.shopping.member.dto.response.MemberResponse;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MemberServiceImplTest {

    @Mock
    private MemberRepository memberRepository;
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
