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
    /** 賣家回覆,未回覆為 null */
    private String sellerReply;
    private LocalDateTime repliedAt;
    /** 是否被管理員隱藏(前台列表不會出現,只有評論者本人在「我的評價」看得到) */
    private boolean hidden;
    /** 「有幫助」票數 */
    private int helpfulCount;

    public static ReviewResponse from(ProductReview review) {
        return new ReviewResponse(
                review.getId(),
                maskName(review.getMember().getName()),
                review.getRating(),
                review.getContent(),
                review.getImages().stream().map(ReviewImage::getUrl).toList(),
                review.getCreatedAt(),
                review.getUpdatedAt(),
                review.getSellerReply(),
                review.getRepliedAt(),
                review.isHidden(),
                review.getHelpfulCount());
    }

    private static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "匿名會員";
        }
        return name.charAt(0) + "**";
    }
}
