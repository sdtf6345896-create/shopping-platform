package com.example.shopping.product.entity;

import com.example.shopping.category.entity.Category;
import com.example.shopping.common.enums.ProductStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "main_image", length = 500)
    private String mainImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status = ProductStatus.OFF_SHELF;

    /** 銷量。只透過 ProductRepository 的相對增減更新,避免併發付款遺失計數或後台編輯商品時被舊值覆蓋 */
    @Column(name = "sales_count", nullable = false, updatable = false)
    private int salesCount;

    /** 平均星等(四捨五入到小數一位),評論新增 / 修改 / 刪除時更新 */
    @Column(name = "rating_average", nullable = false, precision = 2, scale = 1)
    private BigDecimal ratingAverage = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSku> skus = new ArrayList<>();

    /** 主圖以外的商品圖庫 */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<ProductImage> images = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 以網址清單整批取代圖庫,清單順序即顯示順序 */
    public void replaceImages(List<String> urls) {
        images.clear();
        if (urls == null) {
            return;
        }
        int order = 0;
        for (String url : urls) {
            ProductImage image = new ProductImage();
            image.setProduct(this);
            image.setUrl(url.trim());
            image.setSortOrder(order++);
            images.add(image);
        }
    }

    public void replaceSkus(List<ProductSku> newSkus) {
        skus.clear();
        for (ProductSku sku : newSkus) {
            sku.setProduct(this);
            skus.add(sku);
        }
    }
}
