package com.example.shopping.review.service;

import com.example.shopping.review.dto.request.ReviewRequest;
import com.example.shopping.review.dto.response.ReviewResponse;
import com.example.shopping.review.dto.response.ReviewSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ReviewService {

    /** @param withImagesOnly true 時只回傳附照片的評論 */
    /** @param mostHelpful true 依「有幫助」票數排序,否則新到舊(只看有照片時一律新到舊) */
    Page<ReviewResponse> listByProduct(Long productId, boolean withImagesOnly, boolean mostHelpful, Pageable pageable);

    ReviewSummaryResponse getSummary(Long productId);

    Optional<ReviewResponse> getMyReview(Long memberId, Long productId);

    ReviewResponse upsert(Long memberId, Long productId, ReviewRequest request);

    void delete(Long memberId, Long productId);

    /** 重新計算商品的平均星等與評論數(評論異動後呼叫) */
    void refreshProductRating(Long productId);
}
