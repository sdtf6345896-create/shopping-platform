package com.example.shopping.order.repository;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("select case when count(oi) > 0 then true else false end " +
            "from OrderItem oi " +
            "where oi.order.member.id = :memberId " +
            "and oi.productSku.product.id = :productId " +
            "and oi.order.status in :statuses")
    boolean existsPurchase(@Param("memberId") Long memberId,
                            @Param("productId") Long productId,
                            @Param("statuses") Collection<OrderStatus> statuses);
}
