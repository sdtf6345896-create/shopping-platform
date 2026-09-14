package com.example.shopping.order.repository;

import com.example.shopping.order.entity.Orders;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Orders, Long>, JpaSpecificationExecutor<Orders> {

    Optional<Orders> findByIdAndMemberId(Long id, Long memberId);
}
