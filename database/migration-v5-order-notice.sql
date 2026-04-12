-- 订单红点通知表
-- mysql -uroot -p123456 my_system < database/migration-v5-order-notice.sql

USE my_system;

CREATE TABLE IF NOT EXISTS order_notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  scope VARCHAR(10) NOT NULL COMMENT 'BUYER/SELLER',
  notice_type VARCHAR(20) NOT NULL COMMENT 'NEW_ORDER/PAY_PENDING/PAID/SHIPPED/RECEIVED',
  is_read TINYINT(1) NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_notice_user (user_id, is_read, scope),
  INDEX idx_notice_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
