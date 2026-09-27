package com.example.shopping.recommendation.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.recommendation.dto.BoughtTogetherResponse;
import com.example.shopping.recommendation.service.BoughtTogetherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class BoughtTogetherController {

    private final BoughtTogetherService boughtTogetherService;

    public BoughtTogetherController(BoughtTogetherService boughtTogetherService) {
        this.boughtTogetherService = boughtTogetherService;
    }

    @GetMapping("/api/products/{id}/bought-together")
    public ApiResponse<List<BoughtTogetherResponse>> boughtTogether(@PathVariable Long id,
                                                                     @RequestParam(defaultValue = "6") int limit) {
        return ApiResponse.success(boughtTogetherService.boughtTogether(id, limit));
    }
}
