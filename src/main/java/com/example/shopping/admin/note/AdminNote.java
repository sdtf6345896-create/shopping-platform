package com.example.shopping.admin.note;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/** 管理員對訂單 / 會員的內部備註,只在後台顯示 */
@Entity
@Table(name = "admin_note")
@Getter
@Setter
@NoArgsConstructor
public class AdminNote {

    public enum TargetType {
        ORDER,
        MEMBER
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 10)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(name = "admin_id", nullable = false)
    private Long adminId;

    /** 撰寫者帳號快照 */
    @Column(name = "admin_username", nullable = false, length = 50)
    private String adminUsername;

    @Column(nullable = false, length = 500)
    private String content;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
