package com.example.shopping.stockalert.scheduler;

import com.example.shopping.stockalert.service.StockAlertService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定期找出已補貨的訂閱並通知。用排程而不是在每個「加庫存」的地方觸發,
 * 因為庫存可能來自後台修改、編輯商品、取消訂單、退貨等多種路徑。
 */
@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class StockAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(StockAlertScheduler.class);

    private final StockAlertService stockAlertService;

    public StockAlertScheduler(StockAlertService stockAlertService) {
        this.stockAlertService = stockAlertService;
    }

    @Scheduled(fixedDelayString = "${app.stock-alert.check-interval-ms:60000}",
            initialDelayString = "${app.stock-alert.check-interval-ms:60000}")
    public void notifyRestocked() {
        int notified = stockAlertService.notifyRestocked();
        if (notified > 0) {
            log.info("已發送 {} 則貨到通知", notified);
        }
    }
}
