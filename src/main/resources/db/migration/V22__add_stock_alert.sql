-- ============================================================
-- 貨到通知:缺貨規格的訂閱,補貨後通知會員並刪除訂閱
-- ============================================================

CREATE TABLE stock_alert (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id       BIGINT   NOT NULL,
    product_sku_id  BIGINT   NOT NULL,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_stock_alert_member_sku UNIQUE (member_id, product_sku_id),
    CONSTRAINT fk_stock_alert_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_stock_alert_sku FOREIGN KEY (product_sku_id) REFERENCES product_sku (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_stock_alert_sku ON stock_alert (product_sku_id);
