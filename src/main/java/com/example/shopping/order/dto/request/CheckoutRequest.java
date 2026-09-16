package com.example.shopping.order.dto.request;

import com.example.shopping.common.enums.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CheckoutRequest {

    @NotNull(message = "收件地址不可為空")
    private Long addressId;

    @NotNull(message = "付款方式不可為空")
    private PaymentMethod paymentMethod;

    /**
     * 要結帳的購物車項目 id;不填則結帳購物車內所有項目。
     */
    private List<Long> cartItemIds;

    /**
     * 欲套用的優惠券代碼;不填則不使用優惠券。
     */
    private String couponCode;
}
