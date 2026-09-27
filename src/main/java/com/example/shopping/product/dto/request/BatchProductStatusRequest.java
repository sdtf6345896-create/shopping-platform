package com.example.shopping.product.dto.request;

import com.example.shopping.common.enums.ProductStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class BatchProductStatusRequest {

    @NotEmpty(message = "請選擇商品")
    @Size(max = 100, message = "一次最多 100 個商品")
    private List<@NotNull Long> ids;

    @NotNull(message = "狀態不可為空")
    private ProductStatus status;
}
