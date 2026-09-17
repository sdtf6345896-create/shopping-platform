package com.example.shopping.banner.controller;

import com.example.shopping.banner.dto.BannerRequest;
import com.example.shopping.banner.dto.BannerResponse;
import com.example.shopping.banner.dto.BannerStatusRequest;
import com.example.shopping.banner.service.BannerService;
import com.example.shopping.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/banners")
public class AdminBannerController {

    private final BannerService bannerService;

    public AdminBannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping
    public ApiResponse<List<BannerResponse>> list() {
        return ApiResponse.success(bannerService.listAll());
    }

    @PostMapping
    public ApiResponse<BannerResponse> create(@Valid @RequestBody BannerRequest request) {
        return ApiResponse.success("新增成功", bannerService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<BannerResponse> update(@PathVariable Long id, @Valid @RequestBody BannerRequest request) {
        return ApiResponse.success("更新成功", bannerService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<BannerResponse> updateStatus(@PathVariable Long id,
                                                      @Valid @RequestBody BannerStatusRequest request) {
        return ApiResponse.success("狀態更新成功", bannerService.updateStatus(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bannerService.delete(id);
        return ApiResponse.success("刪除成功", null);
    }
}
