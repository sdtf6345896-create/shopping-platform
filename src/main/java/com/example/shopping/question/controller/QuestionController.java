package com.example.shopping.question.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.question.dto.request.QuestionRequest;
import com.example.shopping.question.dto.response.QuestionResponse;
import com.example.shopping.question.service.QuestionService;
import com.example.shopping.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products/{productId}/questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public ApiResponse<PageResponse<QuestionResponse>> list(
            @PathVariable Long productId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PageResponse.from(questionService.listByProduct(productId, pageable)));
    }

    @PostMapping
    public ApiResponse<QuestionResponse> ask(@PathVariable Long productId,
                                             @Valid @RequestBody QuestionRequest request) {
        return ApiResponse.success("提問已送出,我們會盡快回覆",
                questionService.ask(SecurityUtils.getCurrentUserId(), productId, request));
    }
}
