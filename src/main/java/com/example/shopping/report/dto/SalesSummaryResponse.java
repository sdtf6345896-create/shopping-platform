package com.example.shopping.report.dto;

import com.example.shopping.common.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Getter
@AllArgsConstructor
public class SalesSummaryResponse {

    private LocalDate startDate;
    private LocalDate endDate;
    private long totalOrders;
    private long paidOrders;
    private long cancelledOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private Map<OrderStatus, Long> statusCounts;
}
