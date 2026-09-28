-- ============================================================
-- 登入裝置管理:refresh token 記錄所屬登入工作階段、裝置資訊與最後使用時間
-- session_id 在 token 輪替時沿用,代表「一次登入」;既有 token 以自己的 token 值當作 session_id
-- ============================================================

ALTER TABLE refresh_token ADD COLUMN session_id VARCHAR(36) NULL;
ALTER TABLE refresh_token ADD COLUMN user_agent VARCHAR(255) NULL;
ALTER TABLE refresh_token ADD COLUMN ip_address VARCHAR(45) NULL;
ALTER TABLE refresh_token ADD COLUMN last_used_at DATETIME NULL;

UPDATE refresh_token SET session_id = token, last_used_at = created_at WHERE session_id IS NULL;

CREATE INDEX idx_refresh_token_member_session ON refresh_token (member_id, session_id);
