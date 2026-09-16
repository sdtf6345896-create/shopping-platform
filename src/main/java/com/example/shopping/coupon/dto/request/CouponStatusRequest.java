package com.example.shopping.coupon.dto.request;

import com.example.shopping.common.enums.CouponStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CouponStatusRequest {

    @NotNull(message = "狀態不可為空")
    private CouponStatus status;
}
