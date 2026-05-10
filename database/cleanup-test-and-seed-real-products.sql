-- 清理分页测试商品 + 补充真实商品
-- 执行：
-- mysql -uroot -p123456 my_system < database/cleanup-test-and-seed-real-products.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 选一个管理员作为卖家（优先 merch_admin）
SET @seller := (
  SELECT id FROM sys_user
  WHERE username IN ('merch_admin','ops_admin','SUPER_ADMIN')
  ORDER BY FIELD(username,'merch_admin','ops_admin','SUPER_ADMIN')
  LIMIT 1
);

-- 1) 删除之前的分页测试商品
DELETE FROM product_favorite WHERE product_id IN (SELECT id FROM product WHERE name LIKE '【分页测试】%');
DELETE FROM product_review WHERE product_id IN (SELECT id FROM product WHERE name LIKE '【分页测试】%');
DELETE FROM cart WHERE product_id IN (SELECT id FROM product WHERE name LIKE '【分页测试】%');
DELETE FROM orders WHERE product_id IN (SELECT id FROM product WHERE name LIKE '【分页测试】%');
DELETE FROM product WHERE name LIKE '【分页测试】%';

-- 2) 补充真实商品（避免重复插入）
INSERT INTO product
  (name, description, category, price, original_price, image, image_url, stock, seller_id, spec_json, is_seckill, seckill_price, seckill_start_time, seckill_end_time, audit_status, publish_status, create_time)
SELECT * FROM (
  SELECT '华为 MateBook 14 2024' AS name, '14 英寸轻薄本，2.8K 屏，办公学习高性价比。' AS description, '数码电子' AS category, 5299.00 AS price, 5999.00 AS original_price, NULL AS image, NULL AS image_url, 14 AS stock, @seller AS seller_id, '{"colors":["深空灰"],"sizes":["16G+1TB"]}' AS spec_json, 0 AS is_seckill, NULL AS seckill_price, NULL AS seckill_start_time, NULL AS seckill_end_time, 1 AS audit_status, 1 AS publish_status, NOW() AS create_time
  UNION ALL
  SELECT '戴森 Supersonic 吹风机', '智能温控，快速干发，适合长发和卷发护理。', '家用电器', 2799.00, 3199.00, NULL, NULL, 10, @seller, '{"colors":["银白","玫红"],"sizes":["标准"]}', 0, NULL, NULL, NULL, 1, 1, NOW()
  UNION ALL
  SELECT '任天堂 Switch OLED', 'OLED 屏幕版本，支持掌机/主机双模式。', '数码电子', 1899.00, 2299.00, NULL, NULL, 20, @seller, '{"colors":["白色","霓虹红蓝"],"sizes":["标准"]}', 0, NULL, NULL, NULL, 1, 1, NOW()
  UNION ALL
  SELECT '优衣库轻型羽绒服', '轻便保暖，可机洗，通勤旅行都适合。', '服饰鞋帽', 299.00, 399.00, NULL, NULL, 36, @seller, '{"colors":["黑色","藏青","米白"],"sizes":["M","L","XL"]}', 0, NULL, NULL, NULL, 1, 1, NOW()
  UNION ALL
  SELECT '飞利浦电动牙刷 HX6730', '声波震动清洁，3 种刷牙模式，支持感应充电。', '生活用品', 359.00, 499.00, NULL, NULL, 26, @seller, '{"colors":["白色"],"sizes":["标准"]}', 0, NULL, NULL, NULL, 1, 1, NOW()
  UNION ALL
  SELECT '乐高 21342 昆虫收藏', '乐高 Ideas 系列，三款昆虫模型，适合收藏展示。', '图书文创', 499.00, 599.00, NULL, NULL, 12, @seller, '{"colors":[], "sizes":["套装"]}', 0, NULL, NULL, NULL, 1, 1, NOW()
) AS t
WHERE NOT EXISTS (SELECT 1 FROM product p WHERE p.name = t.name);

SET FOREIGN_KEY_CHECKS = 1;
