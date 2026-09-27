package com.example.shopping.returns.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.order.dto.response.OrderResponse;
import com.example.shopping.returns.dto.request.ReturnApplyRequest;
import com.example.shopping.returns.service.ReturnService;
import com.example.shopping.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReturnController {

    private final ReturnService returnService;

    public ReturnController(ReturnService returnService) {
        this.returnService = returnService;
    }

    @PostMapping("/api/orders/{orderId}/return")
    public ApiResponse<OrderResponse> apply(@PathVariable Long orderId,
                                            @Valid @RequestBody ReturnApplyRequest request) {
        return ApiResponse.success("退貨申請已送出",
                returnService.apply(SecurityUtils.getCurrentUserId(), orderId, request));
    }
}
