-- ============================================================
-- 庫存異動紀錄:每次庫存變動(下單、取消、退貨、手動調整、匯入、編輯商品)一筆
-- ============================================================

CREATE TABLE stock_movement (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_id        BIGINT      NOT NULL,
    change_qty    INT         NOT NULL,
    stock_after   INT         NOT NULL,
    reason        VARCHAR(20) NOT NULL,
    reference     VARCHAR(100) NULL,
    created_at    DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_movement_sku FOREIGN KEY (sku_id) REFERENCES product_sku (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_stock_movement_sku ON stock_movement (sku_id, id);
