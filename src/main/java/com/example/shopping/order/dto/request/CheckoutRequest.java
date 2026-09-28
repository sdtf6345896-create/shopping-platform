package com.example.shopping.order.dto.request;

import com.example.shopping.common.enums.PaymentMethod;
import com.example.shopping.common.enums.ShippingMethod;
import com.example.shopping.order.invoice.InvoiceRequest;
import com.example.shopping.order.shipping.CvsPickupRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CheckoutRequest {

    /** 配送方式,不填為宅配 */
    private ShippingMethod shippingMethod;

    /** 宅配的收件地址 id(宅配必填) */
    private Long addressId;

    /** 超商取貨資訊(超商取貨必填) */
    @Valid
    private CvsPickupRequest cvsPickup;

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

    /**
     * 欲折抵的購物金點數;不填或 0 則不使用。
     */
    @Min(value = 0, message = "購物金點數不可為負數")
    private Integer pointsToUse;

    /**
     * 給賣家的備註(例如配送時段),選填。
     */
    @Size(max = 200, message = "訂單備註最多 200 字")
    private String note;

    /** 發票開立方式,不填為會員載具 */
    @Valid
    private InvoiceRequest invoice;
}
