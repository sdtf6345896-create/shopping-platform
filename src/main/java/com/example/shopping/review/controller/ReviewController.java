package com.example.shopping.review.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.review.dto.request.ReviewRequest;
import com.example.shopping.review.dto.response.ReviewResponse;
import com.example.shopping.review.dto.response.ReviewSummaryResponse;
import com.example.shopping.review.service.ReviewService;
import com.example.shopping.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{productId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ApiResponse<PageResponse<ReviewResponse>> list(
            @PathVariable Long productId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PageResponse.from(reviewService.listByProduct(productId, pageable)));
    }

    @GetMapping("/summary")
    public ApiResponse<ReviewSummaryResponse> summary(@PathVariable Long productId) {
        return ApiResponse.success(reviewService.getSummary(productId));
    }

    @GetMapping("/me")
    public ApiResponse<ReviewResponse> myReview(@PathVariable Long productId) {
        return ApiResponse.success(
                reviewService.getMyReview(SecurityUtils.getCurrentUserId(), productId).orElse(null));
    }

    @PutMapping("/me")
    public ApiResponse<ReviewResponse> upsert(@PathVariable Long productId, @Valid @RequestBody ReviewRequest request) {
        return ApiResponse.success("評論送出成功",
                reviewService.upsert(SecurityUtils.getCurrentUserId(), productId, request));
    }

    @DeleteMapping("/me")
    public ApiResponse<Void> delete(@PathVariable Long productId) {
        reviewService.delete(SecurityUtils.getCurrentUserId(), productId);
        return ApiResponse.success("評論已刪除", null);
    }
}
