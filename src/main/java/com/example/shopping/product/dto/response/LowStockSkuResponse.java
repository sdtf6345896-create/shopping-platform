package com.example.shopping.product.dto.response;

import com.example.shopping.product.entity.ProductSku;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LowStockSkuResponse {

    private Long productId;
    private String productName;
    private String mainImage;
    private Long skuId;
    private String skuCode;
    private String specName;
    private int stock;

    public static LowStockSkuResponse from(ProductSku sku) {
        return new LowStockSkuResponse(
                sku.getProduct().getId(),
                sku.getProduct().getName(),
                sku.getProduct().getMainImage(),
                sku.getId(),
                sku.getSkuCode(),
                sku.getSpecName(),
                sku.getStock());
    }
}
