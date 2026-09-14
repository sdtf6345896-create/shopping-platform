package com.example.shopping.order.controller;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.order.dto.request.OrderStatusRequest;
import com.example.shopping.order.dto.response.OrderResponse;
import com.example.shopping.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ApiResponse<PageResponse<OrderResponse>> list(
            @RequestParam(required = false) OrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        return ApiResponse.success(PageResponse.from(orderService.listAdmin(status, pageable)));
    }

    @GetMapping("/{id}")
    public ApiResponse<OrderResponse> detail(@PathVariable Long id) {
        return ApiResponse.success(orderService.getAdminOrder(id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<OrderResponse> updateStatus(@PathVariable Long id,
                                                    @Valid @RequestBody OrderStatusRequest request) {
        return ApiResponse.success("狀態更新成功", orderService.updateStatus(id, request));
    }
}
