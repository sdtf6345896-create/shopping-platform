-- ============================================================
-- 評論照片:每則評論最多 5 張,依 sort_order 排序
-- ============================================================

CREATE TABLE review_image (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id   BIGINT       NOT NULL,
    url         VARCHAR(255) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_review_image_review FOREIGN KEY (review_id) REFERENCES product_review (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_review_image_review ON review_image (review_id, sort_order);
