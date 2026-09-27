package com.example.shopping.review.service;

import com.example.shopping.review.dto.request.ReviewRequest;
import com.example.shopping.review.dto.response.ReviewResponse;
import com.example.shopping.review.dto.response.ReviewSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ReviewService {

    Page<ReviewResponse> listByProduct(Long productId, Pageable pageable);

    ReviewSummaryResponse getSummary(Long productId);

    Optional<ReviewResponse> getMyReview(Long memberId, Long productId);

    ReviewResponse upsert(Long memberId, Long productId, ReviewRequest request);

    void delete(Long memberId, Long productId);

    /** 重新計算商品的平均星等與評論數(評論異動後呼叫) */
    void refreshProductRating(Long productId);
}
