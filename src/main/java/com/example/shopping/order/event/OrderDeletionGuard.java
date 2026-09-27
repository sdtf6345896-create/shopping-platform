package com.example.shopping.order.event;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.member.event.MemberDeletingEvent;
import com.example.shopping.order.repository.OrderItemRepository;
import com.example.shopping.order.repository.OrderRepository;
import com.example.shopping.product.event.ProductDeletingEvent;
import com.example.shopping.product.event.SkusRemovingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

/**
 * 訂單模組對其他模組刪除動作的把關:
 * 已經被下單過的商品 / 規格不能刪除(訂單明細需要保留);還有處理中訂單的會員不能刪除帳號。
 */
@Component
public class OrderDeletionGuard {

    private final OrderItemRepository orderItemRepository;
    private final OrderRepository orderRepository;

    public OrderDeletionGuard(OrderItemRepository orderItemRepository, OrderRepository orderRepository) {
        this.orderItemRepository = orderItemRepository;
        this.orderRepository = orderRepository;
    }

    @EventListener
    public void onProductDeleting(ProductDeletingEvent event) {
        if (orderItemRepository.existsByProductSkuProductId(event.productId())) {
            throw new BusinessException("「" + event.productName() + "」已有訂單,無法刪除,請改為下架");
        }
    }

    @EventListener
    public void onSkusRemoving(SkusRemovingEvent event) {
        if (orderItemRepository.existsByProductSkuIdIn(event.skuIds())) {
            String labels = String.join("、", event.skus().stream().map(SkusRemovingEvent.RemovedSku::label).toList());
            throw new BusinessException("規格已有訂單,無法刪除(" + labels + "),可將庫存設為 0 停售");
        }
    }

    @EventListener
    public void onMemberDeleting(MemberDeletingEvent event) {
        if (orderRepository.existsByMemberIdAndStatusIn(event.memberId(),
                EnumSet.of(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID, OrderStatus.SHIPPING))) {
            throw new BusinessException("尚有處理中的訂單(待付款 / 已付款 / 出貨中),請等訂單完成或取消後再刪除帳號");
        }
    }
}
