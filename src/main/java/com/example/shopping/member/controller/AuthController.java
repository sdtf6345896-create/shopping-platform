package com.example.shopping.member.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.member.dto.request.LoginRequest;
import com.example.shopping.member.dto.request.RegisterRequest;
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
}
