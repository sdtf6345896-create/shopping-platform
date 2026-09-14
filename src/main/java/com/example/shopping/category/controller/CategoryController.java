package com.example.shopping.category.controller;

import com.example.shopping.category.dto.CategoryResponse;
import com.example.shopping.category.service.CategoryService;
import com.example.shopping.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> tree() {
        return ApiResponse.success(categoryService.getPublicTree());
    }
}
