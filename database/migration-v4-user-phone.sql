-- 新增手机号字段
-- mysql -uroot -p123456 my_system < database/migration-v4-user-phone.sql

USE my_system;

ALTER TABLE sys_user
  ADD COLUMN phone VARCHAR(20) NULL COMMENT '手机号' AFTER campus_address;

CREATE INDEX idx_sys_user_phone ON sys_user(phone);
