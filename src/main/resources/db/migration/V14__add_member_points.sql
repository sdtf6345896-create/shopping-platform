-- ============================================================
-- 購物金:訂單完成回饋、結帳折抵、取消退還
-- ============================================================

ALTER TABLE member ADD COLUMN points INT NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN points_used INT NOT NULL DEFAULT 0;

-- 購物金異動明細(每一筆增減都留紀錄,balance_after 為異動後餘額)
CREATE TABLE point_transaction (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id      BIGINT       NOT NULL,
    order_id       BIGINT       NULL,
    amount         INT          NOT NULL,
    type           VARCHAR(20)  NOT NULL,
    description    VARCHAR(255) NOT NULL,
    balance_after  INT          NOT NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_point_transaction_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_point_transaction_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_point_transaction_member ON point_transaction (member_id, created_at);
