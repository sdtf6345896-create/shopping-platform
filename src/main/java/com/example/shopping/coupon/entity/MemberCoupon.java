package com.example.shopping.coupon.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 會員從領券中心領取的優惠券 */
@Entity
@Table(name = "member_coupon")
@Getter
@Setter
@NoArgsConstructor
public class MemberCoupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @CreationTimestamp
    @Column(name = "claimed_at", updatable = false)
    private LocalDateTime claimedAt;
}
