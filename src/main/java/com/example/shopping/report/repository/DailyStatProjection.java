package com.example.shopping.report.repository;

import java.math.BigDecimal;
import java.sql.Date;

public interface DailyStatProjection {

    Date getDay();

    Long getOrderCount();

    BigDecimal getRevenue();
}
