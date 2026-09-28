package com.example.shopping.coupon.dto.request;

import com.example.shopping.member.tier.MemberTier;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/** 後台直接發券到會員的優惠券錢包 */
@Getter
@Setter
public class CouponIssueRequest {

    public enum Target {
        /** 所有啟用中的會員 */
        ALL,
        /** 等級至少為 minTier 的會員 */
        TIER,
        /** 指定 Email 名單 */
        EMAILS
    }

    @NotNull(message = "請選擇發放對象")
    private Target target;

    /** target = TIER 時必填 */
    private MemberTier minTier;

    /** target = EMAILS 時必填 */
    @Size(max = 1000, message = "Email 名單一次最多 1000 筆")
    private List<String> emails;
}
