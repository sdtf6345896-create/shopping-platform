-- ============================================================
-- 電子發票資訊:開立方式與對應欄位(載具 / 統編與抬頭 / 愛心碼)
-- ============================================================

ALTER TABLE orders ADD COLUMN invoice_type          VARCHAR(20) NOT NULL DEFAULT 'MEMBER_CARRIER';
ALTER TABLE orders ADD COLUMN invoice_carrier       VARCHAR(8)  NULL;
ALTER TABLE orders ADD COLUMN invoice_tax_id        VARCHAR(8)  NULL;
ALTER TABLE orders ADD COLUMN invoice_title         VARCHAR(60) NULL;
ALTER TABLE orders ADD COLUMN invoice_donation_code VARCHAR(7)  NULL;
