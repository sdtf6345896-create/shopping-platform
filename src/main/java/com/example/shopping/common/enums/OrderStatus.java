package com.example.shopping.common.enums;

public enum OrderStatus {
    PENDING_PAYMENT,
    PAID,
    SHIPPING,
    COMPLETED,
    CANCELLED,
    /** 已完成的訂單經退貨核准後退款 */
    REFUNDED
}
