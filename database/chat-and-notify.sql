-- 在 my_system 库执行（与 order-service 的 spring.datasource.url 一致）
-- mysql -uroot -p123456 my_system < database/chat-and-notify.sql

USE my_system;

CREATE TABLE IF NOT EXISTS chat_thread (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_product_customer (product_id, customer_id),
  INDEX idx_seller (seller_id),
  INDEX idx_customer (customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS private_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NULL,
  thread_id BIGINT NULL,
  product_id BIGINT NULL,
  sender_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_order_time (order_id, create_time),
  INDEX idx_pm_thread_time (thread_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NULL,
  sender_id BIGINT NOT NULL,
  preview VARCHAR(500) NOT NULL,
  is_read TINYINT(1) NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_unread_time (is_read, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
