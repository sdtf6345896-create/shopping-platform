package com.example.shopping.cart.service;

import com.example.shopping.cart.dto.request.AddCartItemRequest;
import com.example.shopping.cart.dto.request.UpdateCartItemRequest;
import com.example.shopping.cart.dto.response.CartItemResponse;
import com.example.shopping.cart.dto.response.CartSummaryResponse;

public interface CartService {

    CartSummaryResponse getCart(Long memberId);

    CartItemResponse addItem(Long memberId, AddCartItemRequest request);

    CartItemResponse updateQuantity(Long memberId, Long itemId, UpdateCartItemRequest request);

    void removeItem(Long memberId, Long itemId);

    void clearCart(Long memberId);
}
