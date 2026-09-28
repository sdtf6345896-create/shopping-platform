package com.example.shopping;

import com.example.shopping.common.enums.AccountStatus;
import com.example.shopping.common.enums.PointTransactionType;
import com.example.shopping.member.birthday.BirthdayRewardService;
import com.example.shopping.member.entity.Member;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.notification.repository.NotificationRepository;
import com.example.shopping.points.entity.PointTransaction;
import com.example.shopping.points.repository.PointTransactionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** 生日當月發購物金 + 通知;一年只發一次;停用帳號與非當月壽星不發;隔年再發 */
@SpringBootTest
@ActiveProfiles("test")
class BirthdayRewardIntegrationTest {

    @Autowired
    private BirthdayRewardService birthdayRewardService;
    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private PointTransactionRepository pointTransactionRepository;
    @Autowired
    private NotificationRepository notificationRepository;

    private Member member(LocalDate birthday, AccountStatus status) {
        Member member = new Member();
        member.setEmail("bday-" + UUID.randomUUID().toString().substring(0, 8) + "@example.com");
        member.setPassword("x");
        member.setName("壽星");
        member.setEmailVerified(true);
        member.setStatus(status);
        member.setBirthday(birthday);
        return memberRepository.save(member);
    }

    private List<PointTransaction> transactionsOf(Member member) {
        return pointTransactionRepository.findByMemberId(member.getId(), PageRequest.of(0, 50)).getContent();
    }

    @Test
    void grantsOncePerYearToActiveBirthdayMembers() {
        int reward = birthdayRewardService.getRewardPoints();
        Member birthdayMember = member(LocalDate.of(1990, 5, 20), AccountStatus.ACTIVE);
        Member disabled = member(LocalDate.of(1991, 5, 3), AccountStatus.DISABLED);
        Member otherMonth = member(LocalDate.of(1992, 6, 1), AccountStatus.ACTIVE);
        Member noBirthday = member(null, AccountStatus.ACTIVE);

        birthdayRewardService.grantRewards(LocalDate.of(2031, 5, 1));

        assertThat(memberRepository.findPointsById(birthdayMember.getId())).isEqualTo(reward);
        assertThat(transactionsOf(birthdayMember)).singleElement()
                .satisfies(tx -> assertThat(tx.getType()).isEqualTo(PointTransactionType.BIRTHDAY));
        assertThat(notificationRepository.findByMemberId(birthdayMember.getId(), PageRequest.of(0, 10))
                .getTotalElements()).isEqualTo(1);
        for (Member skipped : List.of(disabled, otherMonth, noBirthday)) {
            assertThat(memberRepository.findPointsById(skipped.getId())).isZero();
        }

        // 同月再跑(每小時排程)不會重複發
        birthdayRewardService.grantRewards(LocalDate.of(2031, 5, 31));
        assertThat(memberRepository.findPointsById(birthdayMember.getId())).isEqualTo(reward);

        // 隔年生日月再發一次
        birthdayRewardService.grantRewards(LocalDate.of(2032, 5, 2));
        assertThat(memberRepository.findPointsById(birthdayMember.getId())).isEqualTo(reward * 2);
        assertThat(transactionsOf(birthdayMember)).hasSize(2);
    }
}
