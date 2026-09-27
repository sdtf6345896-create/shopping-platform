package com.example.shopping.points.service;

import com.example.shopping.common.enums.PointTransactionType;
import com.example.shopping.common.exception.BusinessException;
import com.example.shopping.common.exception.ResourceNotFoundException;
import com.example.shopping.member.repository.MemberRepository;
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

    public PointServiceImpl(MemberRepository memberRepository,
                            PointTransactionRepository transactionRepository,
                            PointPolicy pointPolicy) {
        this.memberRepository = memberRepository;
        this.transactionRepository = transactionRepository;
        this.pointPolicy = pointPolicy;
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
