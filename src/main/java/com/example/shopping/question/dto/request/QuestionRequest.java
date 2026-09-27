package com.example.shopping.question.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionRequest {

    @NotBlank(message = "提問內容不可為空")
    @Size(max = 500, message = "提問內容最多 500 字")
    private String content;
}
