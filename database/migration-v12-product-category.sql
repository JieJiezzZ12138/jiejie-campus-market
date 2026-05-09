-- 迁移 v12：商品分类管理
-- 执行：
-- mysql -uroot -p123456 my_system < database/migration-v12-product-category.sql

CREATE TABLE IF NOT EXISTS product_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL UNIQUE,
  icon VARCHAR(64) DEFAULT NULL,
  sort_no INT NOT NULL DEFAULT 100,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO product_category(name, icon, sort_no, status)
SELECT '数码电子', '💻', 10, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM product_category WHERE name='数码电子');

INSERT INTO product_category(name, icon, sort_no, status)
SELECT '书籍资料', '📚', 20, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM product_category WHERE name='书籍资料');

INSERT INTO product_category(name, icon, sort_no, status)
SELECT '衣物鞋帽', '👗', 30, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM product_category WHERE name='衣物鞋帽');

INSERT INTO product_category(name, icon, sort_no, status)
SELECT '生活用品', '🧴', 40, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM product_category WHERE name='生活用品');

INSERT INTO product_category(name, icon, sort_no, status)
SELECT '其他商品', '📦', 50, 1 FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM product_category WHERE name='其他商品');
