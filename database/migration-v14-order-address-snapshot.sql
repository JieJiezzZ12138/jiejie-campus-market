-- 订单收货地址快照：让订单确认页选择的地址真正写入订单
-- 执行：
-- mysql -uroot -p123456 my_system < database/migration-v14-order-address-snapshot.sql

USE my_system;
SET NAMES utf8mb4;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'address_id'),
  'SELECT 1',
  'ALTER TABLE orders ADD COLUMN address_id BIGINT NULL AFTER selected_spec'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'receiver'),
  'SELECT 1',
  'ALTER TABLE orders ADD COLUMN receiver VARCHAR(64) NULL AFTER address_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'receiver_phone'),
  'SELECT 1',
  'ALTER TABLE orders ADD COLUMN receiver_phone VARCHAR(20) NULL AFTER receiver'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'receiver_address'),
  'SELECT 1',
  'ALTER TABLE orders ADD COLUMN receiver_address VARCHAR(500) NULL AFTER receiver_phone'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
