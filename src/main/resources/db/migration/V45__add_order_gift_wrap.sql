-- ============================================================
-- 禮品包裝與賀卡:結帳可加購禮品包裝(固定費用)並附上賀卡留言
-- ============================================================

ALTER TABLE orders ADD COLUMN gift_wrap BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE orders ADD COLUMN gift_wrap_fee DECIMAL(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN gift_message VARCHAR(100) NULL;
