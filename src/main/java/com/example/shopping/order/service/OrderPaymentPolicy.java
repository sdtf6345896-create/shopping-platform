package com.example.shopping.order.service;

import com.example.shopping.common.enums.PaymentMethod;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 付款期限規則:線上付款(信用卡、ATM)須在期限內付款,逾期由排程自動取消;
 * 貨到付款是收貨時才付錢,不設期限。
 */
@Component
public class OrderPaymentPolicy {

    private final Duration paymentTimeout;

    public OrderPaymentPolicy(@Value("${app.order.payment-timeout-minutes:30}") long paymentTimeoutMinutes) {
        this.paymentTimeout = Duration.ofMinutes(paymentTimeoutMinutes);
    }

    /** @return 付款期限;不需要期限時回傳 null */
    public LocalDateTime deadlineFor(PaymentMethod paymentMethod, LocalDateTime orderedAt) {
        if (paymentMethod == PaymentMethod.COD) {
            return null;
        }
        return orderedAt.plus(paymentTimeout);
    }
}
