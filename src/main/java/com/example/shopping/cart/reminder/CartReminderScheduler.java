package com.example.shopping.cart.reminder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class CartReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(CartReminderScheduler.class);

    private final CartReminderService cartReminderService;

    public CartReminderScheduler(CartReminderService cartReminderService) {
        this.cartReminderService = cartReminderService;
    }

    @Scheduled(fixedDelayString = "${app.cart.reminder-check-interval-ms:3600000}", initialDelay = 120000)
    public void sendReminders() {
        int sent = cartReminderService.sendReminders(LocalDateTime.now());
        if (sent > 0) {
            log.info("已發送 {} 則購物車提醒", sent);
        }
    }
}
