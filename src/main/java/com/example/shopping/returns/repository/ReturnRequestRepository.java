package com.example.shopping.returns.repository;

import com.example.shopping.common.enums.ReturnStatus;
import com.example.shopping.returns.entity.ReturnRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {

    boolean existsByOrderId(Long orderId);

    Page<ReturnRequest> findByStatus(ReturnStatus status, Pageable pageable);
}
