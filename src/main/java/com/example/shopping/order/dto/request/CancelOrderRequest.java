package com.example.shopping.order.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CancelOrderRequest {

    /** 取消原因,選填 */
    @Size(max = 100, message = "取消原因最多 100 字")
    private String reason;
}
