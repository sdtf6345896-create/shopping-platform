package com.example.shopping.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddCartItemRequest {

    @NotNull(message = "商品規格不可為空")
    private Long skuId;

    @Min(value = 1, message = "數量至少為 1")
    private int quantity = 1;
}
