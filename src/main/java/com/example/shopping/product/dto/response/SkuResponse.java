package com.example.shopping.product.dto.response;

import com.example.shopping.product.entity.ProductSku;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class SkuResponse {

    private Long id;
    private String skuCode;
    private String specName;
    private BigDecimal price;
    /** 限時特價價格,不在特價期間為 null */
    private BigDecimal salePrice;
    private int stock;

    public static SkuResponse from(ProductSku sku) {
        BigDecimal salePrice = sku.getProduct() != null && sku.getProduct().isOnSale() ? sku.getEffectivePrice() : null;
        return new SkuResponse(sku.getId(), sku.getSkuCode(), sku.getSpecName(), sku.getPrice(), salePrice,
                sku.getStock());
    }
}
