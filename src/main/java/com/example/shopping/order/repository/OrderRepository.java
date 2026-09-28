package com.example.shopping.order.repository;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.order.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Orders, Long>, JpaSpecificationExecutor<Orders> {

    Optional<Orders> findByIdAndMemberId(Long id, Long memberId);

    long countByStatus(OrderStatus status);

    boolean existsByMemberIdAndStatusIn(Long memberId, Collection<OrderStatus> statuses);

    long countByMemberIdAndCouponIdAndStatusNot(Long memberId, Long couponId, OrderStatus status);

    /** 會員在某時間之後、指定狀態訂單的商品金額合計(實付扣掉運費) */
    @Query("select sum(o.totalAmount - o.shippingFee - o.giftWrapFee) from Orders o "
            + "where o.member.id = :memberId and o.status = :status and o.createdAt >= :since")
    BigDecimal sumMerchandiseAmount(@Param("memberId") Long memberId, @Param("status") OrderStatus status,
                                    @Param("since") LocalDateTime since);

    /** 某時間之後指定狀態訂單的商品金額合計達門檻的啟用會員 id(依等級發券用) */
    @Query("select o.member.id from Orders o where o.member.status = com.example.shopping.common.enums.AccountStatus.ACTIVE"
            + " and o.status = :status and o.createdAt >= :since"
            + " group by o.member.id having sum(o.totalAmount - o.shippingFee - o.giftWrapFee) >= :threshold")
    List<Long> findMemberIdsWithMerchandiseAmountAtLeast(@Param("status") OrderStatus status,
                                                          @Param("since") LocalDateTime since,
                                                          @Param("threshold") BigDecimal threshold);

    List<Orders> findByStatusAndShippedAtBefore(OrderStatus status, LocalDateTime shippedBefore);

    List<Orders> findByOrderNoIn(Collection<String> orderNos);

    /** 待出貨清單(出貨單號匯入範本用),最舊的排前面 */
    List<Orders> findByStatusOrderByCreatedAtAsc(OrderStatus status);

    List<Orders> findByStatusAndPaymentDeadlineBefore(OrderStatus status, LocalDateTime deadline);
}
