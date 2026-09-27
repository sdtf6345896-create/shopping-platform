-- ============================================================
-- 商品圖庫:主圖之外的多張商品圖片(依 sort_order 排序)
-- ============================================================

CREATE TABLE product_image (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT       NOT NULL,
    url         VARCHAR(500) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_product_image_product FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_product_image_product ON product_image (product_id, sort_order);
