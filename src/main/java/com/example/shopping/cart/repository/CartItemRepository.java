package com.example.shopping.cart.repository;

import com.example.shopping.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    @Query("select c from CartItem c join fetch c.productSku s join fetch s.product p " +
            "where c.member.id = :memberId order by c.id desc")
    List<CartItem> findAllByMemberIdWithDetails(@Param("memberId") Long memberId);

    Optional<CartItem> findByIdAndMemberId(Long id, Long memberId);

    Optional<CartItem> findByMemberIdAndProductSkuId(Long memberId, Long productSkuId);

    void deleteByMemberId(Long memberId);
}
