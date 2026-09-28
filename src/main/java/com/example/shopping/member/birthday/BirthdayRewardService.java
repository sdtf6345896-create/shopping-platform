package com.example.shopping.member.birthday;

import com.example.shopping.common.enums.NotificationType;
import com.example.shopping.common.enums.PointTransactionType;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.notification.service.NotificationService;
import com.example.shopping.points.service.PointService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

/**
 * 生日禮:生日當月發放購物金,每位會員每年一次。
 * 每位會員各自一筆交易,單一會員失敗不影響其他人;認領靠條件式 UPDATE,多台機器同時跑也不會重複發。
 */
@Service
public class BirthdayRewardService {

    private static final Logger log = LoggerFactory.getLogger(BirthdayRewardService.class);

    private final MemberRepository memberRepository;
    private final PointService pointService;
    private final NotificationService notificationService;
    private final TransactionTemplate transactionTemplate;
    private final int rewardPoints;

    public BirthdayRewardService(MemberRepository memberRepository,
                                 PointService pointService,
                                 NotificationService notificationService,
                                 PlatformTransactionManager transactionManager,
                                 @Value("${app.birthday.reward-points:100}") int rewardPoints) {
        this.memberRepository = memberRepository;
        this.pointService = pointService;
        this.notificationService = notificationService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.rewardPoints = rewardPoints;
    }

    public int getRewardPoints() {
        return rewardPoints;
    }

    /** @return 本次發放生日禮的人數 */
    public int grantRewards(LocalDate today) {
        if (rewardPoints <= 0) {
            return 0;
        }
        int year = today.getYear();
        List<Long> candidates = memberRepository.findBirthdayRewardCandidates(today.getMonthValue(), year);
        int granted = 0;
        for (Long memberId : candidates) {
            try {
                if (Boolean.TRUE.equals(transactionTemplate.execute(status -> grant(memberId, year)))) {
                    granted++;
                }
            } catch (RuntimeException e) {
                log.warn("會員 {} 生日禮發放失敗,下次排程再試", memberId, e);
            }
        }
        return granted;
    }

    private boolean grant(Long memberId, int year) {
        if (memberRepository.claimBirthdayReward(memberId, year) == 0) {
            return false;
        }
        pointService.credit(memberId, null, rewardPoints, PointTransactionType.BIRTHDAY, year + " 年生日禮");
        notificationService.notify(memberId, NotificationType.SYSTEM, "生日快樂!送你 " + rewardPoints + " 點購物金",
                "祝你生日快樂!生日禮購物金已入帳,結帳時可折抵使用。", "/member/points");
        return true;
    }
}
