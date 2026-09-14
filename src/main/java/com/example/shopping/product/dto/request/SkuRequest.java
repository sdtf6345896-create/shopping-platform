package com.example.shopping.product.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SkuRequest {

    @NotBlank(message = "SKU 編號不可為空")
    private String skuCode;

    @NotBlank(message = "規格名稱不可為空")
    private String specName;

    @NotNull(message = "價格不可為空")
    @DecimalMin(value = "0", inclusive = true, message = "價格不可為負數")
    private BigDecimal price;

    @Min(value = 0, message = "庫存不可為負數")
    private int stock;
}
