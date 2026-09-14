package com.example.shopping.cart.controller;

import com.example.shopping.cart.dto.request.AddCartItemRequest;
import com.example.shopping.cart.dto.request.UpdateCartItemRequest;
import com.example.shopping.cart.dto.response.CartItemResponse;
import com.example.shopping.cart.dto.response.CartSummaryResponse;
import com.example.shopping.cart.service.CartService;
import com.example.shopping.common.ApiResponse;
import com.example.shopping.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public ApiResponse<CartSummaryResponse> getCart() {
        return ApiResponse.success(cartService.getCart(SecurityUtils.getCurrentUserId()));
    }

    @PostMapping("/items")
    public ApiResponse<CartItemResponse> addItem(@Valid @RequestBody AddCartItemRequest request) {
        return ApiResponse.success("已加入購物車", cartService.addItem(SecurityUtils.getCurrentUserId(), request));
    }

    @PutMapping("/items/{itemId}")
    public ApiResponse<CartItemResponse> updateQuantity(@PathVariable Long itemId,
                                                         @Valid @RequestBody UpdateCartItemRequest request) {
        return ApiResponse.success("更新成功",
                cartService.updateQuantity(SecurityUtils.getCurrentUserId(), itemId, request));
    }

    @DeleteMapping("/items/{itemId}")
    public ApiResponse<Void> removeItem(@PathVariable Long itemId) {
        cartService.removeItem(SecurityUtils.getCurrentUserId(), itemId);
        return ApiResponse.success("刪除成功", null);
    }

    @DeleteMapping
    public ApiResponse<Void> clearCart() {
        cartService.clearCart(SecurityUtils.getCurrentUserId());
        return ApiResponse.success("購物車已清空", null);
    }
}
