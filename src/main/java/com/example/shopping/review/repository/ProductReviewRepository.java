package com.example.shopping.review.repository;

import com.example.shopping.review.entity.ProductReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductReviewRepository extends JpaRepository<ProductReview, Long> {

    Page<ProductReview> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);

    Optional<ProductReview> findByProductIdAndMemberId(Long productId, Long memberId);

    @Query("select avg(r.rating) as averageRating, count(r) as reviewCount " +
            "from ProductReview r where r.product.id = :productId")
    ReviewSummaryProjection summarizeByProductId(@Param("productId") Long productId);
}
