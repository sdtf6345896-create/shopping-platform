package com.example.shopping.product.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StockUpdateRequest {

    @NotNull(message = "庫存不可為空")
    @Min(value = 0, message = "庫存不可為負數")
    private Integer stock;
}
