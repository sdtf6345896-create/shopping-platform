package com.example.shopping.review.helpful;

import com.example.shopping.review.entity.ProductReview;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

/** 會員對某則評價按「有幫助」;評價被刪除時資料庫會一併刪除投票(ON DELETE CASCADE) */
@Entity
@Table(name = "review_helpful_vote",
        uniqueConstraints = @UniqueConstraint(name = "uk_review_helpful_vote", columnNames = {"review_id", "member_id"}))
@Getter
@Setter
@NoArgsConstructor
public class ReviewHelpfulVote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private ProductReview review;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
