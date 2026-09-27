package com.example.shopping.coupon.repository;

import com.example.shopping.coupon.entity.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long>, JpaSpecificationExecutor<Coupon> {

    Optional<Coupon> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    /** 佔用一個名額;已達發行數量則不更新,回傳 0 */
    @Modifying(flushAutomatically = true)
    @Query("update Coupon c set c.usedQuantity = c.usedQuantity + 1 "
            + "where c.id = :id and (c.totalQuantity is null or c.usedQuantity < c.totalQuantity)")
    int claimOne(@Param("id") Long id);

    /** 歸還一個名額(不會小於 0) */
    @Modifying(flushAutomatically = true)
    @Query("update Coupon c set c.usedQuantity = c.usedQuantity - 1 where c.id = :id and c.usedQuantity > 0")
    int releaseOne(@Param("id") Long id);
}
