package com.example.shopping.review.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewHiddenRequest {

    @NotNull(message = "請指定是否隱藏")
    private Boolean hidden;
}
