package com.example.shopping.member.tier;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 會員等級即時由訂單計算,不另外存欄位,不會有等級與消費紀錄不同步的問題 */
@Service
@Transactional(readOnly = true)
public class MemberTierService {

    static final int LOOKBACK_MONTHS = 12;

    private final OrderRepository orderRepository;

    public MemberTierService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public MemberTier tierOf(Long memberId) {
        return MemberTier.forSpending(spendingOf(memberId));
    }

    public MemberTierResponse describe(Long memberId) {
        BigDecimal spending = spendingOf(memberId);
        MemberTier tier = MemberTier.forSpending(spending);
        MemberTier next = tier.next();
        BigDecimal toNext = next == null ? BigDecimal.ZERO : next.getThreshold().subtract(spending).max(BigDecimal.ZERO);
        return new MemberTierResponse(tier, tier.getLabel(), tier.getPointsMultiplier(), spending,
                next, next == null ? null : next.getLabel(), toNext);
    }

    private BigDecimal spendingOf(Long memberId) {
        BigDecimal sum = orderRepository.sumMerchandiseAmount(memberId, OrderStatus.COMPLETED,
                LocalDateTime.now().minusMonths(LOOKBACK_MONTHS));
        return sum == null ? BigDecimal.ZERO : sum;
    }
}
