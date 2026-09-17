package com.example.shopping.banner.controller;

import com.example.shopping.banner.dto.BannerResponse;
import com.example.shopping.banner.service.BannerService;
import com.example.shopping.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/banners")
public class BannerController {

    private final BannerService bannerService;

    public BannerController(BannerService bannerService) {
        this.bannerService = bannerService;
    }

    @GetMapping
    public ApiResponse<List<BannerResponse>> list() {
        return ApiResponse.success(bannerService.getActiveBanners());
    }
}
