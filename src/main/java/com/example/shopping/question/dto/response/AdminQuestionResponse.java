package com.example.shopping.question.dto.response;

import com.example.shopping.question.entity.ProductQuestion;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/** 後台顯示用,帶商品與完整會員資訊 */
@Getter
@AllArgsConstructor
public class AdminQuestionResponse {

    private Long id;
    private Long productId;
    private String productName;
    private Long memberId;
    private String memberName;
    private String memberEmail;
    private String content;
    private String answer;
    private LocalDateTime answeredAt;
    private LocalDateTime createdAt;

    public static AdminQuestionResponse from(ProductQuestion question) {
        return new AdminQuestionResponse(
                question.getId(),
                question.getProduct().getId(),
                question.getProduct().getName(),
                question.getMember().getId(),
                question.getMember().getName(),
                question.getMember().getEmail(),
                question.getContent(),
                question.getAnswer(),
                question.getAnsweredAt(),
                question.getCreatedAt());
    }
}
