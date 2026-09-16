package com.example.shopping.wishlist.service;

import com.example.shopping.wishlist.dto.response.WishlistItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WishlistService {

    Page<WishlistItemResponse> list(Long memberId, Pageable pageable);

    boolean isFavorited(Long memberId, Long productId);

    void add(Long memberId, Long productId);

    void remove(Long memberId, Long productId);
}
