package com.example.shopping.product.repository;

import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.entity.ProductSku;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {

    Optional<ProductSku> findByIdAndProductId(Long id, Long productId);

    Optional<ProductSku> findBySkuCode(String skuCode);

    /**
     * 扣庫存:檢查與扣除在同一條 UPDATE 完成(庫存不足則不更新),多人同時結帳也不會超賣。
     *
     * @return 受影響筆數,0 表示庫存不足
     */
    @Modifying(flushAutomatically = true)
    @Query("update ProductSku s set s.stock = s.stock - :quantity where s.id = :skuId and s.stock >= :quantity")
    int decrementStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);

    /** 加回庫存(取消訂單、退貨),以相對值更新避免覆蓋其他交易的異動 */
    @Modifying(flushAutomatically = true)
    @Query("update ProductSku s set s.stock = s.stock + :quantity where s.id = :skuId")
    int incrementStock(@Param("skuId") Long skuId, @Param("quantity") int quantity);

    long countByProductStatusAndStockLessThanEqual(ProductStatus status, int threshold);

    /** 指定狀態商品中,庫存小於等於門檻的規格,庫存最少的排前面 */
    @Query("""
            SELECT s FROM ProductSku s JOIN FETCH s.product p
            WHERE p.status = :status AND s.stock <= :threshold
            ORDER BY s.stock ASC, s.id ASC
            """)
    List<ProductSku> findLowStock(@Param("status") ProductStatus status,
                                  @Param("threshold") int threshold,
                                  Pageable pageable);
}
