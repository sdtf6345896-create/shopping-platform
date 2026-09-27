-- ============================================================
-- 商品瀏覽紀錄
-- ============================================================

CREATE TABLE browsing_history (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT   NOT NULL,
    product_id  BIGINT   NOT NULL,
    viewed_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_browsing_history_member_product UNIQUE (member_id, product_id),
    CONSTRAINT fk_browsing_history_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_browsing_history_product FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_browsing_history_member ON browsing_history (member_id);
