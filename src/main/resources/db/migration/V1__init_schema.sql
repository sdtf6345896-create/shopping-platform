-- ============================================================
-- 購物平台初始 Schema
-- ============================================================

CREATE TABLE member (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    email       VARCHAR(100) NOT NULL,
    password    VARCHAR(255) NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    phone       VARCHAR(20),
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_member_email UNIQUE (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE admin (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL,
    password    VARCHAR(255) NOT NULL,
    name        VARCHAR(50)  NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'ADMIN',
    status      VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_admin_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE category (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    parent_id   BIGINT,
    name        VARCHAR(50) NOT NULL,
    sort_order  INT         NOT NULL DEFAULT 0,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_category_parent FOREIGN KEY (parent_id) REFERENCES category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    category_id  BIGINT        NOT NULL,
    name         VARCHAR(200)  NOT NULL,
    description  TEXT,
    price        DECIMAL(10,2) NOT NULL,
    main_image   VARCHAR(500),
    status       VARCHAR(20)   NOT NULL DEFAULT 'OFF_SHELF',
    sales_count  INT           NOT NULL DEFAULT 0,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES category (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE product_sku (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id   BIGINT        NOT NULL,
    sku_code     VARCHAR(50)   NOT NULL,
    spec_name    VARCHAR(100)  NOT NULL,
    price        DECIMAL(10,2) NOT NULL,
    stock        INT           NOT NULL DEFAULT 0,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_sku_code UNIQUE (sku_code),
    CONSTRAINT fk_sku_product FOREIGN KEY (product_id) REFERENCES product (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE address (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id       BIGINT       NOT NULL,
    recipient_name  VARCHAR(50)  NOT NULL,
    phone           VARCHAR(20)  NOT NULL,
    postal_code     VARCHAR(10),
    city            VARCHAR(50)  NOT NULL,
    district        VARCHAR(50)  NOT NULL,
    detail_address  VARCHAR(255) NOT NULL,
    is_default      TINYINT(1)   NOT NULL DEFAULT 0,
    created_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_address_member FOREIGN KEY (member_id) REFERENCES member (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE cart_item (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    member_id       BIGINT   NOT NULL,
    product_sku_id  BIGINT   NOT NULL,
    quantity        INT      NOT NULL DEFAULT 1,
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_cart_member_sku UNIQUE (member_id, product_sku_id),
    CONSTRAINT fk_cart_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_cart_sku FOREIGN KEY (product_sku_id) REFERENCES product_sku (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE orders (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no          VARCHAR(30)   NOT NULL,
    member_id         BIGINT        NOT NULL,
    address_id        BIGINT,
    total_amount      DECIMAL(10,2) NOT NULL,
    payment_method    VARCHAR(20)   NOT NULL,
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING_PAYMENT',
    receiver_name     VARCHAR(50)   NOT NULL,
    receiver_phone    VARCHAR(20)   NOT NULL,
    receiver_address  VARCHAR(255)  NOT NULL,
    created_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_order_no UNIQUE (order_no),
    CONSTRAINT fk_order_member FOREIGN KEY (member_id) REFERENCES member (id),
    CONSTRAINT fk_order_address FOREIGN KEY (address_id) REFERENCES address (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE order_item (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id        BIGINT        NOT NULL,
    product_sku_id  BIGINT        NOT NULL,
    product_name    VARCHAR(200)  NOT NULL,
    spec_name       VARCHAR(100),
    unit_price      DECIMAL(10,2) NOT NULL,
    quantity        INT           NOT NULL,
    subtotal        DECIMAL(10,2) NOT NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT fk_order_item_sku FOREIGN KEY (product_sku_id) REFERENCES product_sku (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_product_category ON product (category_id);
CREATE INDEX idx_sku_product ON product_sku (product_id);
CREATE INDEX idx_order_member ON orders (member_id);
CREATE INDEX idx_order_item_order ON order_item (order_id);
