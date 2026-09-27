package com.example.shopping.order.shipping;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 運費規則:商品金額(套用優惠券後、折抵購物金前)達免運門檻免運費,否則收固定運費。
 */
@Component
public class ShippingPolicy {

    private final BigDecimal fee;
    private final BigDecimal freeThreshold;

    public ShippingPolicy(@Value("${app.shipping.fee:60}") BigDecimal fee,
                          @Value("${app.shipping.free-threshold:999}") BigDecimal freeThreshold) {
        this.fee = fee;
        this.freeThreshold = freeThreshold;
    }

    public BigDecimal feeFor(BigDecimal merchandiseAmount) {
        return merchandiseAmount.compareTo(freeThreshold) >= 0 ? BigDecimal.ZERO : fee;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public BigDecimal getFreeThreshold() {
        return freeThreshold;
    }
}
