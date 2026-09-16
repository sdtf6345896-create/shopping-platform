package com.example.shopping.review.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewRequest {

    @NotNull(message = "評分不可為空")
    @Min(value = 1, message = "評分需介於 1 到 5 之間")
    @Max(value = 5, message = "評分需介於 1 到 5 之間")
    private Integer rating;

    @Size(max = 500, message = "評論內容不可超過 500 字")
    private String content;
}
