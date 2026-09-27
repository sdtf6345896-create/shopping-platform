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
    /** 目前的 SKU 編號(揀貨用);商品名稱、規格、單價則是下單當下的快照 */
    private String skuCode;
    private String productName;
    private String specName;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal subtotal;

    public static OrderItemResponse from(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductSku().getId(),
                item.getProductSku().getSkuCode(),
                item.getProductName(),
                item.getSpecName(),
                item.getUnitPrice(),
                item.getQuantity(),
                item.getSubtotal());
    }
}
