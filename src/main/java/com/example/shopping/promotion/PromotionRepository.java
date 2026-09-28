package com.example.shopping.promotion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface PromotionRepository extends JpaRepository<Promotion, Long> {

    @Query("select p from Promotion p left join fetch p.category where p.active = true"
            + " and (p.startAt is null or p.startAt <= :now) and (p.endAt is null or p.endAt > :now)"
            + " order by p.discountPercent desc, p.minQuantity asc, p.id asc")
    List<Promotion> findRunning(@Param("now") LocalDateTime now);

    @Query("select p from Promotion p left join fetch p.category order by p.createdAt desc, p.id desc")
    List<Promotion> findAllForAdmin();

    boolean existsByCategoryId(Long categoryId);
}
