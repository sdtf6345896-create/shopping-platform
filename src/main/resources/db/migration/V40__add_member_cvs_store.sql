-- ============================================================
-- 常用超商門市:會員儲存的取貨門市與取件人,結帳時可直接選用
-- ============================================================

CREATE TABLE member_cvs_store (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id        BIGINT      NOT NULL,
    brand            VARCHAR(20) NOT NULL,
    store_name       VARCHAR(30) NOT NULL,
    store_code       VARCHAR(8)  NULL,
    recipient_name   VARCHAR(50) NOT NULL,
    recipient_phone  VARCHAR(10) NOT NULL,
    created_at       DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_member_cvs_store_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_member_cvs_store_member ON member_cvs_store (member_id, id);
