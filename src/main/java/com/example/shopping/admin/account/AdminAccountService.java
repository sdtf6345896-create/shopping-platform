package com.example.shopping.admin.account;

import com.example.shopping.admin.entity.Admin;
import com.example.shopping.admin.repository.AdminRepository;
import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.common.enums.Role;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 後台帳號管理(僅 ADMIN):新增管理員 / 客服、調整角色與狀態、重設密碼;以及每個人修改自己的密碼。
 * 為避免把自己或整個後台鎖在門外:不能調整自己的角色或狀態,也不能停用 / 降級最後一個啟用中的 ADMIN。
 */
@Service
@Transactional
public class AdminAccountService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAccountService(AdminRepository adminRepository, PasswordEncoder passwordEncoder) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public record AccountResponse(Long id, String username, String name, String role, AccountStatus status,
                                  LocalDateTime createdAt) {

        static AccountResponse from(Admin admin) {
            return new AccountResponse(admin.getId(), admin.getUsername(), admin.getName(), admin.getRole(),
                    admin.getStatus(), admin.getCreatedAt());
        }
    }

    @Transactional(readOnly = true)
    public AccountResponse me(Long adminId) {
        return AccountResponse.from(findOrThrow(adminId));
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> list() {
        return adminRepository.findAll(Sort.by("id")).stream().map(AccountResponse::from).toList();
    }

    public AccountResponse create(String username, String name, String password, Role role) {
        requireBackOffice(role);
        String trimmed = username.trim();
        if (adminRepository.existsByUsername(trimmed)) {
            throw new BusinessException("帳號已存在");
        }
        Admin admin = new Admin();
        admin.setUsername(trimmed);
        admin.setName(name.trim());
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(role.name());
        return AccountResponse.from(adminRepository.save(admin));
    }

    public AccountResponse update(Long operatorId, Long adminId, String name, Role role, AccountStatus status) {
        requireBackOffice(role);
        Admin admin = findOrThrow(adminId);
        boolean changesAccess = !admin.getRole().equals(role.name()) || admin.getStatus() != status;
        if (changesAccess && adminId.equals(operatorId)) {
            throw new BusinessException("不能調整自己的角色或狀態,請由其他管理員操作");
        }
        boolean losesAdmin = Role.ADMIN.name().equals(admin.getRole()) && admin.getStatus() == AccountStatus.ACTIVE
                && (role != Role.ADMIN || status != AccountStatus.ACTIVE);
        if (losesAdmin && adminRepository.countByRoleAndStatus(Role.ADMIN.name(), AccountStatus.ACTIVE) <= 1) {
            throw new BusinessException("至少要保留一個啟用中的管理員");
        }
        admin.setName(name.trim());
        admin.setRole(role.name());
        admin.setStatus(status);
        return AccountResponse.from(admin);
    }

    /** 管理員替別人重設密碼 */
    public void resetPassword(Long adminId, String newPassword) {
        findOrThrow(adminId).setPassword(passwordEncoder.encode(newPassword));
    }

    /** 自己改密碼,需驗證目前密碼 */
    public void changeOwnPassword(Long adminId, String currentPassword, String newPassword) {
        Admin admin = findOrThrow(adminId);
        if (!passwordEncoder.matches(currentPassword, admin.getPassword())) {
            throw new BusinessException("目前密碼不正確");
        }
        admin.setPassword(passwordEncoder.encode(newPassword));
    }

    private static void requireBackOffice(Role role) {
        if (role == null || !role.isBackOffice()) {
            throw new BusinessException("角色只能是 ADMIN 或 STAFF");
        }
    }

    private Admin findOrThrow(Long adminId) {
        return adminRepository.findById(adminId).orElseThrow(() -> new ResourceNotFoundException("帳號不存在"));
    }
}
