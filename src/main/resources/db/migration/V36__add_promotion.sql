-- ============================================================
-- 滿件折扣活動(免輸入代碼,結帳自動套用最划算的一個),訂單記錄活動名稱與折扣快照
-- ============================================================

CREATE TABLE promotion (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(50) NOT NULL,
    category_id       BIGINT      NULL,
    min_quantity      INT         NOT NULL,
    discount_percent  INT         NOT NULL,
    start_at          DATETIME    NULL,
    end_at            DATETIME    NULL,
    active            BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_promotion_category FOREIGN KEY (category_id) REFERENCES category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE orders ADD COLUMN promotion_discount DECIMAL(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN promotion_name VARCHAR(50) NULL;
