-- ============================================================
-- 商品評價摘要(平均星等、評論數)存在商品上,列表頁顯示與排序不必每次彙總 product_review
-- ============================================================

ALTER TABLE product ADD COLUMN rating_average DECIMAL(2,1) NOT NULL DEFAULT 0;
ALTER TABLE product ADD COLUMN review_count INT NOT NULL DEFAULT 0;

UPDATE product p
JOIN (
    SELECT product_id, ROUND(AVG(rating), 1) AS avg_rating, COUNT(*) AS cnt
    FROM product_review
    GROUP BY product_id
) r ON r.product_id = p.id
SET p.rating_average = r.avg_rating,
    p.review_count = r.cnt;
