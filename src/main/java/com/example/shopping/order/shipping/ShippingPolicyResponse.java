package com.example.shopping.order.shipping;

import java.math.BigDecimal;

public record ShippingPolicyResponse(BigDecimal fee, BigDecimal freeThreshold) {
}
