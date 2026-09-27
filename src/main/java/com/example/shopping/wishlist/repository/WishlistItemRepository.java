package com.example.shopping.wishlist.repository;

import java.util.List;
import java.time.LocalDateTime;
import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.wishlist.entity.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    @Query(value = "select w from WishlistItem w join fetch w.product p where w.member.id = :memberId",
            countQuery = "select count(w) from WishlistItem w where w.member.id = :memberId")
    Page<WishlistItem> findAllByMemberIdWithDetails(@Param("memberId") Long memberId, Pageable pageable);

    Optional<WishlistItem> findByMemberIdAndProductId(Long memberId, Long productId);

    boolean existsByMemberIdAndProductId(Long memberId, Long productId);

    @Modifying
    @Query("delete from WishlistItem x where x.product.id = :productId")
    int deleteAllByProductId(@Param("productId") Long productId);

    @Modifying
    @Query("delete from WishlistItem x where x.member.id = :memberId")
    int deleteAllByMemberId(@Param("memberId") Long memberId);

    /** 收藏的商品正在特價、且這一檔特價還沒通知過的收藏 */
    @Query("select w from WishlistItem w join fetch w.product p "
            + "where p.status = :status and p.saleDiscountPercent is not null "
            + "and p.saleStartAt <= :now and p.saleEndAt > :now "
            + "and (w.saleNotifiedStart is null or w.saleNotifiedStart <> p.saleStartAt)")
    List<WishlistItem> findUnnotifiedOnSale(@Param("status") ProductStatus status, @Param("now") LocalDateTime now);
}
