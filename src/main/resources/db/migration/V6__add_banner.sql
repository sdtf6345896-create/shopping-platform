-- ============================================================
-- 首頁輪播 Banner
-- ============================================================

CREATE TABLE banner (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(100) NOT NULL,
    subtitle    VARCHAR(200),
    image_url   VARCHAR(500) NOT NULL,
    link_url    VARCHAR(500),
    sort_order  INT          NOT NULL DEFAULT 0,
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO banner (title, subtitle, image_url, link_url, sort_order, status) VALUES
('新品上市', '本季新款搶先看', 'https://placehold.co/1200x400/e4393c/ffffff?text=新品上市', '/products?sort=createdAt,desc', 0, 'ACTIVE'),
('限時優惠', '精選商品折扣中', 'https://placehold.co/1200x400/4facfe/ffffff?text=限時優惠', '/products', 1, 'ACTIVE'),
('會員專屬', '註冊即享購物優惠', 'https://placehold.co/1200x400/43e97b/ffffff?text=會員專屬', '/register', 2, 'ACTIVE');
