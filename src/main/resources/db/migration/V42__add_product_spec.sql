-- ============================================================
-- 商品規格表:商品頁顯示的「項目 / 內容」列(材質、產地、尺寸…),依 sort_order 排序
-- ============================================================

CREATE TABLE product_spec (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id  BIGINT       NOT NULL,
    spec_name   VARCHAR(30)  NOT NULL,
    spec_value  VARCHAR(200) NOT NULL,
    sort_order  INT          NOT NULL DEFAULT 0,
    CONSTRAINT fk_product_spec_product FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_product_spec_product ON product_spec (product_id, sort_order);
