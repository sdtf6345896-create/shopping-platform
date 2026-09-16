package com.example.shopping.review.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReviewSummaryResponse {

    private double averageRating;
    private long reviewCount;
}
