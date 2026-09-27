-- ============================================================
-- 限時特價:商品層級的折扣百分比與活動期間,期間內所有規格依比例打折
-- ============================================================

ALTER TABLE product ADD COLUMN sale_discount_percent INT      NULL;
ALTER TABLE product ADD COLUMN sale_start_at         DATETIME NULL;
ALTER TABLE product ADD COLUMN sale_end_at           DATETIME NULL;

CREATE INDEX idx_product_sale_end ON product (sale_end_at);
