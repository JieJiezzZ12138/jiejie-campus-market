-- 邮箱验证码注册/找回密码
-- mysql -uroot -p123456 my_system < database/migration-v8-email-auth.sql

USE my_system;

ALTER TABLE sys_user
  ADD COLUMN email VARCHAR(128) NULL AFTER phone;

CREATE UNIQUE INDEX idx_sys_user_email ON sys_user(email);

CREATE TABLE IF NOT EXISTS email_verify_code (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(128) NOT NULL,
  biz_type VARCHAR(32) NOT NULL COMMENT 'REGISTER/RESET_PASSWORD',
  code VARCHAR(10) NOT NULL,
  expire_time DATETIME NOT NULL,
  used TINYINT(1) NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_email_biz (email, biz_type, used, expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
