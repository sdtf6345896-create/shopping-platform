package com.example.shopping.admin.account;

import com.example.shopping.admin.entity.Admin;
import com.example.shopping.admin.repository.AdminRepository;
import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.common.enums.Role;
import com.example.shopping.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminAccountServiceTest {

    @Mock
    private AdminRepository adminRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminAccountService service;
    private Admin otherAdmin;

    @BeforeEach
    void setUp() {
        service = new AdminAccountService(adminRepository, passwordEncoder);
        otherAdmin = new Admin();
        otherAdmin.setId(2L);
        otherAdmin.setUsername("other");
        otherAdmin.setName("另一位管理員");
        otherAdmin.setRole("ADMIN");
        lenient().when(adminRepository.findById(2L)).thenReturn(Optional.of(otherAdmin));
    }

    @Test
    void keepsAtLeastOneActiveAdmin() {
        when(adminRepository.countByRoleAndStatus("ADMIN", AccountStatus.ACTIVE)).thenReturn(1L);

        assertThatThrownBy(() -> service.update(1L, 2L, "另一位管理員", Role.STAFF, AccountStatus.ACTIVE))
                .isInstanceOf(BusinessException.class).hasMessageContaining("至少要保留");
        assertThatThrownBy(() -> service.update(1L, 2L, "另一位管理員", Role.ADMIN, AccountStatus.DISABLED))
                .isInstanceOf(BusinessException.class);
        assertThat(otherAdmin.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void allowsDemotingWhenAnotherAdminRemains() {
        when(adminRepository.countByRoleAndStatus("ADMIN", AccountStatus.ACTIVE)).thenReturn(2L);

        service.update(1L, 2L, "改名", Role.STAFF, AccountStatus.ACTIVE);

        assertThat(otherAdmin.getRole()).isEqualTo("STAFF");
        assertThat(otherAdmin.getName()).isEqualTo("改名");
    }

    @Test
    void rejectsMemberRole() {
        assertThatThrownBy(() -> service.update(1L, 2L, "x", Role.MEMBER, AccountStatus.ACTIVE))
                .isInstanceOf(BusinessException.class).hasMessageContaining("ADMIN 或 STAFF");
    }
}
