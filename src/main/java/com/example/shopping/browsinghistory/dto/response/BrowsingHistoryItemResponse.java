package com.example.shopping.browsinghistory.dto.response;

import com.example.shopping.browsinghistory.entity.BrowsingHistoryItem;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BrowsingHistoryItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String mainImage;
    private BigDecimal price;
    private ProductStatus productStatus;
    private LocalDateTime viewedAt;

    public static BrowsingHistoryItemResponse from(BrowsingHistoryItem item) {
        Product product = item.getProduct();
        return new BrowsingHistoryItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getMainImage(),
                product.getPrice(),
                product.getStatus(),
                item.getViewedAt());
    }
}
