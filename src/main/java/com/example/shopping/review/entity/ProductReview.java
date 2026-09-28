package com.example.shopping.review.entity;

import com.example.shopping.member.entity.Member;
import com.example.shopping.product.entity.Product;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_review")
@Getter
@Setter
@NoArgsConstructor
public class ProductReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private int rating;

    @Column(length = 500)
    private String content;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<ReviewImage> images = new ArrayList<>();

    /** 賣家公開回覆,未回覆為 null */
    @Column(name = "seller_reply", length = 500)
    private String sellerReply;

    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    /** 被管理員隱藏的評價不出現在前台,也不列入評分統計 */
    @Column(nullable = false)
    private boolean hidden;

    /** 「有幫助」票數。唯讀對應:由 ProductReviewRepository 的相對值 UPDATE 增減,評論者編輯評價時不會蓋掉 */
    @Column(name = "helpful_count", nullable = false, insertable = false, updatable = false)
    @ColumnDefault("0")
    private int helpfulCount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    /** 以網址清單整批取代評論照片,順序即顯示順序 */
    public void replaceImages(List<String> urls) {
        images.clear();
        if (urls == null) {
            return;
        }
        int order = 0;
        for (String url : urls) {
            ReviewImage image = new ReviewImage();
            image.setReview(this);
            image.setUrl(url);
            image.setSortOrder(order++);
            images.add(image);
        }
    }
}
