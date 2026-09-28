package com.example.shopping.order.message;

import com.example.shopping.audit.AdminAudit;
import com.example.shopping.audit.AuditTarget;
import com.example.shopping.common.ApiResponse;
import com.example.shopping.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminOrderMessageController {

    private final OrderMessageService orderMessageService;

    public AdminOrderMessageController(OrderMessageService orderMessageService) {
        this.orderMessageService = orderMessageService;
    }

    /** 最後一則是會員留言、等待賣家回覆的訂單 */
    @GetMapping("/order-messages/awaiting")
    public ApiResponse<PageResponse<AwaitingMessageResponse>> awaiting(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(PageResponse.from(orderMessageService.listAwaitingReply(pageable)));
    }

    @GetMapping("/orders/{orderId}/messages")
    public ApiResponse<List<OrderMessageResponse>> list(@PathVariable Long orderId) {
        return ApiResponse.success(orderMessageService.listForAdmin(orderId));
    }

    @AdminAudit(action = "回覆訂單留言", target = AuditTarget.ORDER, targetId = "#orderId")
    @PostMapping("/orders/{orderId}/messages")
    public ApiResponse<OrderMessageResponse> reply(@PathVariable Long orderId,
                                                   @Valid @RequestBody OrderMessageRequest request) {
        return ApiResponse.success("已回覆", orderMessageService.replyByAdmin(orderId, request.getContent()));
    }
}
