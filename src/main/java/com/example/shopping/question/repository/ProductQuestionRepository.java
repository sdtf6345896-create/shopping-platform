package com.example.shopping.question.repository;

import com.example.shopping.question.entity.ProductQuestion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ProductQuestionRepository extends JpaRepository<ProductQuestion, Long> {

    Page<ProductQuestion> findByProductId(Long productId, Pageable pageable);

    Page<ProductQuestion> findByAnsweredAtIsNull(Pageable pageable);

    long countByAnsweredAtIsNull();

    Page<ProductQuestion> findByAnsweredAtIsNotNull(Pageable pageable);

    /** 防洗版:會員在某時間之後對同一商品提問的次數 */
    long countByMemberIdAndProductIdAndCreatedAtAfter(Long memberId, Long productId, LocalDateTime since);

    @Modifying
    @Query("delete from ProductQuestion x where x.product.id = :productId")
    int deleteAllByProductId(@Param("productId") Long productId);
}
