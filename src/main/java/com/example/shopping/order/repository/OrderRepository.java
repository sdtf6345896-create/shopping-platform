package com.example.shopping.order.repository;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.order.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Orders, Long>, JpaSpecificationExecutor<Orders> {

    Optional<Orders> findByIdAndMemberId(Long id, Long memberId);

    long countByStatus(OrderStatus status);

    /** 會員在某時間之後、指定狀態訂單的商品金額合計(實付扣掉運費) */
    @Query("select sum(o.totalAmount - o.shippingFee) from Orders o "
            + "where o.member.id = :memberId and o.status = :status and o.createdAt >= :since")
    BigDecimal sumMerchandiseAmount(@Param("memberId") Long memberId, @Param("status") OrderStatus status,
                                    @Param("since") LocalDateTime since);

    List<Orders> findByStatusAndPaymentDeadlineBefore(OrderStatus status, LocalDateTime deadline);
}
