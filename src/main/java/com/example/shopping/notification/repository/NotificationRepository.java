package com.example.shopping.notification.repository;

import com.example.shopping.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findByMemberId(Long memberId, Pageable pageable);

    Optional<Notification> findByIdAndMemberId(Long id, Long memberId);

    long countByMemberIdAndReadAtIsNull(Long memberId);

    @Modifying
    @Query("update Notification n set n.readAt = :now where n.memberId = :memberId and n.readAt is null")
    int markAllRead(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("delete from Notification n where n.memberId = :memberId")
    int deleteAllByMemberId(@Param("memberId") Long memberId);
}
