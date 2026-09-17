package com.example.shopping.admin.service;

import com.example.shopping.admin.dto.request.AdminLoginRequest;
import com.example.shopping.admin.dto.response.AdminLoginResponse;
import com.example.shopping.admin.entity.Admin;
import com.example.shopping.admin.repository.AdminRepository;
import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.common.enums.Role;
import com.example.shopping.common.exception.BusinessException;
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
class AdminAuthServiceImplTest {

    @Mock
    private AdminRepository adminRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private AdminAuthServiceImpl adminAuthService;

    private Admin activeAdmin;

    @BeforeEach
    void setUp() {
        activeAdmin = new Admin();
        activeAdmin.setId(1L);
        activeAdmin.setUsername("admin");
        activeAdmin.setPassword("encoded-password");
        activeAdmin.setName("系統管理員");
        activeAdmin.setStatus(AccountStatus.ACTIVE);
    }

    @Test
    void login_returnsToken_whenCredentialsValid() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(activeAdmin));
        when(passwordEncoder.matches("admin123", "encoded-password")).thenReturn(true);
        when(jwtTokenProvider.generateToken(1L, "admin", Role.ADMIN)).thenReturn("token-abc");

        AdminLoginResponse response = adminAuthService.login(request);

        assertThat(response.getToken()).isEqualTo("token-abc");
        assertThat(response.getAdminId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("admin");
    }

    @Test
    void login_throws_whenUsernameNotFound() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("nobody");
        request.setPassword("admin123");

        when(adminRepository.findByUsername("nobody")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAuthService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("帳號或密碼錯誤");
    }

    @Test
    void login_throws_whenPasswordWrong() {
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("wrong");

        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(activeAdmin));
        when(passwordEncoder.matches("wrong", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> adminAuthService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("帳號或密碼錯誤");
    }

    @Test
    void login_throws_whenAccountDisabled() {
        activeAdmin.setStatus(AccountStatus.DISABLED);
        AdminLoginRequest request = new AdminLoginRequest();
        request.setUsername("admin");
        request.setPassword("admin123");

        when(adminRepository.findByUsername("admin")).thenReturn(Optional.of(activeAdmin));
        when(passwordEncoder.matches("admin123", "encoded-password")).thenReturn(true);

        assertThatThrownBy(() -> adminAuthService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("已被停用");
        verify(jwtTokenProvider, never()).generateToken(any(), any(), any());
    }
}
