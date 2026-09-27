package com.example.shopping.wishlist.scheduler;

import com.example.shopping.wishlist.service.WishlistSaleNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class WishlistSaleScheduler {

    private static final Logger log = LoggerFactory.getLogger(WishlistSaleScheduler.class);

    private final WishlistSaleNotifier wishlistSaleNotifier;

    public WishlistSaleScheduler(WishlistSaleNotifier wishlistSaleNotifier) {
        this.wishlistSaleNotifier = wishlistSaleNotifier;
    }

    @Scheduled(fixedDelayString = "${app.wishlist.sale-check-interval-ms:300000}",
            initialDelayString = "${app.wishlist.sale-check-interval-ms:300000}")
    public void notifySales() {
        int sent = wishlistSaleNotifier.notifySales(LocalDateTime.now());
        if (sent > 0) {
            log.info("已發送 {} 則收藏商品特價通知", sent);
        }
    }
}
