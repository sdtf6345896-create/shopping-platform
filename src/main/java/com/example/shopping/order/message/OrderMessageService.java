package com.example.shopping.order.message;

import com.example.shopping.common.enums.NotificationType;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.notification.service.NotificationService;
import com.example.shopping.order.entity.Orders;
import com.example.shopping.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 訂單留言:會員針對自己的訂單留言,賣家(管理員)回覆後以站內通知提醒會員 */
@Service
@Transactional
public class OrderMessageService {

    /** 單一訂單的留言上限,避免被拿來灌訊息 */
    static final int MAX_MESSAGES_PER_ORDER = 100;

    private final OrderMessageRepository messageRepository;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;

    public OrderMessageService(OrderMessageRepository messageRepository,
                               OrderRepository orderRepository,
                               NotificationService notificationService) {
        this.messageRepository = messageRepository;
        this.orderRepository = orderRepository;
        this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public List<OrderMessageResponse> listForMember(Long memberId, Long orderId) {
        return list(ownedOrder(memberId, orderId).getId());
    }

    public OrderMessageResponse postByMember(Long memberId, Long orderId, String content) {
        return post(ownedOrder(memberId, orderId), MessageSender.MEMBER, content);
    }

    @Transactional(readOnly = true)
    public List<OrderMessageResponse> listForAdmin(Long orderId) {
        return list(anyOrder(orderId).getId());
    }

    public OrderMessageResponse replyByAdmin(Long orderId, String content) {
        Orders order = anyOrder(orderId);
        OrderMessageResponse reply = post(order, MessageSender.ADMIN, content);
        notificationService.notify(order.getMember().getId(), NotificationType.ORDER, "賣家回覆了你的訂單留言",
                "訂單 " + order.getOrderNo() + ":" + abbreviate(reply.content()), "/orders/" + order.getId());
        return reply;
    }

    @Transactional(readOnly = true)
    public Page<AwaitingMessageResponse> listAwaitingReply(Pageable pageable) {
        return messageRepository.findAwaitingReply(pageable).map(AwaitingMessageResponse::from);
    }

    private List<OrderMessageResponse> list(Long orderId) {
        return messageRepository.findByOrderIdOrderByIdAsc(orderId).stream().map(OrderMessageResponse::from).toList();
    }

    private OrderMessageResponse post(Orders order, MessageSender sender, String content) {
        if (messageRepository.countByOrderId(order.getId()) >= MAX_MESSAGES_PER_ORDER) {
            throw new BusinessException("此訂單的留言已達上限,請改用客服信箱聯絡");
        }
        OrderMessage message = new OrderMessage();
        message.setOrder(order);
        message.setSender(sender);
        message.setContent(content.trim());
        return OrderMessageResponse.from(messageRepository.save(message));
    }

    private Orders ownedOrder(Long memberId, Long orderId) {
        return orderRepository.findByIdAndMemberId(orderId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("訂單不存在"));
    }

    private Orders anyOrder(Long orderId) {
        return orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("訂單不存在"));
    }

    private static String abbreviate(String text) {
        return text.length() <= 60 ? text : text.substring(0, 60) + "…";
    }
}
