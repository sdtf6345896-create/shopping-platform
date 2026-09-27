-- ============================================================
-- 管理員操作紀錄:後台所有寫入操作(含失敗)與敏感資料匯出
-- ============================================================

CREATE TABLE admin_audit_log (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id        BIGINT       NOT NULL,
    admin_username  VARCHAR(50)  NOT NULL,
    action          VARCHAR(50)  NOT NULL,
    target_type     VARCHAR(30)  NOT NULL,
    target_id       VARCHAR(50)  NULL,
    detail          VARCHAR(500) NULL,
    success         BOOLEAN      NOT NULL,
    error_message   VARCHAR(255) NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_admin_audit_log_created ON admin_audit_log (created_at);
CREATE INDEX idx_admin_audit_log_target ON admin_audit_log (target_type, target_id);
