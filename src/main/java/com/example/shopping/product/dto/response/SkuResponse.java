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
    private int stock;

    public static SkuResponse from(ProductSku sku) {
        return new SkuResponse(sku.getId(), sku.getSkuCode(), sku.getSpecName(), sku.getPrice(), sku.getStock());
    }
}
