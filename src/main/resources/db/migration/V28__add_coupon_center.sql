-- ============================================================
-- 領券中心:公開可領取的優惠券,與會員已領取的優惠券
-- ============================================================

ALTER TABLE coupon ADD COLUMN claimable BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE member_coupon (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT   NOT NULL,
    coupon_id   BIGINT   NOT NULL,
    claimed_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_member_coupon UNIQUE (member_id, coupon_id),
    CONSTRAINT fk_member_coupon_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_member_coupon_coupon FOREIGN KEY (coupon_id) REFERENCES coupon (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
