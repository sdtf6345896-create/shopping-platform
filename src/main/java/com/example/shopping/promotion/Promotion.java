package com.example.shopping.promotion;

import com.example.shopping.category.entity.Category;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 滿件折扣:同一活動範圍內的商品合計滿 minQuantity 件,這些商品打 discountPercent 折扣 */
@Entity
@Table(name = "promotion")
@Getter
@Setter
@NoArgsConstructor
public class Promotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    /** 適用分類(含其子分類);null 表示全站商品 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "min_quantity", nullable = false)
    private int minQuantity;

    /** 折扣百分比,例如 10 代表打 9 折 */
    @Column(name = "discount_percent", nullable = false)
    private int discountPercent;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(nullable = false)
    private boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public boolean isRunningAt(LocalDateTime now) {
        return active
                && (startAt == null || !now.isBefore(startAt))
                && (endAt == null || now.isBefore(endAt));
    }
}
