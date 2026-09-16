-- ============================================================
-- 願望清單 / 收藏
-- ============================================================

CREATE TABLE wishlist_item (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT   NOT NULL,
    product_id  BIGINT   NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_wishlist_member_product UNIQUE (member_id, product_id),
    CONSTRAINT fk_wishlist_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_wishlist_product FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_wishlist_member ON wishlist_item (member_id);
