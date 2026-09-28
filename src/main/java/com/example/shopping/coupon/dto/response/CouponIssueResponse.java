package com.example.shopping.coupon.dto.response;

import java.util.List;

/**
 * @param targeted       符合條件的會員數
 * @param issued         這次新放進錢包的人數
 * @param alreadyHeld    原本就持有、這次略過的人數
 * @param unmatchedEmails 名單中找不到或已停用的 Email
 */
public record CouponIssueResponse(int targeted, int issued, int alreadyHeld, List<String> unmatchedEmails) {
}
