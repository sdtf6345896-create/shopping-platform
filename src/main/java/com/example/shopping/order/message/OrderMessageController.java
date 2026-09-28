package com.example.shopping.order.message;

import com.example.shopping.common.ApiResponse;
import com.example.shopping.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders/{orderId}/messages")
public class OrderMessageController {

    private final OrderMessageService orderMessageService;

    public OrderMessageController(OrderMessageService orderMessageService) {
        this.orderMessageService = orderMessageService;
    }

    @GetMapping
    public ApiResponse<List<OrderMessageResponse>> list(@PathVariable Long orderId) {
        return ApiResponse.success(orderMessageService.listForMember(SecurityUtils.getCurrentUserId(), orderId));
    }

    @PostMapping
    public ApiResponse<OrderMessageResponse> post(@PathVariable Long orderId,
                                                  @Valid @RequestBody OrderMessageRequest request) {
        return ApiResponse.success("留言已送出",
                orderMessageService.postByMember(SecurityUtils.getCurrentUserId(), orderId, request.getContent()));
    }
}
