package com.example.shopping.member.entity;

import com.example.shopping.common.enums.AccountStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "member")
@Getter
@Setter
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified = false;

    /**
     * 購物金餘額。唯讀對應:增減一律透過 MemberRepository 的條件式 UPDATE,
     * 避免其他交易拿舊的 Member 存檔時把餘額蓋回去(lost update)。
     */
    @Column(nullable = false, insertable = false, updatable = false)
    @ColumnDefault("0")
    private int points;

    /** 生日;設定後會員不可自行修改,避免反覆改生日領取生日禮 */
    private LocalDate birthday;

    /** 最近一次發放生日禮的年份。唯讀對應:由 MemberRepository 的條件式 UPDATE 認領,確保一年只發一次 */
    @Column(name = "birthday_reward_year", insertable = false, updatable = false)
    private Integer birthdayRewardYear;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
