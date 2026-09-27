-- ============================================================
-- 熱門搜尋:記錄有搜尋結果的關鍵字次數(已正規化:去空白、小寫)
-- ============================================================

CREATE TABLE search_keyword (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    keyword           VARCHAR(30) NOT NULL,
    search_count      BIGINT      NOT NULL DEFAULT 0,
    last_searched_at  DATETIME    NOT NULL,
    CONSTRAINT uk_search_keyword UNIQUE (keyword)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_search_keyword_recent ON search_keyword (last_searched_at, search_count);
