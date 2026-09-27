package com.example.shopping.report.repository;

import java.math.BigDecimal;

public interface CategorySalesProjection {

    Long getCategoryId();

    Long getSoldQuantity();

    BigDecimal getRevenue();
}
