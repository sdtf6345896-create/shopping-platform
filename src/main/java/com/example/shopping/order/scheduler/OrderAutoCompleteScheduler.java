package com.example.shopping.order.scheduler;

import com.example.shopping.order.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** 出貨一段時間後會員仍未確認收貨,視為已收到並自動完成訂單(觸發購物金回饋、起算退貨鑑賞期) */
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class OrderAutoCompleteScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderAutoCompleteScheduler.class);

    private final OrderService orderService;
    private final long autoCompleteDays;

    public OrderAutoCompleteScheduler(OrderService orderService,
                                      @Value("${app.order.auto-complete-days:7}") long autoCompleteDays) {
        this.orderService = orderService;
        this.autoCompleteDays = autoCompleteDays;
    }

    @Scheduled(fixedDelayString = "${app.order.auto-complete-check-interval-ms:3600000}",
            initialDelayString = "${app.order.auto-complete-check-interval-ms:3600000}")
    public void autoComplete() {
        int completed = orderService.autoCompleteShipped(LocalDateTime.now().minusDays(autoCompleteDays));
        if (completed > 0) {
            log.info("已自動完成 {} 筆出貨超過 {} 天的訂單", completed, autoCompleteDays);
        }
    }
}
