package com.example.shopping.member.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.member.dto.request.ChangePasswordRequest;
import com.example.shopping.member.dto.request.MemberUpdateRequest;
import com.example.shopping.member.dto.response.MemberResponse;
import com.example.shopping.member.service.MemberService;
import com.example.shopping.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    @GetMapping("/me")
    public ApiResponse<MemberResponse> getProfile() {
        return ApiResponse.success(memberService.getProfile(SecurityUtils.getCurrentUserId()));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        memberService.changePassword(SecurityUtils.getCurrentUserId(), request);
        return ApiResponse.success("密碼已變更,其他裝置需重新登入", null);
    }

    @PutMapping("/me")
    public ApiResponse<MemberResponse> updateProfile(@Valid @RequestBody MemberUpdateRequest request) {
        return ApiResponse.success("更新成功", memberService.updateProfile(SecurityUtils.getCurrentUserId(), request));
    }
}
