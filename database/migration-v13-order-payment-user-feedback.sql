-- 迁移 v13：订单支付信息 + 通用用户反馈
-- 执行：
-- mysql -uroot -p123456 my_system < database/migration-v13-order-payment-user-feedback.sql

ALTER TABLE orders
  ADD COLUMN IF NOT EXISTS payment_method VARCHAR(32) NULL COMMENT '支付方式',
  ADD COLUMN IF NOT EXISTS pay_time DATETIME NULL COMMENT '支付时间',
  ADD COLUMN IF NOT EXISTS pay_txn_no VARCHAR(64) NULL COMMENT '支付流水号';

CREATE TABLE IF NOT EXISTS user_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  reply_content VARCHAR(2000) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理 2驳回',
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);
