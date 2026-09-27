-- ============================================================
-- 訂單狀態歷程 + 出貨物流資訊
-- ============================================================

ALTER TABLE orders ADD COLUMN shipping_carrier VARCHAR(30) NULL;
ALTER TABLE orders ADD COLUMN tracking_number  VARCHAR(50) NULL;
ALTER TABLE orders ADD COLUMN shipped_at       DATETIME    NULL;

CREATE TABLE order_status_log (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id     BIGINT       NOT NULL,
    from_status  VARCHAR(20)  NULL,
    to_status    VARCHAR(20)  NOT NULL,
    actor        VARCHAR(20)  NOT NULL,
    note         VARCHAR(255) NULL,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_status_log_order FOREIGN KEY (order_id) REFERENCES orders (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_order_status_log_order ON order_status_log (order_id);

-- 既有訂單補一筆目前狀態的紀錄,讓歷程頁不會是空的
INSERT INTO order_status_log (order_id, from_status, to_status, actor, note, created_at)
SELECT id, NULL, status, 'SYSTEM', '歷史訂單匯入', created_at FROM orders;
