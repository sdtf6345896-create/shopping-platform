package com.example.shopping.report.dto;

import com.example.shopping.report.repository.DailyStatProjection;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class DailySalesResponse {

    private LocalDate date;
    private long orderCount;
    private BigDecimal revenue;

    public static DailySalesResponse from(DailyStatProjection projection) {
        return new DailySalesResponse(
                projection.getDay().toLocalDate(),
                projection.getOrderCount(),
                projection.getRevenue());
    }
}
