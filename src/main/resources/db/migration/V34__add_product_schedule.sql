-- ============================================================
-- 商品排程上架 / 下架:時間到由排程自動切換狀態,套用後清空
-- ============================================================

ALTER TABLE product ADD COLUMN publish_at DATETIME NULL;
ALTER TABLE product ADD COLUMN unpublish_at DATETIME NULL;

CREATE INDEX idx_product_publish_at ON product (publish_at);
CREATE INDEX idx_product_unpublish_at ON product (unpublish_at);
