package com.example.shopping.product.repository;

import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsByCategoryId(Long categoryId);

    @Modifying
    @Query("update Product p set p.status = :status where p.id in :ids")
    int updateStatusByIds(@Param("ids") Collection<Long> ids, @Param("status") ProductStatus status);

    /** 搜尋建議:名稱包含關鍵字的商品,熱銷優先(Containing 會跳脫 % 與 _,不會被當成萬用字元) */
    List<Product> findByStatusAndNameContainingIgnoreCaseOrderBySalesCountDescIdDesc(
            ProductStatus status, String keyword, Pageable pageable);

    @Query("select p from Product p where p.status = :status and p.saleDiscountPercent is not null "
            + "and p.saleStartAt <= :now and p.saleEndAt > :now order by p.saleEndAt asc, p.id asc")
    List<Product> findOnSale(@Param("status") ProductStatus status, @Param("now") LocalDateTime now,
                             Pageable pageable);

    /** 以相對值增減銷量(delta 可為負),不會小於 0 */
    @Modifying(flushAutomatically = true)
    @Query("update Product p set p.salesCount = case when p.salesCount + :delta < 0 then 0 "
            + "else p.salesCount + :delta end where p.id = :productId")
    int addSalesCount(@Param("productId") Long productId, @Param("delta") int delta);

    /** 同分類的其他商品,依銷量排序(相關商品推薦用) */
    List<Product> findByCategoryIdAndStatusAndIdNotOrderBySalesCountDescIdDesc(
            Long categoryId, ProductStatus status, Long excludeId, Pageable pageable);

    /** 指定分類群中、排除指定商品後的熱銷商品(個人化推薦用) */
    List<Product> findByCategoryIdInAndStatusAndIdNotInOrderBySalesCountDescIdDesc(
            Collection<Long> categoryIds, ProductStatus status, Collection<Long> excludeIds, Pageable pageable);

    /** 排除指定商品後的熱銷商品(同分類不夠時補位用) */
    List<Product> findByStatusAndIdNotInOrderBySalesCountDescIdDesc(
            ProductStatus status, Collection<Long> excludeIds, Pageable pageable);
}
