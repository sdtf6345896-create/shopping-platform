package com.example.shopping.recommendation.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.recommendation.dto.RecommendationResponse;
import com.example.shopping.recommendation.service.RecommendationService;
import com.example.shopping.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ApiResponse<RecommendationResponse> recommend(@RequestParam(defaultValue = "8") int limit) {
        return ApiResponse.success(recommendationService.recommend(SecurityUtils.getCurrentUserId(), limit));
    }
}
