package com.example.shopping.product.dto.response;

import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ProductListResponse {

    private Long id;
    private String name;
    private String mainImage;
    private BigDecimal price;
    private ProductStatus status;
    private int salesCount;

    public static ProductListResponse from(Product product) {
        return new ProductListResponse(
                product.getId(),
                product.getName(),
                product.getMainImage(),
                product.getPrice(),
                product.getStatus(),
                product.getSalesCount());
    }
}
