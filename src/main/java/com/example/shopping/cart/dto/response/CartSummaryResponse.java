package com.example.shopping.cart.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class CartSummaryResponse {

    private List<CartItemResponse> items;
    private int totalQuantity;
    private BigDecimal totalAmount;

    public static CartSummaryResponse from(List<CartItemResponse> items) {
        int totalQuantity = items.stream().mapToInt(CartItemResponse::getQuantity).sum();
        BigDecimal totalAmount = items.stream()
                .map(CartItemResponse::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new CartSummaryResponse(items, totalQuantity, totalAmount);
    }
}
