package com.example.shopping.review.service;

import com.example.shopping.common.enums.NotificationType;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.notification.service.NotificationService;
import com.example.shopping.review.dto.response.AdminReviewResponse;
import com.example.shopping.review.entity.ProductReview;
import com.example.shopping.review.repository.ProductReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** 後台評價管理:查詢、公開回覆、隱藏 / 取消隱藏 */
@Service
@Transactional
public class AdminReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final NotificationService notificationService;

    public AdminReviewService(ProductReviewRepository reviewRepository,
                              ReviewService reviewService,
                              NotificationService notificationService) {
        this.reviewRepository = reviewRepository;
        this.reviewService = reviewService;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public Page<AdminReviewResponse> search(Integer rating, Boolean replied, Boolean hidden, String keyword,
                                            Pageable pageable) {
        String trimmed = keyword == null || keyword.isBlank() ? null : keyword.trim();
        return reviewRepository.searchAdmin(rating, replied, hidden, trimmed, pageable).map(AdminReviewResponse::from);
    }

    /** 回覆評價(空白 = 刪除回覆);第一次回覆時通知評論者 */
    public AdminReviewResponse reply(Long reviewId, String reply) {
        ProductReview review = findOrThrow(reviewId);
        String trimmed = reply == null || reply.isBlank() ? null : reply.trim();
        boolean firstReply = review.getSellerReply() == null && trimmed != null;
        review.setSellerReply(trimmed);
        review.setRepliedAt(trimmed == null ? null : LocalDateTime.now());
        if (firstReply) {
            notificationService.notify(review.getMember().getId(), NotificationType.SYSTEM, "賣家回覆了你的評價",
                    "你對「" + review.getProduct().getName() + "」的評價有新回覆,快去看看吧!",
                    "/products/" + review.getProduct().getId());
        }
        return AdminReviewResponse.from(review);
    }

    /** 隱藏 / 取消隱藏,並重算商品評分 */
    public AdminReviewResponse setHidden(Long reviewId, boolean hidden) {
        ProductReview review = findOrThrow(reviewId);
        if (review.isHidden() != hidden) {
            review.setHidden(hidden);
            reviewService.refreshProductRating(review.getProduct().getId());
        }
        return AdminReviewResponse.from(review);
    }

    private ProductReview findOrThrow(Long reviewId) {
        return reviewRepository.findById(reviewId).orElseThrow(() -> new ResourceNotFoundException("評價不存在"));
    }
}
