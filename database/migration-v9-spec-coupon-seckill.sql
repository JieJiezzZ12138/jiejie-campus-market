-- 商品规格 + 优惠券 + 秒杀
-- mysql -uroot -p123456 my_system < database/migration-v9-spec-coupon-seckill.sql

USE my_system;

ALTER TABLE product
  ADD COLUMN spec_json TEXT NULL COMMENT '规格JSON，如{\"colors\":[\"黑\",\"白\"],\"sizes\":[\"S\",\"M\"]}' AFTER description,
  ADD COLUMN is_seckill TINYINT(1) NOT NULL DEFAULT 0 AFTER publish_status,
  ADD COLUMN seckill_price DECIMAL(10,2) NULL AFTER is_seckill,
  ADD COLUMN seckill_start_time DATETIME NULL AFTER seckill_price,
  ADD COLUMN seckill_end_time DATETIME NULL AFTER seckill_start_time;

ALTER TABLE cart
  ADD COLUMN selected_spec VARCHAR(255) NULL COMMENT '已选规格（颜色/尺寸）' AFTER quantity;

ALTER TABLE orders
  ADD COLUMN selected_spec VARCHAR(255) NULL AFTER buy_count,
  ADD COLUMN coupon_id BIGINT NULL AFTER selected_spec,
  ADD COLUMN coupon_title VARCHAR(120) NULL AFTER coupon_id,
  ADD COLUMN discount_amount DECIMAL(10,2) NULL AFTER coupon_title;

CREATE TABLE IF NOT EXISTS coupon (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL,
  threshold_amount DECIMAL(10,2) NOT NULL,
  discount_amount DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  status TINYINT(1) NOT NULL DEFAULT 1,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_coupon (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  coupon_id BIGINT NOT NULL,
  use_status TINYINT(1) NOT NULL DEFAULT 0,
  use_time DATETIME NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_coupon (user_id, use_status),
  INDEX idx_coupon (coupon_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
