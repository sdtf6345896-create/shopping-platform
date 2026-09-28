package com.example.shopping.product.stock;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findBySkuIdOrderByIdDesc(Long skuId, Pageable pageable);

    @Modifying
    @Query("delete from StockMovement m where m.skuId in :skuIds")
    int deleteBySkuIds(@Param("skuIds") Collection<Long> skuIds);

    @Modifying
    @Query("delete from StockMovement m where m.skuId in"
            + " (select s.id from ProductSku s where s.product.id = :productId)")
    int deleteByProductId(@Param("productId") Long productId);
}
