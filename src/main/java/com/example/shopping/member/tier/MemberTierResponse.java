package com.example.shopping.member.tier;

import java.math.BigDecimal;

/**
 * @param spending     近 12 個月已完成訂單的商品金額
 * @param nextTier     下一個等級,已是最高級為 null
 * @param amountToNext 距離下一級還差多少,已是最高級為 0
 */
public record MemberTierResponse(MemberTier tier, String label, BigDecimal pointsMultiplier, BigDecimal spending,
                                 MemberTier nextTier, String nextLabel, BigDecimal amountToNext) {
}
