-- ============================================================
-- 配送方式:宅配 / 超商取貨(門市資訊寫在 receiver_address)
-- ============================================================

ALTER TABLE orders ADD COLUMN shipping_method VARCHAR(20) NOT NULL DEFAULT 'HOME_DELIVERY';
