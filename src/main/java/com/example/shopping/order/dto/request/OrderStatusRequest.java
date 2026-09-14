package com.example.shopping.order.dto.request;

import com.example.shopping.common.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusRequest {

    @NotNull(message = "狀態不可為空")
    private OrderStatus status;
}
