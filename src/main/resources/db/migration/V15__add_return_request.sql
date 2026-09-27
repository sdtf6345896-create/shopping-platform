-- ============================================================
-- 退貨申請:訂單完成後 7 天內可申請,管理員核准後退款(訂單狀態改為 REFUNDED)
-- ============================================================

-- 訂單完成時回饋的購物金,退貨時據此收回
ALTER TABLE orders ADD COLUMN points_earned INT NOT NULL DEFAULT 0;

CREATE TABLE return_request (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id      BIGINT       NOT NULL,
    reason        VARCHAR(500) NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    admin_note    VARCHAR(255) NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at  DATETIME     NULL,
    CONSTRAINT uk_return_request_order UNIQUE (order_id),
    CONSTRAINT fk_return_request_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_return_request_status ON return_request (status, created_at);
