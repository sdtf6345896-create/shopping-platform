package com.example.shopping.member.referral;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/me/referral")
public class ReferralController {

    private final ReferralService referralService;

    public ReferralController(ReferralService referralService) {
        this.referralService = referralService;
    }

    @GetMapping
    public ApiResponse<ReferralResponse> myReferral() {
        return ApiResponse.success(referralService.getMyReferral(SecurityUtils.getCurrentUserId()));
    }
}
