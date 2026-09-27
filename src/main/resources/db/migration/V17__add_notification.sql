-- ============================================================
-- 站內通知:訂單狀態、退貨結果、提問回覆等,與 email 通知同步產生
-- ============================================================

CREATE TABLE notification (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT       NOT NULL,
    type        VARCHAR(20)  NOT NULL,
    title       VARCHAR(100) NOT NULL,
    content     VARCHAR(500) NOT NULL,
    link        VARCHAR(255) NULL,
    read_at     DATETIME     NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notification_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_notification_member_created ON notification (member_id, created_at);
CREATE INDEX idx_notification_member_unread ON notification (member_id, read_at);
