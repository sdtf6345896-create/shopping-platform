package com.example.shopping.order.message;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderMessageRequest {

    @NotBlank(message = "留言內容不可為空")
    @Size(max = 500, message = "留言最多 500 字")
    private String content;
}
