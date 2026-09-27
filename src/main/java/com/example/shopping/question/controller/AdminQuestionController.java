package com.example.shopping.question.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.question.dto.request.AnswerRequest;
import com.example.shopping.question.dto.response.AdminQuestionResponse;
import com.example.shopping.question.service.QuestionService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/questions")
public class AdminQuestionController {

    private final QuestionService questionService;

    public AdminQuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminQuestionResponse>> list(
            @RequestParam(required = false) Boolean answered,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PageResponse.from(questionService.listAdmin(answered, pageable)));
    }

    @PutMapping("/{id}/answer")
    public ApiResponse<AdminQuestionResponse> answer(@PathVariable Long id,
                                                     @Valid @RequestBody AnswerRequest request) {
        return ApiResponse.success("回覆成功", questionService.answer(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ApiResponse.success("刪除成功", null);
    }
}
