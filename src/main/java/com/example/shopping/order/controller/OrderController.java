package com.example.shopping.order.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.order.dto.request.CheckoutRequest;
import com.example.shopping.order.dto.response.OrderResponse;
import com.example.shopping.order.service.OrderService;
import com.example.shopping.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> list(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 10, sort = "createdAt") Pageable pageable) {

        return ApiResponse.success(PageResponse.from(
                orderService.listMyOrders(SecurityUtils.getCurrentUserId(), status, pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(orderService.getMyOrder(SecurityUtils.getCurrentUserId(), id));
    }

    @PostMapping
    public ApiResponse<OrderResponse> checkout(@Valid @RequestBody CheckoutRequest request) {
        return ApiResponse.success("訂單建立成功", orderService.checkout(SecurityUtils.getCurrentUserId(), request));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<OrderResponse> pay(@PathVariable Long id) {
        return ApiResponse.success("付款成功", orderService.pay(SecurityUtils.getCurrentUserId(), id));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<OrderResponse> cancel(@PathVariable Long id) {
        return ApiResponse.success("訂單已取消", orderService.cancelByMember(SecurityUtils.getCurrentUserId(), id));
    }
}
