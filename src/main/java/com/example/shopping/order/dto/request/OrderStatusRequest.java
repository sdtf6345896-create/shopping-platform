package com.example.shopping.order.dto.request;

import com.example.shopping.common.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusRequest {

    @NotNull(message = "狀態不可為空")
    private OrderStatus status;

    /** 物流業者,狀態改為 SHIPPING 時必填 */
    @Size(max = 30, message = "物流業者最多 30 字")
    private String shippingCarrier;

    /** 物流追蹤單號,狀態改為 SHIPPING 時必填 */
    @Size(max = 50, message = "物流單號最多 50 字")
    private String trackingNumber;

    /** 管理員備註(例如取消原因),會寫入訂單歷程 */
    @Size(max = 255, message = "備註最多 255 字")
    private String note;
}
