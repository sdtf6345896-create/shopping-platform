package com.example.shopping.order.shipping;

import com.example.shopping.common.enums.ShippingMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 運費規則:商品金額(套用優惠券後、折抵購物金前)達免運門檻免運費,否則依配送方式收固定運費。
 */
@Component
public class ShippingPolicy {

    private final BigDecimal fee;
    private final BigDecimal cvsFee;
    private final BigDecimal freeThreshold;
    private final BigDecimal giftWrapFee;

    public ShippingPolicy(@Value("${app.shipping.fee:60}") BigDecimal fee,
                          @Value("${app.shipping.cvs-fee:45}") BigDecimal cvsFee,
                          @Value("${app.shipping.free-threshold:999}") BigDecimal freeThreshold,
                          @Value("${app.shipping.gift-wrap-fee:30}") BigDecimal giftWrapFee) {
        this.fee = fee;
        this.cvsFee = cvsFee;
        this.freeThreshold = freeThreshold;
        this.giftWrapFee = giftWrapFee;
    }

    public BigDecimal feeFor(ShippingMethod method, BigDecimal merchandiseAmount) {
        if (merchandiseAmount.compareTo(freeThreshold) >= 0) {
            return BigDecimal.ZERO;
        }
        return method == ShippingMethod.CVS_PICKUP ? cvsFee : fee;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public BigDecimal getCvsFee() {
        return cvsFee;
    }

    /** 禮品包裝費(不受免運門檻影響) */
    public BigDecimal getGiftWrapFee() {
        return giftWrapFee;
    }

    public BigDecimal getFreeThreshold() {
        return freeThreshold;
    }
}
