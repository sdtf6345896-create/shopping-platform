package com.example.shopping.member.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.member.dto.request.ForgotPasswordRequest;
import com.example.shopping.member.dto.request.LoginRequest;
import com.example.shopping.member.dto.request.RefreshTokenRequest;
import com.example.shopping.member.dto.request.RegisterRequest;
import com.example.shopping.member.dto.request.ResetPasswordRequest;
import com.example.shopping.member.dto.response.LoginResponse;
import com.example.shopping.member.dto.response.MemberResponse;
import com.example.shopping.member.service.MemberService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final MemberService memberService;

    public AuthController(MemberService memberService) {
        this.memberService = memberService;
    }

    @PostMapping("/register")
    public ApiResponse<MemberResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResponse.success("註冊成功", memberService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success("登入成功", memberService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ApiResponse.success("Token 更新成功", memberService.refresh(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        memberService.logout(request);
        return ApiResponse.success("已登出", null);
    }

    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        memberService.forgotPassword(request);
        return ApiResponse.success("若該 Email 已註冊,重設密碼信將寄出", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        memberService.resetPassword(request);
        return ApiResponse.success("密碼重設成功,請重新登入", null);
    }
}
