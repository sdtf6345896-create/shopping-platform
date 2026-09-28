package com.example.shopping.review.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewReplyRequest {

    /** 回覆內容;空白表示刪除回覆 */
    @Size(max = 500, message = "回覆最多 500 字")
    private String reply;
}
