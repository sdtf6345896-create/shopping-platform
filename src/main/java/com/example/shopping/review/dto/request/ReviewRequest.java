package com.example.shopping.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReviewRequest {

    @NotNull(message = "評分不可為空")
    @Min(value = 1, message = "評分需介於 1 到 5 之間")
    @Max(value = 5, message = "評分需介於 1 到 5 之間")
    private Integer rating;

    @Size(max = 500, message = "評論內容不可超過 500 字")
    private String content;

    /** 評論照片,只接受本站上傳 API 回傳的路徑(避免嵌入外部網址),最多 5 張 */
    @Size(max = 5, message = "評論照片最多 5 張")
    private List<@Pattern(regexp = "^/uploads/[A-Za-z0-9-]+\\.(jpg|png|webp)$", message = "照片網址格式不正確")
            String> images;
}
