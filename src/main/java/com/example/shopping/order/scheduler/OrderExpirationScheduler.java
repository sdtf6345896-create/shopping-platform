package com.example.shopping.order.scheduler;

import com.example.shopping.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 定期取消逾期未付款的訂單,歸還庫存與優惠券名額 */
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class OrderExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderExpirationScheduler.class);

    private final OrderService orderService;

    public OrderExpirationScheduler(OrderService orderService) {
        this.orderService = orderService;
    }

    @Scheduled(fixedDelayString = "${app.order.expire-check-interval-ms:60000}",
            initialDelayString = "${app.order.expire-check-interval-ms:60000}")
    public void cancelExpiredOrders() {
        int cancelled = orderService.cancelExpiredOrders(LocalDateTime.now());
        if (cancelled > 0) {
            log.info("已自動取消 {} 筆逾期未付款訂單", cancelled);
        }
    }
}
