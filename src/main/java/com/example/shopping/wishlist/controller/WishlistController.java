package com.example.shopping.wishlist.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.security.SecurityUtils;
import com.example.shopping.wishlist.dto.response.WishlistItemResponse;
import com.example.shopping.wishlist.service.WishlistService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ApiResponse<PageResponse<WishlistItemResponse>> list(
            @PageableDefault(size = 12, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PageResponse.from(
                wishlistService.list(SecurityUtils.getCurrentUserId(), pageable)));
    }

    @GetMapping("/{productId}")
    public ApiResponse<Boolean> isFavorited(@PathVariable Long productId) {
        return ApiResponse.success(wishlistService.isFavorited(SecurityUtils.getCurrentUserId(), productId));
    }

    @PostMapping("/{productId}")
    public ApiResponse<Void> add(@PathVariable Long productId) {
        wishlistService.add(SecurityUtils.getCurrentUserId(), productId);
        return ApiResponse.success("已加入收藏", null);
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(@PathVariable Long productId) {
        wishlistService.remove(SecurityUtils.getCurrentUserId(), productId);
        return ApiResponse.success("已移除收藏", null);
    }
}
