-- ============================================================
-- 邀請好友:會員邀請碼、被誰邀請、以及邀請獎勵是否已發放
-- ============================================================

ALTER TABLE member ADD COLUMN referral_code VARCHAR(12) NULL;
ALTER TABLE member ADD COLUMN referred_by_id BIGINT NULL;
ALTER TABLE member ADD COLUMN referral_rewarded BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE member ADD CONSTRAINT uk_member_referral_code UNIQUE (referral_code);
ALTER TABLE member ADD CONSTRAINT fk_member_referred_by FOREIGN KEY (referred_by_id) REFERENCES member (id);
