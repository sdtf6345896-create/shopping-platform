package com.example.shopping.review.controller;

import com.example.shopping.audit.AdminAudit;
import com.example.shopping.audit.AuditTarget;
import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.review.dto.request.ReviewHiddenRequest;
import com.example.shopping.review.dto.request.ReviewReplyRequest;
import com.example.shopping.review.dto.response.AdminReviewResponse;
import com.example.shopping.review.service.AdminReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
public class AdminReviewController {

    private final AdminReviewService adminReviewService;

    public AdminReviewController(AdminReviewService adminReviewService) {
        this.adminReviewService = adminReviewService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AdminReviewResponse>> list(
            @RequestParam(required = false) Integer rating,
            @RequestParam(required = false) Boolean replied,
            @RequestParam(required = false) Boolean hidden,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.success(PageResponse.from(adminReviewService.search(rating, replied, hidden, keyword, pageable)));
    }

    @AdminAudit(action = "回覆評價", target = AuditTarget.PRODUCT, targetId = "#result?.data?.productId",
            detail = "'評價 #' + #id")
    @PutMapping("/{id}/reply")
    public ApiResponse<AdminReviewResponse> reply(@PathVariable Long id, @Valid @RequestBody ReviewReplyRequest request) {
        return ApiResponse.success("已儲存回覆", adminReviewService.reply(id, request.getReply()));
    }

    @AdminAudit(action = "隱藏/顯示評價", target = AuditTarget.PRODUCT, targetId = "#result?.data?.productId",
            detail = "'評價 #' + #id + (#request.hidden ? ' 隱藏' : ' 取消隱藏')")
    @PatchMapping("/{id}/hidden")
    public ApiResponse<AdminReviewResponse> setHidden(@PathVariable Long id,
                                                      @Valid @RequestBody ReviewHiddenRequest request) {
        return ApiResponse.success(request.getHidden() ? "已隱藏" : "已取消隱藏",
                adminReviewService.setHidden(id, request.getHidden()));
    }
}
