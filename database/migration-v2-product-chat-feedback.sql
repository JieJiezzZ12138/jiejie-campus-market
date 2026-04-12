-- 在已有 v1 私信表的基础上升级：商品页私聊线程 + 交易反馈
-- mysql -uroot -p123456 my_system < database/migration-v2-product-chat-feedback.sql

USE my_system;

CREATE TABLE IF NOT EXISTS chat_thread (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL COMMENT '咨询者/买家侧用户，与 seller 对应一条会话',
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

-- 以下对已存在的 private_message 表执行一次即可；若列已存在会报错，可忽略对应行
ALTER TABLE private_message MODIFY COLUMN order_id BIGINT NULL;
ALTER TABLE private_message ADD COLUMN thread_id BIGINT NULL COMMENT '关联 chat_thread' AFTER order_id;
ALTER TABLE private_message ADD COLUMN product_id BIGINT NULL AFTER thread_id;
CREATE INDEX idx_pm_thread_time ON private_message (thread_id, create_time);
