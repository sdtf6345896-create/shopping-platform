package com.example.shopping.member.birthday;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@ConditionalOnProperty(name = "app.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class BirthdayRewardScheduler {

    private static final Logger log = LoggerFactory.getLogger(BirthdayRewardScheduler.class);

    private final BirthdayRewardService birthdayRewardService;

    public BirthdayRewardScheduler(BirthdayRewardService birthdayRewardService) {
        this.birthdayRewardService = birthdayRewardService;
    }

    /** 每小時檢查一次(認領是冪等的),服務停機或帳號重新啟用後很快就會補發 */
    @Scheduled(fixedDelayString = "${app.birthday.check-interval-ms:3600000}", initialDelay = 60000)
    public void grantRewards() {
        int granted = birthdayRewardService.grantRewards(LocalDate.now());
        if (granted > 0) {
            log.info("已發放 {} 份生日禮", granted);
        }
    }
}
