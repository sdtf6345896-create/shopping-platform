package com.example.shopping.cart.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateCartItemRequest {

    @NotNull(message = "數量不可為空")
    @Min(value = 1, message = "數量至少為 1")
    private Integer quantity;
}
