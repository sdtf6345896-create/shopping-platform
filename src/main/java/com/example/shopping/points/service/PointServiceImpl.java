package com.example.shopping.points.service;

import com.example.shopping.common.enums.NotificationType;
import com.example.shopping.common.enums.PointTransactionType;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.repository.MemberRepository;
import com.example.shopping.notification.service.NotificationService;
import com.example.shopping.points.dto.PointBalanceResponse;
import com.example.shopping.points.dto.PointTransactionResponse;
import com.example.shopping.points.entity.PointTransaction;
import com.example.shopping.points.repository.PointTransactionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PointServiceImpl implements PointService {

    private final MemberRepository memberRepository;
    private final PointTransactionRepository transactionRepository;
    private final PointPolicy pointPolicy;
    private final NotificationService notificationService;

    public PointServiceImpl(MemberRepository memberRepository,
                            PointTransactionRepository transactionRepository,
                            PointPolicy pointPolicy,
                            NotificationService notificationService) {
        this.memberRepository = memberRepository;
        this.transactionRepository = transactionRepository;
        this.pointPolicy = pointPolicy;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional(readOnly = true)
    public PointBalanceResponse getBalance(Long memberId) {
        return new PointBalanceResponse(currentBalance(memberId),
                pointPolicy.getEarnRate(), pointPolicy.getMaxRedeemRatio());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PointTransactionResponse> listTransactions(Long memberId, Pageable pageable) {
        return transactionRepository.findByMemberId(memberId, pageable).map(PointTransactionResponse::from);
    }

    @Override
    public void deduct(Long memberId, Long orderId, int amount, PointTransactionType type, String description) {
        requirePositive(amount);
        // 條件式 UPDATE:餘額足夠才扣,一條 SQL 完成檢查與扣除,不會有併發超扣
        if (memberRepository.deductPoints(memberId, amount) == 0) {
            throw new BusinessException("購物金餘額不足");
        }
        record(memberId, orderId, -amount, type, description);
    }

    @Override
    public void credit(Long memberId, Long orderId, int amount, PointTransactionType type, String description) {
        requirePositive(amount);
        memberRepository.addPoints(memberId, amount);
        record(memberId, orderId, amount, type, description);
    }

    @Override
    public PointBalanceResponse adjust(Long memberId, int amount, String reason) {
        if (amount == 0) {
            throw new BusinessException("調整點數不可為 0");
        }
        if (!memberRepository.existsById(memberId)) {
            throw new ResourceNotFoundException("會員不存在");
        }
        String description = reason.trim();
        if (amount > 0) {
            credit(memberId, null, amount, PointTransactionType.ADJUST, description);
            notificationService.notify(memberId, NotificationType.SYSTEM, "獲得 " + amount + " 點購物金",
                    description + ",結帳時可折抵使用。", "/member/points");
        } else {
            deduct(memberId, null, -amount, PointTransactionType.ADJUST, description);
            notificationService.notify(memberId, NotificationType.SYSTEM, "購物金調整 " + amount + " 點",
                    description, "/member/points");
        }
        return getBalance(memberId);
    }

    private void record(Long memberId, Long orderId, int amount, PointTransactionType type, String description) {
        PointTransaction tx = new PointTransaction();
        tx.setMemberId(memberId);
        tx.setOrderId(orderId);
        tx.setAmount(amount);
        tx.setType(type);
        tx.setDescription(description);
        tx.setBalanceAfter(currentBalance(memberId));
        transactionRepository.save(tx);
    }

    private int currentBalance(Long memberId) {
        Integer balance = memberRepository.findPointsById(memberId);
        if (balance == null) {
            throw new ResourceNotFoundException("會員不存在");
        }
        return balance;
    }

    private static void requirePositive(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("購物金異動數量必須大於 0");
        }
    }
}
