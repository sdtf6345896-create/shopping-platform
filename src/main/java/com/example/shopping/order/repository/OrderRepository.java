package com.example.shopping.order.repository;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.order.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Orders, Long>, JpaSpecificationExecutor<Orders> {

    Optional<Orders> findByIdAndMemberId(Long id, Long memberId);

    long countByStatus(OrderStatus status);

    List<Orders> findByStatusAndPaymentDeadlineBefore(OrderStatus status, LocalDateTime deadline);
}
