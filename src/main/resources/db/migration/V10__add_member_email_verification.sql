-- ============================================================
-- 會員 Email 驗證
-- ============================================================

ALTER TABLE member ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT FALSE;

-- 既有會員(含示範資料)視為已驗證,避免上線後既有帳號被擋在登入頁外
UPDATE member SET email_verified = TRUE;

CREATE TABLE email_verification_token (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id   BIGINT       NOT NULL,
    token       VARCHAR(100) NOT NULL,
    expires_at  DATETIME     NOT NULL,
    used        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_email_verification_token_token UNIQUE (token),
    CONSTRAINT fk_email_verification_token_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
