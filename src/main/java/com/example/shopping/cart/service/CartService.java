package com.example.shopping.cart.service;

import com.example.shopping.cart.dto.request.AddCartItemRequest;
import com.example.shopping.cart.dto.request.UpdateCartItemRequest;
import com.example.shopping.cart.dto.response.CartItemResponse;
import com.example.shopping.cart.dto.response.CartSummaryResponse;

import java.math.BigDecimal;
import java.util.List;

public interface CartService {

    CartSummaryResponse getCart(Long memberId);

    CartItemResponse addItem(Long memberId, AddCartItemRequest request);

    CartItemResponse updateQuantity(Long memberId, Long itemId, UpdateCartItemRequest request);

    void removeItem(Long memberId, Long itemId);

    void clearCart(Long memberId);

    /**
     * 計算指定購物車項目(不填則為全部)的小計金額,供優惠券試算使用。
     */
    BigDecimal calculateSubtotal(Long memberId, List<Long> cartItemIds);
}
