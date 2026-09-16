-- ============================================================
-- 商品評論 / 評分
-- ============================================================

CREATE TABLE product_review (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT       NOT NULL,
    member_id   BIGINT       NOT NULL,
    rating      INT          NOT NULL,
    content     VARCHAR(500),
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_review_product_member UNIQUE (product_id, member_id),
    CONSTRAINT chk_review_rating CHECK (rating BETWEEN 1 AND 5),
    CONSTRAINT fk_review_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_review_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_review_product ON product_review (product_id);
