-- ============================================================
-- Demo / 展示用資料:補齊分類與商品,讓前台瀏覽與後台管理畫面
-- 有足夠的內容可看。不含會員或訂單資料(見 DataInitializer,
-- 那部分需要密碼雜湊與可重複的會員身分,用 Java seed 更合適)。
--
-- 注意:這是作品集展示用資料,若此專案真的部署到正式環境,
-- 請勿讓 flyway 對 prod 資料庫套用這支 migration。
-- ============================================================

-- ---------- 分類:女裝 ----------
INSERT INTO category (name, sort_order, status) VALUES ('女裝', 2, 'ACTIVE');
SET @cat_women = LAST_INSERT_ID();
INSERT INTO category (parent_id, name, sort_order, status) VALUES (@cat_women, '上衣', 1, 'ACTIVE');
SET @cat_women_top = LAST_INSERT_ID();
INSERT INTO category (parent_id, name, sort_order, status) VALUES (@cat_women, '洋裝', 2, 'ACTIVE');
SET @cat_women_dress = LAST_INSERT_ID();

-- ---------- 分類:3C家電 ----------
INSERT INTO category (name, sort_order, status) VALUES ('3C家電', 3, 'ACTIVE');
SET @cat_electronics = LAST_INSERT_ID();
INSERT INTO category (parent_id, name, sort_order, status) VALUES (@cat_electronics, '手機平板', 1, 'ACTIVE');
SET @cat_mobile = LAST_INSERT_ID();
INSERT INTO category (parent_id, name, sort_order, status) VALUES (@cat_electronics, '生活家電', 2, 'ACTIVE');
SET @cat_appliance = LAST_INSERT_ID();

-- ---------- 分類:生活居家 ----------
INSERT INTO category (name, sort_order, status) VALUES ('生活居家', 4, 'ACTIVE');
SET @cat_home = LAST_INSERT_ID();
INSERT INTO category (parent_id, name, sort_order, status) VALUES (@cat_home, '廚房用品', 1, 'ACTIVE');
SET @cat_kitchen = LAST_INSERT_ID();
INSERT INTO category (parent_id, name, sort_order, status) VALUES (@cat_home, '收納用品', 2, 'ACTIVE');
SET @cat_storage = LAST_INSERT_ID();

-- ---------- 商品:男裝 > 上衣(既有分類 id=2,再加一件) ----------
INSERT INTO product (category_id, name, description, price, main_image, status, sales_count) VALUES
  (2, '透氣條紋POLO衫', '彈性棉質面料,商務休閒皆宜,吸濕排汗好活動。', 780.00,
   'https://placehold.co/500x500/2a5db0/ffffff?text=POLO', 'ON_SALE', 12);
SET @p_polo = LAST_INSERT_ID();
INSERT INTO product_sku (product_id, sku_code, spec_name, price, stock) VALUES
  (@p_polo, 'POLO-NVY-M', '深藍/M', 780.00, 25),
  (@p_polo, 'POLO-NVY-L', '深藍/L', 780.00, 18),
  (@p_polo, 'POLO-WHT-M', '白色/M', 780.00, 20);

-- ---------- 商品:女裝 > 上衣 ----------
INSERT INTO product (category_id, name, description, price, main_image, status, sales_count) VALUES
  (@cat_women_top, '簡約開襟針織外套', '輕薄針織材質,春秋必備百搭單品。', 890.00,
   'https://placehold.co/500x500/c98a9c/ffffff?text=Cardigan', 'ON_SALE', 8);
SET @p_cardigan = LAST_INSERT_ID();
INSERT INTO product_sku (product_id, sku_code, spec_name, price, stock) VALUES
  (@p_cardigan, 'CARD-BEG-S', '杏色/S', 890.00, 15),
  (@p_cardigan, 'CARD-BEG-M', '杏色/M', 890.00, 20),
  (@p_cardigan, 'CARD-GRY-M', '灰色/M', 890.00, 10);

-- ---------- 商品:女裝 > 洋裝 ----------
INSERT INTO product (category_id, name, description, price, main_image, status, sales_count) VALUES
  (@cat_women_dress, '碎花雪紡洋裝', '夏季必備飄逸剪裁,顯瘦顯高。', 1280.00,
   'https://placehold.co/500x500/d9a441/ffffff?text=Dress', 'ON_SALE', 15);
SET @p_dress = LAST_INSERT_ID();
INSERT INTO product_sku (product_id, sku_code, spec_name, price, stock) VALUES
  (@p_dress, 'DRESS-FLR-S', '花色/S', 1280.00, 12),
  (@p_dress, 'DRESS-FLR-M', '花色/M', 1280.00, 9),
  (@p_dress, 'DRESS-FLR-L', '花色/L', 1280.00, 0);

-- ---------- 商品:3C家電 > 手機平板 ----------
INSERT INTO product (category_id, name, description, price, main_image, status, sales_count) VALUES
  (@cat_mobile, '真無線藍牙耳機', '主動降噪,單次續航 8 小時,含充電盒共 32 小時。', 1990.00,
   'https://placehold.co/500x500/1c8a5c/ffffff?text=Earbuds', 'ON_SALE', 34);
SET @p_earbuds = LAST_INSERT_ID();
INSERT INTO product_sku (product_id, sku_code, spec_name, price, stock) VALUES
  (@p_earbuds, 'EARBUD-BLK', '黑色', 1990.00, 40);

-- ---------- 商品:3C家電 > 生活家電(示範一個下架商品) ----------
INSERT INTO product (category_id, name, description, price, main_image, status, sales_count) VALUES
  (@cat_appliance, '手持式蒸氣掛燙機', '30 秒快速加熱,輕巧好收納。', 1590.00,
   'https://placehold.co/500x500/6a4fb5/ffffff?text=Steamer', 'OFF_SHELF', 5);
SET @p_steamer = LAST_INSERT_ID();
INSERT INTO product_sku (product_id, sku_code, spec_name, price, stock) VALUES
  (@p_steamer, 'STEAMER-WHT', '白色', 1590.00, 0);

-- ---------- 商品:生活居家 > 廚房用品 ----------
INSERT INTO product (category_id, name, description, price, main_image, status, sales_count) VALUES
  (@cat_kitchen, '316不鏽鋼保溫杯 500ml', '12 小時保冷、6 小時保熱,通過食品級認證。', 450.00,
   'https://placehold.co/500x500/a8720f/ffffff?text=Bottle', 'ON_SALE', 22);
SET @p_bottle = LAST_INSERT_ID();
INSERT INTO product_sku (product_id, sku_code, spec_name, price, stock) VALUES
  (@p_bottle, 'BOTTLE-BLK', '黑色', 450.00, 30),
  (@p_bottle, 'BOTTLE-WHT', '白色', 450.00, 25);

-- ---------- 商品:生活居家 > 收納用品 ----------
INSERT INTO product (category_id, name, description, price, main_image, status, sales_count) VALUES
  (@cat_storage, '可折疊收納箱(3入組)', '免安裝一秒成型,承重耐用,不用時可完全折平。', 690.00,
   'https://placehold.co/500x500/5b6b63/ffffff?text=Storage', 'ON_SALE', 6);
SET @p_storage_box = LAST_INSERT_ID();
INSERT INTO product_sku (product_id, sku_code, spec_name, price, stock) VALUES
  (@p_storage_box, 'STORAGEBOX-3PK', '3入組', 690.00, 50);
