package com.example.shopping.review.dto.response;

import com.example.shopping.review.entity.ProductReview;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private String memberName;
    private int rating;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewResponse from(ProductReview review) {
        return new ReviewResponse(
                review.getId(),
                maskName(review.getMember().getName()),
                review.getRating(),
                review.getContent(),
                review.getCreatedAt(),
                review.getUpdatedAt());
    }

    private static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "匿名會員";
        }
        return name.charAt(0) + "**";
    }
}
