-- ============================================================
-- 評價管理:賣家公開回覆評價、隱藏不當評價(不列入前台與評分統計)
-- ============================================================

ALTER TABLE product_review ADD COLUMN seller_reply VARCHAR(500) NULL;
ALTER TABLE product_review ADD COLUMN replied_at DATETIME NULL;
ALTER TABLE product_review ADD COLUMN hidden BOOLEAN NOT NULL DEFAULT FALSE;
