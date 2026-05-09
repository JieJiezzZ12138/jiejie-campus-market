-- 纠纷三方聊天室 + 管理员向超管反馈
-- mysql -uroot -p123456 my_system < database/migration-v10-dispute-admin-feedback.sql

USE my_system;

CREATE TABLE IF NOT EXISTS dispute_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  sender_role VARCHAR(20) NOT NULL COMMENT 'BUYER/SELLER/ADMIN',
  content VARCHAR(2000) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_dispute_order (order_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reporter_id BIGINT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  status TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0待处理 1已处理 2驳回',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_admin_feedback_reporter (reporter_id, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
