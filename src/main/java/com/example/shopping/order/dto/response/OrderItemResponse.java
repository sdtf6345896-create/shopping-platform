package com.example.shopping.order.dto.response;

import com.example.shopping.order.entity.OrderItem;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class OrderItemResponse {

    private Long id;
    private Long skuId;
    private String productName;
    private String specName;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal subtotal;

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductSku().getId(),
                item.getProductName(),
                item.getSpecName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal());
    }
}
