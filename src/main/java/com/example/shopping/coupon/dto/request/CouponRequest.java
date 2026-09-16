package com.example.shopping.coupon.dto.request;

import com.example.shopping.common.enums.DiscountType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
public class CouponRequest {

    @NotBlank(message = "優惠券代碼不可為空")
    private String code;

    @NotBlank(message = "優惠券名稱不可為空")
    private String name;

    @NotNull(message = "折扣類型不可為空")
    private DiscountType discountType;

    @NotNull(message = "折扣數值不可為空")
    @Positive(message = "折扣數值必須大於 0")
    private BigDecimal discountValue;

    /** 僅百分比折扣適用,折抵上限金額 */
    private BigDecimal maxDiscountAmount;

    @NotNull(message = "最低消費金額不可為空")
    @DecimalMin(value = "0", message = "最低消費金額不可為負數")
    private BigDecimal minSpendAmount;

    /** 總發放張數上限,留空代表不限制 */
    private Integer totalQuantity;

    private LocalDateTime startAt;

    private LocalDateTime endAt;
}
