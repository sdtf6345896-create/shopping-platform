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
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /** 限時特價折扣(例如 20 代表打 8 折),未設定為 null */
    @Column(name = "sale_discount_percent")
    private Integer saleDiscountPercent;

    @Column(name = "sale_start_at")
    private LocalDateTime saleStartAt;

    @Column(name = "sale_end_at")
    private LocalDateTime saleEndAt;

    /** 排程上架時間;時間到由 ProductScheduleService 改為上架並清空 */
    @Column(name = "publish_at")
    private LocalDateTime publishAt;

    /** 排程下架時間;時間到由 ProductScheduleService 改為下架並清空 */
    @Column(name = "unpublish_at")
    private LocalDateTime unpublishAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSku> skus = new ArrayList<>();

    /** 主圖以外的商品圖庫 */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<ProductImage> images = new ArrayList<>();

    /** 規格表(材質、產地、尺寸…) */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<ProductSpec> specs = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 現在是否在限時特價期間內(含開始、不含結束時間) */
    public boolean isOnSale() {
        LocalDateTime now = LocalDateTime.now();
        return saleDiscountPercent != null && saleStartAt != null && saleEndAt != null
                && !now.isBefore(saleStartAt) && now.isBefore(saleEndAt);
    }

    /** 套用限時特價後的價格,四捨五入到整數元;不在特價期間則回傳原價 */
    public BigDecimal applySale(BigDecimal price) {
        if (!isOnSale()) {
            return price;
        }
        return price.multiply(BigDecimal.valueOf(100 - saleDiscountPercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .setScale(2, RoundingMode.UNNECESSARY);
    }

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

    /** 以新清單整個取代規格表(依傳入順序排序) */
    public void replaceSpecs(List<ProductSpec> incoming) {
        specs.clear();
        if (incoming == null) {
            return;
        }
        int order = 0;
        for (ProductSpec spec : incoming) {
            spec.setProduct(this);
            spec.setSortOrder(order++);
            specs.add(spec);
        }
    }

    /**
     * 依 SKU 編號合併規格:同編號的就地更新(保留 id,訂單與購物車的引用不受影響)、新編號新增、
     * 不在清單中的移除。
     *
     * @return 被移除的規格(呼叫端需確認它們可以刪除)
     */
    public List<ProductSku> mergeSkus(List<ProductSku> incoming) {
        Map<String, ProductSku> existingByCode = new HashMap<>();
        for (ProductSku sku : skus) {
            existingByCode.put(sku.getSkuCode(), sku);
        }
        List<ProductSku> merged = new ArrayList<>();
        for (ProductSku next : incoming) {
            ProductSku current = existingByCode.remove(next.getSkuCode());
            if (current == null) {
                next.setProduct(this);
                merged.add(next);
            } else {
                current.setSpecName(next.getSpecName());
                current.setPrice(next.getPrice());
                current.setStock(next.getStock());
                merged.add(current);
            }
        }
        List<ProductSku> removed = new ArrayList<>(existingByCode.values());
        skus.clear();
        skus.addAll(merged);
        return removed;
    }

    public void replaceSkus(List<ProductSku> newSkus) {
        skus.clear();
        for (ProductSku sku : newSkus) {
            sku.setProduct(this);
            skus.add(sku);
        }
    }
}
