package com.example.shopping.review.dto.response;

import com.example.shopping.review.entity.ProductReview;
import com.example.shopping.review.entity.ReviewImage;

import java.time.LocalDateTime;
import java.util.List;

/** 後台評價管理用:不遮蔽會員姓名,附商品與會員資訊 */
public record AdminReviewResponse(Long id, Long productId, String productName, Long memberId, String memberName,
                                  String memberEmail, int rating, String content, List<String> images,
                                  String sellerReply, LocalDateTime repliedAt, boolean hidden,
                                  LocalDateTime createdAt) {

    public static AdminReviewResponse from(ProductReview review) {
        return new AdminReviewResponse(review.getId(), review.getProduct().getId(), review.getProduct().getName(),
                review.getMember().getId(), review.getMember().getName(), review.getMember().getEmail(),
                review.getRating(), review.getContent(),
                review.getImages().stream().map(ReviewImage::getUrl).toList(),
                review.getSellerReply(), review.getRepliedAt(), review.isHidden(), review.getCreatedAt());
    }
}
