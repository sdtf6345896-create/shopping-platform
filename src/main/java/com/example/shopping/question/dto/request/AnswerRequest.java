package com.example.shopping.question.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnswerRequest {

    @NotBlank(message = "回覆內容不可為空")
    @Size(max = 1000, message = "回覆內容最多 1000 字")
    private String answer;
}
