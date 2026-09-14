package com.example.shopping.category.controller;

import com.example.shopping.category.dto.CategoryRequest;
import com.example.shopping.category.dto.CategoryResponse;
import com.example.shopping.category.dto.CategoryStatusRequest;
import com.example.shopping.category.service.CategoryService;
import com.example.shopping.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> list() {
        return ApiResponse.success(categoryService.listAll());
    }

    @PostMapping
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success("新增成功", categoryService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success("更新成功", categoryService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<CategoryResponse> updateStatus(@PathVariable Long id,
                                                       @Valid @RequestBody CategoryStatusRequest request) {
        return ApiResponse.success("狀態更新成功", categoryService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success("刪除成功", null);
    }
}
