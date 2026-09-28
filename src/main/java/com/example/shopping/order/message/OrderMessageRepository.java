package com.example.shopping.order.message;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderMessageRepository extends JpaRepository<OrderMessage, Long> {

    List<OrderMessage> findByOrderIdOrderByIdAsc(Long orderId);

    long countByOrderId(Long orderId);

    /** 各訂單的最後一則留言中,由會員發送(= 等賣家回覆)的那些,最早的排前面 */
    @Query(value = "select m from OrderMessage m join fetch m.order o join fetch o.member"
            + " where m.sender = com.example.shopping.order.message.MessageSender.MEMBER"
            + " and m.id in (select max(m2.id) from OrderMessage m2 group by m2.order.id) order by m.id asc",
            countQuery = "select count(m) from OrderMessage m"
                    + " where m.sender = com.example.shopping.order.message.MessageSender.MEMBER"
                    + " and m.id in (select max(m2.id) from OrderMessage m2 group by m2.order.id)")
    Page<OrderMessage> findAwaitingReply(Pageable pageable);

    @Query("select count(m) from OrderMessage m"
            + " where m.sender = com.example.shopping.order.message.MessageSender.MEMBER"
            + " and m.id in (select max(m2.id) from OrderMessage m2 group by m2.order.id)")
    long countAwaitingReply();
}
