package com.example.shopping.points.service;

import com.example.shopping.common.enums.NotificationType;
import com.example.shopping.common.enums.PointTransactionType;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.notification.service.NotificationService;
import com.example.shopping.points.entity.PointTransaction;
import com.example.shopping.points.repository.PointTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PointServiceImplTest {

    @Mock
    private MemberRepository memberRepository;
    @Mock
    private PointTransactionRepository transactionRepository;
    @Mock
    private NotificationService notificationService;

    private PointServiceImpl pointService;

    @BeforeEach
    void setUp() {
        pointService = new PointServiceImpl(memberRepository, transactionRepository,
                new PointPolicy(new BigDecimal("0.01"), new BigDecimal("0.5")), notificationService);
    }

    @Test
    void deduct_recordsNegativeTransactionWithNewBalance() {
        when(memberRepository.deductPoints(1L, 200)).thenReturn(1);
        when(memberRepository.findPointsById(1L)).thenReturn(300);

        pointService.deduct(1L, 9L, 200, PointTransactionType.REDEEM, "訂單折抵");

        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualTo(-200);
        assertThat(captor.getValue().getBalanceAfter()).isEqualTo(300);
        assertThat(captor.getValue().getOrderId()).isEqualTo(9L);
    }

    @Test
    void deduct_throws_whenBalanceInsufficient() {
        when(memberRepository.deductPoints(1L, 200)).thenReturn(0);

        assertThatThrownBy(() -> pointService.deduct(1L, 9L, 200, PointTransactionType.REDEEM, "訂單折抵"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("餘額不足");
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void credit_recordsPositiveTransaction() {
        when(memberRepository.findPointsById(1L)).thenReturn(510);

        pointService.credit(1L, 9L, 10, PointTransactionType.EARN, "訂單完成回饋");

        verify(memberRepository).addPoints(1L, 10);
        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualTo(10);
        assertThat(captor.getValue().getBalanceAfter()).isEqualTo(510);
    }

    @Test
    void rejectsNonPositiveAmounts() {
        assertThatThrownBy(() -> pointService.credit(1L, null, 0, PointTransactionType.ADJUST, "x"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void adjust_grantsPointsAndNotifiesMember() {
        when(memberRepository.existsById(1L)).thenReturn(true);
        when(memberRepository.findPointsById(1L)).thenReturn(150);

        assertThat(pointService.adjust(1L, 100, " 客服補償 ").getBalance()).isEqualTo(150);

        verify(memberRepository).addPoints(1L, 100);
        ArgumentCaptor<PointTransaction> captor = ArgumentCaptor.forClass(PointTransaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(PointTransactionType.ADJUST);
        assertThat(captor.getValue().getDescription()).isEqualTo("客服補償");
        verify(notificationService).notify(eq(1L), eq(NotificationType.SYSTEM), eq("獲得 100 點購物金"),
                anyString(), eq("/member/points"));
    }

    @Test
    void adjust_negativeAmountDeducts() {
        when(memberRepository.existsById(1L)).thenReturn(true);
        when(memberRepository.deductPoints(1L, 30)).thenReturn(1);
        when(memberRepository.findPointsById(1L)).thenReturn(20);

        pointService.adjust(1L, -30, "誤發收回");

        verify(memberRepository).deductPoints(1L, 30);
    }

    @Test
    void adjust_rejectsZeroAndUnknownMember() {
        assertThatThrownBy(() -> pointService.adjust(1L, 0, "x")).isInstanceOf(BusinessException.class);

        when(memberRepository.existsById(99L)).thenReturn(false);
        assertThatThrownBy(() -> pointService.adjust(99L, 10, "x")).isInstanceOf(ResourceNotFoundException.class);
        verify(notificationService, never()).notify(any(), any(), any(), any(), any());
    }
}
