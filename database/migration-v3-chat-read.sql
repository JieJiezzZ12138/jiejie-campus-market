-- 新增私信已读状态表（用于红点/未读统计）
-- mysql -uroot -p123456 my_system < database/migration-v3-chat-read.sql

USE my_system;

CREATE TABLE IF NOT EXISTS chat_thread_read (
  thread_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  last_read_time DATETIME NOT NULL,
  PRIMARY KEY (thread_id, user_id),
  INDEX idx_read_user (user_id, last_read_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
