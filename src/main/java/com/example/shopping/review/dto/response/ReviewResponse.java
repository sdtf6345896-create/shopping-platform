package com.example.shopping.review.dto.response;

import com.example.shopping.review.entity.ProductReview;
import com.example.shopping.review.entity.ReviewImage;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private String memberName;
    private int rating;
    private String content;
    private List<String> images;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ReviewResponse from(ProductReview review) {
        return new ReviewResponse(
                review.getId(),
                maskName(review.getMember().getName()),
                review.getRating(),
                review.getContent(),
                review.getImages().stream().map(ReviewImage::getUrl).toList(),
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
