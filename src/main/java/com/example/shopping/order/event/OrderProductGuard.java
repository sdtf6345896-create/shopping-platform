package com.example.shopping.order.event;

import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.order.repository.OrderItemRepository;
import com.example.shopping.product.event.ProductDeletingEvent;
import com.example.shopping.product.event.SkusRemovingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** 已經被下單過的商品 / 規格不能刪除(訂單明細需要保留),改為下架或把庫存設為 0 */
@Component
public class OrderProductGuard {

    private final OrderItemRepository orderItemRepository;

    public OrderProductGuard(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
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
}
