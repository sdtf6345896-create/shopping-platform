package com.example.shopping.member.cvs;

import com.example.shopping.order.shipping.CvsBrand;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 會員常用的超商取貨門市(含取件人) */
@Entity
@Table(name = "member_cvs_store")
@Getter
@Setter
@NoArgsConstructor
public class MemberCvsStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CvsBrand brand;

    @Column(name = "store_name", nullable = false, length = 30)
    private String storeName;

    @Column(name = "store_code", length = 8)
    private String storeCode;

    @Column(name = "recipient_name", nullable = false, length = 50)
    private String recipientName;

    @Column(name = "recipient_phone", nullable = false, length = 10)
    private String recipientPhone;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
