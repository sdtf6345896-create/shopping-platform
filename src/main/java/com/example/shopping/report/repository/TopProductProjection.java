package com.example.shopping.report.repository;

import java.math.BigDecimal;

public interface TopProductProjection {

    Long getProductId();

    String getProductName();

    String getMainImage();

    Long getSoldQuantity();

    BigDecimal getRevenue();
}
