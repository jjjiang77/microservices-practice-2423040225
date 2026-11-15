SET NAMES utf8mb4;
CREATE DATABASE IF NOT EXISTS shangxiaotao_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS shangxiaotao_product DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
CREATE DATABASE IF NOT EXISTS shangxiaotao_purchase DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE shangxiaotao_user;
CREATE TABLE IF NOT EXISTS t_user
(
    id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '用户主键',
    student_id   VARCHAR(20) NOT NULL COMMENT '学号，唯一标识校内身份',
    nickname     VARCHAR(32) NOT NULL COMMENT '昵称',
    department   VARCHAR(40) NULL COMMENT '院系',
    role         VARCHAR(16) NOT NULL DEFAULT 'STUDENT' COMMENT '角色: STUDENT/ADMIN',
    credit_score INT         NOT NULL DEFAULT 100 COMMENT '信用分',
    created_at   DATETIME(6) NOT NULL COMMENT '创建时间',
    updated_at   DATETIME(6) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    CONSTRAINT uk_user_student_id UNIQUE (student_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '用户表';

USE shangxiaotao_product;
CREATE TABLE IF NOT EXISTS t_product
(
    id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '商品主键',
    title           VARCHAR(60)   NOT NULL COMMENT '标题',
    description     VARCHAR(500)  NULL COMMENT '描述',
    price           DECIMAL(10, 2) NOT NULL COMMENT '价格（元）',
    category        VARCHAR(24)   NOT NULL COMMENT '分类: TEXTBOOK/ELECTRONICS/DAILY/LARGE_ITEM/CLOTHING',
    condition_level VARCHAR(16)   NOT NULL COMMENT '新旧程度: NEW/LIKE_NEW/GOOD/FAIR',
    status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING' COMMENT '状态: PENDING/ON_SALE/SOLD/OFF_SHELF',
    seller_id       BIGINT        NOT NULL COMMENT '卖家用户 id',
    seller_nickname VARCHAR(64) NULL,
    created_at      DATETIME(6)   NOT NULL COMMENT '创建时间',
    updated_at      DATETIME(6)   NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    CONSTRAINT ck_product_price_positive CHECK (price > 0),
    INDEX idx_product_status (status),
    INDEX idx_product_category (category),
    INDEX idx_product_seller_id (seller_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT '商品表';

-- 商品的 seller_id 是用户服务返回的编号，跨库关系通过接口校验。
CREATE TABLE IF NOT EXISTS schema_migration (
    version VARCHAR(64) NOT NULL PRIMARY KEY
) ENGINE=InnoDB;
