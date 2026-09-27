package com.example.shopping.product.repository;

import com.example.shopping.common.enums.ProductStatus;
import com.example.shopping.product.entity.Product;
import com.example.shopping.product.entity.ProductSku;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> hasStatus(ProductStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Product> hasCategoryId(Long categoryId) {
        return (root, query, cb) -> categoryId == null ? null : cb.equal(root.get("category").get("id"), categoryId);
    }

    public static Specification<Product> priceGreaterOrEqual(BigDecimal minPrice) {
        return (root, query, cb) -> minPrice == null ? null : cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }

    public static Specification<Product> priceLessOrEqual(BigDecimal maxPrice) {
        return (root, query, cb) -> maxPrice == null ? null : cb.lessThanOrEqualTo(root.get("price"), maxPrice);
    }

    /** 至少一個規格有庫存 */
    public static Specification<Product> inStock(boolean inStockOnly) {
        return (root, query, cb) -> {
            if (!inStockOnly) {
                return null;
            }
            Subquery<Long> sku = query.subquery(Long.class);
            Root<ProductSku> skuRoot = sku.from(ProductSku.class);
            sku.select(skuRoot.get("id"))
                    .where(cb.equal(skuRoot.get("product"), root), cb.greaterThan(skuRoot.get("stock"), 0));
            return cb.exists(sku);
        };
    }

    public static Specification<Product> nameContains(String keyword) {
        return (root, query, cb) -> (keyword == null || keyword.isBlank())
                ? null
                : cb.like(root.get("name"), "%" + keyword + "%");
    }
}
