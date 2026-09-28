package com.example.shopping.admin.service;

import com.example.shopping.admin.dto.request.AdminLoginRequest;
import com.example.shopping.admin.dto.response.AdminLoginResponse;
import com.example.shopping.admin.entity.Admin;
import com.example.shopping.admin.repository.AdminRepository;
import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.common.enums.Role;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.security.JwtTokenProvider;
import com.example.shopping.security.LoginAttemptService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminAuthServiceImpl implements AdminAuthService {

    private static final String LOGIN_SCOPE = "admin";

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginAttemptService loginAttemptService;

    public AdminAuthServiceImpl(AdminRepository adminRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtTokenProvider jwtTokenProvider,
                                 LoginAttemptService loginAttemptService) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {
        loginAttemptService.checkNotLocked(LOGIN_SCOPE, request.getUsername());

        Admin admin = adminRepository.findByUsername(request.getUsername()).orElse(null);
        if (admin == null || !passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            loginAttemptService.recordFailure(LOGIN_SCOPE, request.getUsername());
            throw new BusinessException("帳號或密碼錯誤");
        }
        loginAttemptService.recordSuccess(LOGIN_SCOPE, request.getUsername());
        if (admin.getStatus() != AccountStatus.ACTIVE) {
            throw new BusinessException("帳號已被停用,請聯繫系統管理員");
        }

        String token = jwtTokenProvider.generateToken(admin.getId(), admin.getUsername(), Role.valueOf(admin.getRole()));
        return AdminLoginResponse.of(token, admin.getId(), admin.getUsername(), admin.getName(), admin.getRole());
    }
}
