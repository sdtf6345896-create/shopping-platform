package com.example.shopping.product.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class ProductRequest {

    @NotNull(message = "分類不可為空")
    private Long categoryId;

    @NotBlank(message = "商品名稱不可為空")
    private String name;

    private String description;

    @NotNull(message = "價格不可為空")
    @DecimalMin(value = "0", inclusive = true, message = "價格不可為負數")
    private BigDecimal price;

    private String mainImage;

    @NotEmpty(message = "至少需要一個規格(SKU)")
    @Valid
    private List<SkuRequest> skus;
}
