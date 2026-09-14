package com.example.shopping.report.dto;

import com.example.shopping.report.repository.TopProductProjection;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class TopProductResponse {

    private Long productId;
    private String productName;
    private String mainImage;
    private long soldQuantity;
    private BigDecimal revenue;

    public static TopProductResponse from(TopProductProjection projection) {
        return new TopProductResponse(
                projection.getProductId(),
                projection.getProductName(),
                projection.getMainImage(),
                projection.getSoldQuantity(),
                projection.getRevenue());
    }
}
