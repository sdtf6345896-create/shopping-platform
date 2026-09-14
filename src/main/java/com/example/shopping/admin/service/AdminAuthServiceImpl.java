package com.example.shopping.admin.service;

import com.example.shopping.admin.dto.request.AdminLoginRequest;
import com.example.shopping.admin.dto.response.AdminLoginResponse;
import com.example.shopping.admin.entity.Admin;
import com.example.shopping.admin.repository.AdminRepository;
import com.example.shopping.common.enums.Role;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AdminAuthServiceImpl(AdminRepository adminRepository,
                                 PasswordEncoder passwordEncoder,
                                 JwtTokenProvider jwtTokenProvider) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    public AdminLoginResponse login(AdminLoginRequest request) {
        Admin admin = adminRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BusinessException("帳號或密碼錯誤"));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BusinessException("帳號或密碼錯誤");
        }

        String token = jwtTokenProvider.generateToken(admin.getId(), admin.getUsername(), Role.ADMIN);
        return AdminLoginResponse.of(token, admin.getId(), admin.getUsername(), admin.getName());
    }
}
