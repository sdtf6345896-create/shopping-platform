-- ============================================================
-- 優惠券折扣機制
-- ============================================================

CREATE TABLE coupon (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    code                 VARCHAR(30)   NOT NULL,
    name                 VARCHAR(100)  NOT NULL,
    discount_type        VARCHAR(20)   NOT NULL,
    discount_value       DECIMAL(10,2) NOT NULL,
    max_discount_amount  DECIMAL(10,2),
    min_spend_amount     DECIMAL(10,2) NOT NULL DEFAULT 0,
    total_quantity       INT,
    used_quantity        INT           NOT NULL DEFAULT 0,
    start_at             DATETIME,
    end_at               DATETIME,
    status               VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
    created_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_coupon_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE orders
    ADD COLUMN coupon_id BIGINT NULL,
    ADD COLUMN coupon_code VARCHAR(30) NULL,
    ADD COLUMN subtotal_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD COLUMN discount_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    ADD CONSTRAINT fk_order_coupon FOREIGN KEY (coupon_id) REFERENCES coupon (id);

-- 既有訂單沒有折扣資料,小計等於原本的實付金額
UPDATE orders SET subtotal_amount = total_amount WHERE subtotal_amount = 0;

CREATE INDEX idx_order_coupon ON orders (coupon_id);

-- ---------- Demo 優惠券 ----------
INSERT INTO coupon (code, name, discount_type, discount_value, max_discount_amount, min_spend_amount, total_quantity, used_quantity, start_at, end_at, status)
VALUES
  ('WELCOME100', '新會員折抵 100 元', 'FIXED_AMOUNT', 100.00, NULL, 500.00, NULL, 0, NULL, NULL, 'ACTIVE'),
  ('SAVE10', '全館 9 折(上限折 300 元)', 'PERCENTAGE', 10.00, 300.00, 1000.00, 200, 0, NULL, NULL, 'ACTIVE'),
  ('SUMMER50', '夏季特惠折抵 50 元', 'FIXED_AMOUNT', 50.00, NULL, 0.00, 100, 0, NULL, NULL, 'ACTIVE');
