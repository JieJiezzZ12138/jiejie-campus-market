-- 迁移 v16：购物车主关联从 username 迁移到 user_id，兼容保留 username
-- 执行：
-- mysql -uroot -p123456 my_system < database/migration-v16-cart-user-id.sql

USE my_system;
SET NAMES utf8mb4;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'cart' AND column_name = 'user_id'),
  'SELECT 1',
  'ALTER TABLE cart ADD COLUMN user_id BIGINT NULL AFTER id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE cart c
LEFT JOIN sys_user u ON u.username = c.username
SET c.user_id = u.id
WHERE c.user_id IS NULL;

UPDATE cart keep_row
JOIN (
  SELECT MIN(id) AS keep_id, user_id, product_id, SUM(quantity) AS total_quantity
  FROM cart
  WHERE user_id IS NOT NULL
  GROUP BY user_id, product_id
  HAVING COUNT(*) > 1
) merged
  ON keep_row.id = merged.keep_id
SET keep_row.quantity = merged.total_quantity;

DELETE c1
FROM cart c1
JOIN cart c2
  ON c1.user_id = c2.user_id
 AND c1.product_id = c2.product_id
 AND c1.id > c2.id
WHERE c1.user_id IS NOT NULL;

SET @idx_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'cart' AND index_name = 'idx_cart_user_id'),
  'SELECT 1',
  'CREATE INDEX idx_cart_user_id ON cart(user_id)'
);
PREPARE stmt FROM @idx_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @uk_sql = IF(
  EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'cart' AND index_name = 'uk_cart_user_product'),
  'SELECT 1',
  'CREATE UNIQUE INDEX uk_cart_user_product ON cart(user_id, product_id)'
);
PREPARE stmt FROM @uk_sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
