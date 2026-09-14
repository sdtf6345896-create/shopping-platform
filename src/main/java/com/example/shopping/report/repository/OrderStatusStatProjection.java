package com.example.shopping.report.repository;

import com.example.shopping.common.enums.OrderStatus;

import java.math.BigDecimal;

public interface OrderStatusStatProjection {

    OrderStatus getStatus();

    long getCount();

    BigDecimal getAmount();
}
