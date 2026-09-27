package com.example.shopping.stockalert.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.security.SecurityUtils;
import com.example.shopping.stockalert.service.StockAlertService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stock-alerts")
public class StockAlertController {

    private final StockAlertService stockAlertService;

    public StockAlertController(StockAlertService stockAlertService) {
        this.stockAlertService = stockAlertService;
    }

    /** 我在某商品已訂閱貨到通知的規格 id */
    @GetMapping
    public ApiResponse<List<Long>> mine(@RequestParam Long productId) {
        return ApiResponse.success(stockAlertService.subscribedSkuIds(SecurityUtils.getCurrentUserId(), productId));
    }

    @PostMapping("/{skuId}")
    public ApiResponse<Void> subscribe(@PathVariable Long skuId) {
        stockAlertService.subscribe(SecurityUtils.getCurrentUserId(), skuId);
        return ApiResponse.success("到貨時會通知您", null);
    }

    @DeleteMapping("/{skuId}")
    public ApiResponse<Void> unsubscribe(@PathVariable Long skuId) {
        stockAlertService.unsubscribe(SecurityUtils.getCurrentUserId(), skuId);
        return ApiResponse.success("已取消貨到通知", null);
    }
}
