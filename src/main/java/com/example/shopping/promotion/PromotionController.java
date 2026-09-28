package com.example.shopping.promotion;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.security.SecurityUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    /** 公開:進行中的滿件折扣,商品頁用來顯示「任選 2 件 9 折」標籤 */
    @GetMapping("/api/promotions")
    public ApiResponse<List<PromotionResponse>> running() {
        LocalDateTime now = LocalDateTime.now();
        return ApiResponse.success(promotionService.listRunning(now).stream()
                .map(p -> PromotionResponse.from(p, now)).toList());
    }

    /** 會員:以購物車(或指定項目)試算滿件折扣與「再買幾件」提示 */
    @GetMapping("/api/cart/promotion")
    public ApiResponse<PromotionCalculator.Result> preview(@RequestParam(required = false) List<Long> cartItemIds) {
        return ApiResponse.success(promotionService.previewForMember(SecurityUtils.getCurrentUserId(), cartItemIds));
    }
}
