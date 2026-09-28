package com.example.shopping.cart.repository;

import com.example.shopping.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("select c from CartItem c join fetch c.productSku s join fetch s.product p " +
            "where c.member.id = :memberId order by c.id desc")
    List<CartItem> findAllByMemberIdWithDetails(@Param("memberId") Long memberId);

    /** 購物車提醒候選:啟用會員、購物車最後異動早於 cutoff,且這次異動之後還沒提醒過 */
    interface IdleCart {
        Long getMemberId();

        LocalDateTime getLastActivity();

        long getItemCount();
    }

    @Query("select m.id as memberId, max(c.updatedAt) as lastActivity, count(c) as itemCount"
            + " from CartItem c join c.member m"
            + " where m.status = com.example.shopping.common.enums.AccountStatus.ACTIVE and m.marketingOptIn = true"
            + " group by m.id, m.cartRemindedAt"
            + " having max(c.updatedAt) < :cutoff"
            + " and (m.cartRemindedAt is null or m.cartRemindedAt < max(c.updatedAt))")
    List<IdleCart> findIdleCarts(@Param("cutoff") LocalDateTime cutoff);

    Optional<CartItem> findByIdAndMemberId(Long id, Long memberId);

    Optional<CartItem> findByMemberIdAndProductSkuId(Long memberId, Long productSkuId);

    void deleteByMemberId(Long memberId);

    @Modifying
    @Query("delete from CartItem c where c.productSku.id in :skuIds")
    int deleteBySkuIds(@Param("skuIds") Collection<Long> skuIds);

    @Modifying
    @Query("delete from CartItem c where c.productSku.id in "
            + "(select s.id from ProductSku s where s.product.id = :productId)")
    int deleteByProductId(@Param("productId") Long productId);
}
