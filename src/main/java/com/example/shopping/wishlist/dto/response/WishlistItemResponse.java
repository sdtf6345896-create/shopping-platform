package com.example.shopping.wishlist.dto.response;

import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.entity.Product;
import com.example.shopping.wishlist.entity.WishlistItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class WishlistItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private String mainImage;
    private BigDecimal price;
    private ProductStatus productStatus;
    private LocalDateTime createdAt;

    public static WishlistItemResponse from(WishlistItem item) {
        Product product = item.getProduct();
        return new WishlistItemResponse(
                item.getId(),
                product.getId(),
                product.getName(),
                product.getMainImage(),
                product.getPrice(),
                product.getStatus(),
                item.getCreatedAt());
    }
}
