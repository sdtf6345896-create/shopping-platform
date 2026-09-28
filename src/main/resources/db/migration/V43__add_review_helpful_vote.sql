-- ============================================================
-- 評價「有幫助」投票:每位會員對每則評價最多一票;helpful_count 為彙總(排序用)
-- ============================================================

ALTER TABLE product_review ADD COLUMN helpful_count INT NOT NULL DEFAULT 0;

CREATE TABLE review_helpful_vote (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id   BIGINT   NOT NULL,
    member_id   BIGINT   NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_review_helpful_vote UNIQUE (review_id, member_id),
    CONSTRAINT fk_review_helpful_vote_review FOREIGN KEY (review_id) REFERENCES product_review (id) ON DELETE CASCADE,
    CONSTRAINT fk_review_helpful_vote_member FOREIGN KEY (member_id) REFERENCES member (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_review_helpful_vote_member ON review_helpful_vote (member_id);
