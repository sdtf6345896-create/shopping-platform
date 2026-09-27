package com.example.shopping.points.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PointPolicyTest {

    private final PointPolicy policy = new PointPolicy(new BigDecimal("0.01"), new BigDecimal("0.5"));

    @Test
    void pointsEarnedFor_roundsDown() {
        assertThat(policy.pointsEarnedFor(new BigDecimal("1099.99"))).isEqualTo(10);
        assertThat(policy.pointsEarnedFor(new BigDecimal("99"))).isZero();
    }

    @Test
    void maxRedeemable_isLimitedByBalanceAndRatio() {
        assertThat(policy.maxRedeemable(1000, new BigDecimal("1181"))).isEqualTo(590);
        assertThat(policy.maxRedeemable(100, new BigDecimal("1181"))).isEqualTo(100);
        assertThat(policy.maxRedeemable(100, BigDecimal.ZERO)).isZero();
    }
}
