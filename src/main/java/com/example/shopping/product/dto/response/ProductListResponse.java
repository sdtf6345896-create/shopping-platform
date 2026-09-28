package com.example.shopping.product.dto.response;

import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ProductListResponse {

    private Long id;
    private String name;
    private String mainImage;
    private BigDecimal price;
    /** 限時特價價格,不在特價期間為 null */
    private BigDecimal salePrice;
    private Integer saleDiscountPercent;
    private LocalDateTime saleEndAt;
    private ProductStatus status;
    /** 排程上架 / 下架時間(後台列表顯示用) */
    private LocalDateTime publishAt;
    private LocalDateTime unpublishAt;
    private int salesCount;
    private BigDecimal ratingAverage;
    private int reviewCount;

    public static ProductListResponse from(Product product) {
        return new ProductListResponse(
                product.getId(),
                product.getName(),
                product.getMainImage(),
                product.getPrice(),
                product.isOnSale() ? product.applySale(product.getPrice()) : null,
                product.isOnSale() ? product.getSaleDiscountPercent() : null,
                product.isOnSale() ? product.getSaleEndAt() : null,
                product.getStatus(),
                product.getPublishAt(),
                product.getUnpublishAt(),
                product.getSalesCount(),
                product.getRatingAverage(),
                product.getReviewCount());
    }
}
