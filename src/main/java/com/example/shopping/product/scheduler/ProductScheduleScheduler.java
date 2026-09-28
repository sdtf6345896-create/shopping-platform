package com.example.shopping.product.scheduler;

import com.example.shopping.product.service.ProductScheduleService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class ProductScheduleScheduler {

    private static final Logger log = LoggerFactory.getLogger(ProductScheduleScheduler.class);

    private final ProductScheduleService productScheduleService;

    public ProductScheduleScheduler(ProductScheduleService productScheduleService) {
        this.productScheduleService = productScheduleService;
    }

    @Scheduled(fixedDelayString = "${app.product.schedule-check-interval-ms:60000}", initialDelay = 10000)
    public void applyDue() {
        ProductScheduleService.Result result = productScheduleService.applyDue(LocalDateTime.now());
        if (result.published() > 0 || result.unpublished() > 0) {
            log.info("商品排程:上架 {} 件、下架 {} 件", result.published(), result.unpublished());
        }
    }
}
