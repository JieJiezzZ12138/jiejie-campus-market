-- 迁移 v15：订单商品快照，移除 order-service 对 product 表的直接查询依赖
-- 执行：
-- mysql -uroot -p123456 my_system < database/migration-v15-order-product-snapshot.sql

USE my_system;
SET NAMES utf8mb4;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'product_name'),
  'SELECT 1',
  'ALTER TABLE orders ADD COLUMN product_name VARCHAR(120) NULL AFTER product_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'product_image'),
  'SELECT 1',
  'ALTER TABLE orders ADD COLUMN product_image VARCHAR(500) NULL AFTER product_name'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'seller_id'),
  'SELECT 1',
  'ALTER TABLE orders ADD COLUMN seller_id BIGINT NULL AFTER product_image'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE orders o
LEFT JOIN product p ON p.id = o.product_id
SET o.product_name = COALESCE(o.product_name, p.name),
    o.product_image = COALESCE(o.product_image, p.image),
    o.seller_id = COALESCE(o.seller_id, p.seller_id)
WHERE o.product_name IS NULL OR o.product_image IS NULL OR o.seller_id IS NULL;
