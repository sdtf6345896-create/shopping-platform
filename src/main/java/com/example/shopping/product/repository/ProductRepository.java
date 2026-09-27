package com.example.shopping.product.repository;

import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.entity.Product;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    boolean existsByCategoryId(Long categoryId);

    /** 同分類的其他商品,依銷量排序(相關商品推薦用) */
    List<Product> findByCategoryIdAndStatusAndIdNotOrderBySalesCountDescIdDesc(
            Long categoryId, ProductStatus status, Long excludeId, Pageable pageable);

    /** 排除指定商品後的熱銷商品(同分類不夠時補位用) */
    List<Product> findByStatusAndIdNotInOrderBySalesCountDescIdDesc(
            ProductStatus status, Collection<Long> excludeIds, Pageable pageable);
}
