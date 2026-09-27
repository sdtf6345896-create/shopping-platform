package com.example.shopping.stockalert.repository;

import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.stockalert.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {

    Optional<StockAlert> findByMemberIdAndProductSkuId(Long memberId, Long skuId);

    @Query("select a.productSku.id from StockAlert a "
            + "where a.member.id = :memberId and a.productSku.product.id = :productId")
    List<Long> findSubscribedSkuIds(@Param("memberId") Long memberId, @Param("productId") Long productId);

    /** 已補貨(有庫存且上架中)的訂閱 */
    @Query("select a from StockAlert a join fetch a.productSku s join fetch s.product p join fetch a.member m "
            + "where s.stock > 0 and p.status = :status")
    List<StockAlert> findRestocked(@Param("status") ProductStatus status);

    @Modifying
    @Query("delete from StockAlert a where a.productSku.id in :skuIds")
    int deleteBySkuIds(@Param("skuIds") Collection<Long> skuIds);

    @Modifying
    @Query("delete from StockAlert a where a.productSku.id in "
            + "(select s.id from ProductSku s where s.product.id = :productId)")
    int deleteByProductId(@Param("productId") Long productId);
}
