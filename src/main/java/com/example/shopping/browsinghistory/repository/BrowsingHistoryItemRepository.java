package com.example.shopping.browsinghistory.repository;

import com.example.shopping.browsinghistory.entity.BrowsingHistoryItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BrowsingHistoryItemRepository extends JpaRepository<BrowsingHistoryItem, Long> {

    @Query(value = "select h from BrowsingHistoryItem h join fetch h.product p where h.member.id = :memberId",
            countQuery = "select count(h) from BrowsingHistoryItem h where h.member.id = :memberId")
    Page<BrowsingHistoryItem> findAllByMemberIdWithDetails(@Param("memberId") Long memberId, Pageable pageable);

    Optional<BrowsingHistoryItem> findByMemberIdAndProductId(Long memberId, Long productId);

    void deleteByMemberId(Long memberId);

    @Modifying
    @Query("delete from BrowsingHistoryItem x where x.product.id = :productId")
    int deleteAllByProductId(@Param("productId") Long productId);
}
