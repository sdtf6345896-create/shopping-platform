package com.example.shopping.question.dto.response;

import com.example.shopping.question.entity.ProductQuestion;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/** 前台顯示用,會員姓名遮罩 */
@Getter
@AllArgsConstructor
public class QuestionResponse {

    private Long id;
    private String memberName;
    private String content;
    private String answer;
    private LocalDateTime answeredAt;
    private LocalDateTime createdAt;

    public static QuestionResponse from(ProductQuestion question) {
        return new QuestionResponse(
                question.getId(),
                maskName(question.getMember().getName()),
                question.getContent(),
                question.getAnswer(),
                question.getAnsweredAt(),
                question.getCreatedAt());
    }

    private static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "匿名會員";
        }
        return name.charAt(0) + "**";
    }
}
