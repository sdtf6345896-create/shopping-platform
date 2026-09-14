package com.example.shopping.admin.service;

import com.example.shopping.admin.dto.request.AdminLoginRequest;
import com.example.shopping.admin.dto.response.AdminLoginResponse;

public interface AdminAuthService {

    AdminLoginResponse login(AdminLoginRequest request);
}
