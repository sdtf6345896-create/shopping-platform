package com.example.shopping.product.repository;

import com.example.shopping.product.entity.ProductSku;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> {

    Optional<ProductSku> findByIdAndProductId(Long id, Long productId);
}
