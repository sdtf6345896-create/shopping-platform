package com.example.shopping.product.dto.request;

import com.example.shopping.common.enums.ProductStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProductStatusRequest {

    @NotNull(message = "狀態不可為空")
    private ProductStatus status;
}
