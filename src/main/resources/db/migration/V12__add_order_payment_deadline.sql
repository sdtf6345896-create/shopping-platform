-- ============================================================
-- 訂單付款期限:逾期未付款由排程自動取消(貨到付款不設期限)
-- ============================================================

ALTER TABLE orders ADD COLUMN payment_deadline DATETIME NULL;

-- 既有待付款訂單給 30 分鐘期限(從建立時間起算)
UPDATE orders
SET payment_deadline = DATE_ADD(created_at, INTERVAL 30 MINUTE)
WHERE status = 'PENDING_PAYMENT' AND payment_method <> 'COD';

CREATE INDEX idx_orders_status_payment_deadline ON orders (status, payment_deadline);
