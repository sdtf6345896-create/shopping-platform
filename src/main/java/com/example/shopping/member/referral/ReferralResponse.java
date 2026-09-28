package com.example.shopping.member.referral;

import java.math.BigDecimal;

/**
 * @param invitedCount  用這個邀請碼註冊的人數
 * @param rewardedCount 其中已完成首筆訂單、發過獎勵的人數
 */
public record ReferralResponse(String code, long invitedCount, long rewardedCount,
                               int referrerPoints, int refereePoints, BigDecimal minOrderAmount) {
}
