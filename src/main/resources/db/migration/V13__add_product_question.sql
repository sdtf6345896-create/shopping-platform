-- ============================================================
-- 商品問答:會員提問,管理員回覆
-- ============================================================

CREATE TABLE product_question (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id   BIGINT        NOT NULL,
    member_id    BIGINT        NOT NULL,
    content      VARCHAR(500)  NOT NULL,
    answer       VARCHAR(1000) NULL,
    answered_at  DATETIME      NULL,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_question_product FOREIGN KEY (product_id) REFERENCES product (id),
    CONSTRAINT fk_product_question_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_product_question_product ON product_question (product_id, created_at);
CREATE INDEX idx_product_question_answered ON product_question (answered_at);
