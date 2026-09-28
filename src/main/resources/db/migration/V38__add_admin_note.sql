-- ============================================================
-- 後台內部備註:管理員對訂單 / 會員留下的內部筆記(會員看不到)
-- ============================================================

CREATE TABLE admin_note (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    target_type     VARCHAR(10)  NOT NULL,
    target_id       BIGINT       NOT NULL,
    admin_id        BIGINT       NOT NULL,
    admin_username  VARCHAR(50)  NOT NULL,
    content         VARCHAR(500) NOT NULL,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_admin_note_target ON admin_note (target_type, target_id, id);
