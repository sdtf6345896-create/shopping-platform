package com.example.shopping.category.controller;

import com.example.shopping.audit.AdminAudit;
import com.example.shopping.audit.AuditTarget;
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

    @AdminAudit(action = "新增分類", target = AuditTarget.CATEGORY, detail = "#request.name")
    @PostMapping
    public ApiResponse<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success("新增成功", categoryService.create(request));
    }

    @AdminAudit(action = "修改分類", target = AuditTarget.CATEGORY, detail = "#request.name")
    @PutMapping("/{id}")
    public ApiResponse<CategoryResponse> update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return ApiResponse.success("更新成功", categoryService.update(id, request));
    }

    @AdminAudit(action = "分類啟用/停用", target = AuditTarget.CATEGORY, detail = "#request.status")
    @PatchMapping("/{id}/status")
    public ApiResponse<CategoryResponse> updateStatus(@PathVariable Long id,
                                                       @Valid @RequestBody CategoryStatusRequest request) {
        return ApiResponse.success("狀態更新成功", categoryService.updateStatus(id, request));
    }

    @AdminAudit(action = "刪除分類", target = AuditTarget.CATEGORY)
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ApiResponse.success("刪除成功", null);
    }
}
