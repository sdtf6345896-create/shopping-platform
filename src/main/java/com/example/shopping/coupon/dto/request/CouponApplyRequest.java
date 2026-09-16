package com.example.shopping.coupon.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CouponApplyRequest {

    @NotBlank(message = "請輸入優惠券代碼")
    private String code;

    /**
     * 要試算折扣的購物車項目 id;不填則以購物車內所有項目計算。
     */
    private List<Long> cartItemIds;
}
