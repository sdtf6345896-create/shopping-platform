package com.example.shopping.wishlist.repository;

import com.example.shopping.wishlist.entity.WishlistItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {

    @Query(value = "select w from WishlistItem w join fetch w.product p where w.member.id = :memberId",
            countQuery = "select count(w) from WishlistItem w where w.member.id = :memberId")
    Page<WishlistItem> findAllByMemberIdWithDetails(@Param("memberId") Long memberId, Pageable pageable);

    Optional<WishlistItem> findByMemberIdAndProductId(Long memberId, Long productId);

    boolean existsByMemberIdAndProductId(Long memberId, Long productId);
}
