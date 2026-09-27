-- ============================================================
-- 運費:未達免運門檻時加收固定運費,已包含在 total_amount
-- ============================================================

ALTER TABLE orders ADD COLUMN shipping_fee DECIMAL(10,2) NOT NULL DEFAULT 0;
