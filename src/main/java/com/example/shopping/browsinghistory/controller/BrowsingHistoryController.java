package com.example.shopping.browsinghistory.controller;

import com.example.shopping.browsinghistory.dto.response.BrowsingHistoryItemResponse;
import com.example.shopping.browsinghistory.service.BrowsingHistoryService;
import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.security.SecurityUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/browsing-history")
public class BrowsingHistoryController {

    private final BrowsingHistoryService browsingHistoryService;

    public BrowsingHistoryController(BrowsingHistoryService browsingHistoryService) {
        this.browsingHistoryService = browsingHistoryService;
    }

    @GetMapping
    public ApiResponse<PageResponse<BrowsingHistoryItemResponse>> list(
            @PageableDefault(size = 12, sort = "viewedAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ApiResponse.success(PageResponse.from(
                browsingHistoryService.list(SecurityUtils.getCurrentUserId(), pageable)));
    }

    @PostMapping("/{productId}")
    public ApiResponse<Void> recordView(@PathVariable Long productId) {
        browsingHistoryService.recordView(SecurityUtils.getCurrentUserId(), productId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{productId}")
    public ApiResponse<Void> remove(@PathVariable Long productId) {
        browsingHistoryService.remove(SecurityUtils.getCurrentUserId(), productId);
        return ApiResponse.success("已刪除", null);
    }

    @DeleteMapping
    public ApiResponse<Void> clear() {
        browsingHistoryService.clear(SecurityUtils.getCurrentUserId());
        return ApiResponse.success("已清空瀏覽紀錄", null);
    }
}
