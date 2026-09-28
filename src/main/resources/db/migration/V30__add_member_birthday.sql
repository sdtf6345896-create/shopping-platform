-- ============================================================
-- 生日禮:會員生日(設定後不可自行修改)與最近一次發放生日禮的年份
-- ============================================================

ALTER TABLE member ADD COLUMN birthday DATE NULL;
ALTER TABLE member ADD COLUMN birthday_reward_year INT NULL;

CREATE INDEX idx_member_birthday ON member (birthday);
