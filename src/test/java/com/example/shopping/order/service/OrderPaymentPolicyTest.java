package com.example.shopping.order.service;

import com.example.shopping.common.enums.PaymentMethod;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class OrderPaymentPolicyTest {

    private final OrderPaymentPolicy policy = new OrderPaymentPolicy(30);
    private final LocalDateTime orderedAt = LocalDateTime.of(2026, 9, 27, 15, 0);

    @Test
    void deadlineFor_onlinePayment_addsTimeout() {
        assertThat(policy.deadlineFor(PaymentMethod.CREDIT_CARD, orderedAt))
                .isEqualTo(LocalDateTime.of(2026, 9, 27, 15, 30));
        assertThat(policy.deadlineFor(PaymentMethod.ATM, orderedAt))
                .isEqualTo(LocalDateTime.of(2026, 9, 27, 15, 30));
    }

    @Test
    void deadlineFor_cashOnDelivery_hasNoDeadline() {
        assertThat(policy.deadlineFor(PaymentMethod.COD, orderedAt)).isNull();
    }
}
