package com.example.shopping.member.tier;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MemberTierController {

    private final MemberTierService memberTierService;

    public MemberTierController(MemberTierService memberTierService) {
        this.memberTierService = memberTierService;
    }

    @GetMapping("/api/members/me/tier")
    public ApiResponse<MemberTierResponse> myTier() {
        return ApiResponse.success(memberTierService.describe(SecurityUtils.getCurrentUserId()));
    }

    @GetMapping("/api/admin/members/{id}/tier")
    public ApiResponse<MemberTierResponse> memberTier(@PathVariable Long id) {
        return ApiResponse.success(memberTierService.describe(id));
    }
}
