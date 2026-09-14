package com.example.shopping.report.repository;

import com.example.shopping.common.enums.OrderStatus;
import com.example.shopping.order.entity.Orders;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ReportRepository extends Repository<Orders, Long> {

    @Query("select o.status as status, count(o) as count, sum(o.totalAmount) as amount " +
            "from Orders o where o.createdAt between :start and :end group by o.status")
    List<OrderStatusStatProjection> findStatusStats(@Param("start") LocalDateTime start,
                                                     @Param("end") LocalDateTime end);

    @Query("select p.id as productId, p.name as productName, p.mainImage as mainImage, " +
            "sum(oi.quantity) as soldQuantity, sum(oi.subtotal) as revenue " +
            "from OrderItem oi join oi.productSku s join s.product p join oi.order o " +
            "where o.status in :statuses and o.createdAt between :start and :end " +
            "group by p.id, p.name, p.mainImage order by sum(oi.quantity) desc")
    List<TopProductProjection> findTopProducts(@Param("statuses") List<OrderStatus> statuses,
                                                @Param("start") LocalDateTime start,
                                                @Param("end") LocalDateTime end,
                                                Pageable pageable);

    @Query(value = "select DATE(o.created_at) as day, count(*) as orderCount, sum(o.total_amount) as revenue " +
            "from orders o where o.status in (:statuses) and o.created_at between :start and :end " +
            "group by DATE(o.created_at) order by day", nativeQuery = true)
    List<DailyStatProjection> findDailyStats(@Param("statuses") List<String> statuses,
                                              @Param("start") LocalDateTime start,
                                              @Param("end") LocalDateTime end);
}
