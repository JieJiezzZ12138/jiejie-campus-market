-- 杰物 Jemall：一键清理 + 补表 + 测试数据（MySQL 8）
-- 执行：
-- mysql -uroot -p123456 my_system < database/reset-seed-ecommerce.sql

USE my_system;
SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- =====================================================
-- 0) 兜底建表：缺什么补什么（保证脚本一次通过）
-- =====================================================

CREATE TABLE IF NOT EXISTS sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  nickname VARCHAR(64) DEFAULT NULL,
  avatar VARCHAR(500) DEFAULT NULL,
  phone VARCHAR(20) DEFAULT NULL,
  email VARCHAR(128) DEFAULT NULL,
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  audit_status TINYINT(1) NOT NULL DEFAULT 1,
  campus_address VARCHAR(255) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(120) NOT NULL,
  description VARCHAR(2000) DEFAULT NULL,
  category VARCHAR(64) DEFAULT NULL,
  price DECIMAL(10,2) NOT NULL DEFAULT 0,
  original_price DECIMAL(10,2) DEFAULT 0,
  image VARCHAR(500) DEFAULT NULL,
  image_url VARCHAR(500) DEFAULT NULL,
  stock INT NOT NULL DEFAULT 0,
  seller_id BIGINT DEFAULT NULL,
  spec_json TEXT NULL,
  is_seckill TINYINT(1) NOT NULL DEFAULT 0,
  seckill_price DECIMAL(10,2) DEFAULT NULL,
  seckill_start_time DATETIME DEFAULT NULL,
  seckill_end_time DATETIME DEFAULT NULL,
  audit_status TINYINT(1) NOT NULL DEFAULT 1,
  publish_status TINYINT(1) NOT NULL DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS orders (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_no VARCHAR(64) NOT NULL UNIQUE,
  buyer_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  buy_count INT NOT NULL DEFAULT 1,
  selected_spec VARCHAR(255) DEFAULT NULL,
  address_id BIGINT DEFAULT NULL,
  receiver VARCHAR(64) DEFAULT NULL,
  receiver_phone VARCHAR(20) DEFAULT NULL,
  receiver_address VARCHAR(500) DEFAULT NULL,
  coupon_id BIGINT DEFAULT NULL,
  coupon_title VARCHAR(120) DEFAULT NULL,
  discount_amount DECIMAL(10,2) DEFAULT NULL,
  total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
  order_status TINYINT NOT NULL DEFAULT 0,
  payment_method VARCHAR(32) DEFAULT NULL,
  pay_time DATETIME DEFAULT NULL,
  pay_txn_no VARCHAR(64) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 兼容旧库：补齐 orders 缺失列（按需）
SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'selected_spec'),
  'SELECT 1',
  'ALTER TABLE orders ADD COLUMN selected_spec VARCHAR(255) NULL AFTER buy_count'
);

-- 兼容历史结构：商品图集字段改为 TEXT，支持最多 9 张本地上传图片路径
SET @exists_image_url := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'product'
    AND COLUMN_NAME = 'image_url'
);
SET @alter_image_url := IF(
  @exists_image_url = 1,
  'ALTER TABLE product MODIFY COLUMN image_url TEXT NULL',
  'SELECT 1'
);
PREPARE stmt_alter_image_url FROM @alter_image_url;
EXECUTE stmt_alter_image_url;
DEALLOCATE PREPARE stmt_alter_image_url;
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'coupon_id'),
  'SELECT 1',
  'ALTER TABLE orders ADD COLUMN coupon_id BIGINT NULL AFTER selected_spec'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

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

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'coupon_title'),
  'SELECT 1',
  'ALTER TABLE orders ADD COLUMN coupon_title VARCHAR(120) NULL AFTER coupon_id'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'discount_amount'),
  'SELECT 1',
  'ALTER TABLE orders ADD COLUMN discount_amount DECIMAL(10,2) NULL AFTER coupon_title'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'payment_method'),
  'SELECT 1',
  'ALTER TABLE orders ADD COLUMN payment_method VARCHAR(32) NULL AFTER order_status'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'pay_time'),
  'SELECT 1',
  'ALTER TABLE orders ADD COLUMN pay_time DATETIME NULL AFTER payment_method'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(
  EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'orders' AND column_name = 'pay_txn_no'),
  'SELECT 1',
  'ALTER TABLE orders ADD COLUMN pay_txn_no VARCHAR(64) NULL AFTER pay_time'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS cart (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  username VARCHAR(64) NOT NULL,
  product_id BIGINT NOT NULL,
  quantity INT NOT NULL DEFAULT 1,
  selected_spec VARCHAR(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_favorite (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  product_id BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_review (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  rating TINYINT NOT NULL,
  content VARCHAR(500) NOT NULL,
  image_url VARCHAR(500) DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_address (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  receiver VARCHAR(64) NOT NULL,
  phone VARCHAR(20) NOT NULL,
  province VARCHAR(64) NOT NULL,
  city VARCHAR(64) NOT NULL,
  district VARCHAR(64) NOT NULL,
  detail_address VARCHAR(255) NOT NULL,
  is_default TINYINT(1) NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS system_notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL,
  content VARCHAR(2000) NOT NULL,
  status TINYINT(1) NOT NULL DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS banner_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL,
  subtitle VARCHAR(255) DEFAULT NULL,
  image_url VARCHAR(500) DEFAULT NULL,
  bg_color VARCHAR(32) DEFAULT NULL,
  sort_no INT NOT NULL DEFAULT 100,
  status TINYINT(1) NOT NULL DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS coupon (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(120) NOT NULL,
  threshold_amount DECIMAL(10,2) NOT NULL,
  discount_amount DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  status TINYINT(1) NOT NULL DEFAULT 1,
  start_time DATETIME NOT NULL,
  end_time DATETIME NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_coupon (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  coupon_id BIGINT NOT NULL,
  use_status TINYINT(1) NOT NULL DEFAULT 0,
  use_time DATETIME DEFAULT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS product_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(64) NOT NULL UNIQUE,
  icon VARCHAR(64) DEFAULT NULL,
  sort_no INT NOT NULL DEFAULT 100,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_thread (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  product_id BIGINT NOT NULL,
  seller_id BIGINT NOT NULL,
  customer_id BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_product_customer (product_id, customer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS private_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT DEFAULT NULL,
  thread_id BIGINT DEFAULT NULL,
  product_id BIGINT DEFAULT NULL,
  sender_id BIGINT NOT NULL,
  receiver_id BIGINT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS chat_thread_read (
  thread_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  last_read_time DATETIME NOT NULL,
  PRIMARY KEY (thread_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS dispute_message (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  sender_id BIGINT NOT NULL,
  sender_role VARCHAR(20) NOT NULL,
  content VARCHAR(2000) NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS order_notice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT NOT NULL,
  user_id BIGINT NOT NULL,
  scope VARCHAR(10) NOT NULL,
  notice_type VARCHAR(20) NOT NULL,
  is_read TINYINT(1) NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_notification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  order_id BIGINT DEFAULT NULL,
  sender_id BIGINT NOT NULL,
  preview VARCHAR(500) NOT NULL,
  is_read TINYINT(1) NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS admin_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reporter_id BIGINT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  status TINYINT(1) NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS user_feedback (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  content VARCHAR(2000) NOT NULL,
  reply_content VARCHAR(2000) DEFAULT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS email_verify_code (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  email VARCHAR(128) NOT NULL,
  biz_type VARCHAR(32) NOT NULL,
  code VARCHAR(10) NOT NULL,
  expire_time DATETIME NOT NULL,
  used TINYINT(1) NOT NULL DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- =====================================================
-- 1) 动态清空：仅清理存在的表
-- =====================================================
DROP PROCEDURE IF EXISTS truncate_if_exists;
DELIMITER $$
CREATE PROCEDURE truncate_if_exists(IN tname VARCHAR(128))
BEGIN
  IF EXISTS (
    SELECT 1 FROM information_schema.tables
    WHERE table_schema = DATABASE() AND table_name = tname
  ) THEN
    SET @sql = CONCAT('TRUNCATE TABLE `', tname, '`');
    PREPARE stmt FROM @sql;
    EXECUTE stmt;
    DEALLOCATE PREPARE stmt;
  END IF;
END $$
DELIMITER ;

CALL truncate_if_exists('private_message');
CALL truncate_if_exists('chat_thread_read');
CALL truncate_if_exists('chat_thread');
CALL truncate_if_exists('dispute_message');
CALL truncate_if_exists('order_feedback');
CALL truncate_if_exists('order_notice');
CALL truncate_if_exists('admin_notification');
CALL truncate_if_exists('cart');
CALL truncate_if_exists('product_favorite');
CALL truncate_if_exists('product_review');
CALL truncate_if_exists('user_coupon');
CALL truncate_if_exists('orders');
CALL truncate_if_exists('user_address');
CALL truncate_if_exists('user_feedback');
CALL truncate_if_exists('admin_feedback');
CALL truncate_if_exists('email_verify_code');
CALL truncate_if_exists('banner_item');
CALL truncate_if_exists('system_notice');
CALL truncate_if_exists('coupon');
CALL truncate_if_exists('product');
CALL truncate_if_exists('product_category');

DROP PROCEDURE IF EXISTS truncate_if_exists;

-- 删除普通用户，保留管理员体系
DELETE FROM sys_user WHERE role = 'USER';

-- 统一测试账号密码为 123456（BCrypt）
-- 明文：123456
-- 哈希：$2y$10$lIquPCQjg8SbIs.F0u3iCOVEu.Srjm7Es84wtAZcoZStYvtbv1NAm
SET @pwd123456 = '$2y$10$lIquPCQjg8SbIs.F0u3iCOVEu.Srjm7Es84wtAZcoZStYvtbv1NAm';

-- =====================================================
-- 2) 管理员账号规范
-- =====================================================
UPDATE sys_user
SET username = 'SUPER_ADMIN', nickname = 'SUPER_ADMIN', role = 'SUPER_ADMIN'
WHERE role = 'SUPER_ADMIN'
  AND (username = 'admin' OR username = 'super_admin' OR nickname = '超级管理员');

INSERT INTO sys_user(username, password, nickname, avatar, phone, email, role, audit_status, campus_address, create_time)
SELECT 'SUPER_ADMIN', @pwd123456, 'SUPER_ADMIN', NULL, '13800000000', 'superadmin@jemall.local', 'SUPER_ADMIN', 1, '总部运营中心', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'SUPER_ADMIN');

INSERT INTO sys_user(username, password, nickname, avatar, phone, email, role, audit_status, campus_address, create_time)
SELECT 'ops_admin', @pwd123456, '运营管理员', NULL, '13900000001', 'ops_admin@jemall.local', 'ADMIN', 1, '杰物运营中心 A 座', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'ops_admin');

INSERT INTO sys_user(username, password, nickname, avatar, phone, email, role, audit_status, campus_address, create_time)
SELECT 'merch_admin', @pwd123456, '商品管理员', NULL, '13900000002', 'merch_admin@jemall.local', 'ADMIN', 1, '杰物商品中心 B 座', NOW()
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'merch_admin');

-- 普通用户账号（测试）
INSERT INTO sys_user(username, password, nickname, avatar, phone, email, role, audit_status, campus_address, create_time) VALUES
('buyer_chen', @pwd123456, '陈雨桐', NULL, '13711110001', 'chen.yutong@demo.local', 'USER', 1, '上海市浦东新区锦绣路 88 弄 6 号', NOW()),
('buyer_lin',  @pwd123456, '林子皓', NULL, '13711110002', 'lin.zihao@demo.local', 'USER', 1, '杭州市西湖区文三路 199 号', NOW()),
('buyer_wang', @pwd123456, '王可欣', NULL, '13711110003', 'wang.kexin@demo.local', 'USER', 1, '成都市高新区天府三街 120 号', NOW()),
('buyer_sun',  @pwd123456, '孙浩然', NULL, '13711110004', 'sun.haoran@demo.local', 'USER', 1, '南京市建邺区江东中路 68 号', NOW());

-- 兼容不重置数据场景：把现有测试账号密码也统一改为 123456
UPDATE sys_user
SET password = @pwd123456
WHERE username IN ('SUPER_ADMIN', 'ops_admin', 'merch_admin', 'buyer_chen', 'buyer_lin', 'buyer_wang', 'buyer_sun');

-- =====================================================
-- 3) 分类、公告、轮播、优惠券
-- =====================================================
INSERT INTO product_category(name, icon, sort_no, status, create_time) VALUES
('数码电子', '💻', 10, 1, NOW()),
('家用电器', '🏠', 20, 1, NOW()),
('服饰鞋帽', '👟', 30, 1, NOW()),
('食品生鲜', '🍎', 40, 1, NOW()),
('图书文创', '📚', 50, 1, NOW()),
('运动户外', '🏃', 60, 1, NOW());

INSERT INTO banner_item(title, subtitle, image_url, bg_color, sort_no, status, create_time) VALUES
('杰物 618 品质盛典', '手机数码至高直降 800 元', NULL, '#4f8cff', 10, 1, NOW()),
('家电焕新周', '空调冰洗组合优惠，次日达覆盖 100 城', NULL, '#1f9d8b', 20, 1, NOW()),
('新客专享礼', '注册即领满 199 减 30 优惠券', NULL, '#f59e0b', 30, 1, NOW());

INSERT INTO system_notice(title, content, status, create_time) VALUES
('发货时效说明', '工作日 16:00 前订单优先当日出库，偏远地区以物流时效为准。', 1, NOW()),
('退款流程升级', '申请退款后将进入商家审核，若出现争议由平台管理员介入裁决。', 1, NOW()),
('客服服务时间', '在线客服服务时间 09:00-22:00，紧急问题可在订单页提交反馈。', 1, NOW());

INSERT INTO coupon(title, threshold_amount, discount_amount, stock, status, start_time, end_time, create_time) VALUES
('全品类满199减30', 199.00, 30.00, 500, 1, NOW(), DATE_ADD(NOW(), INTERVAL 30 DAY), NOW()),
('数码专享满999减120', 999.00, 120.00, 200, 1, NOW(), DATE_ADD(NOW(), INTERVAL 20 DAY), NOW()),
('图书文创满99减15', 99.00, 15.00, 300, 1, NOW(), DATE_ADD(NOW(), INTERVAL 25 DAY), NOW());

-- =====================================================
-- 4) 商品数据（管理员即商家）
-- =====================================================
SET @seller1 = (SELECT id FROM sys_user WHERE username = 'ops_admin' LIMIT 1);
SET @seller2 = (SELECT id FROM sys_user WHERE username = 'merch_admin' LIMIT 1);

INSERT INTO product(name, description, category, price, original_price, image, image_url, stock, seller_id, spec_json, is_seckill, seckill_price, seckill_start_time, seckill_end_time, audit_status, publish_status, create_time) VALUES
('Apple iPhone 14 128G（在保）', '国行正品，电池健康 92%，附原装充电线，支持7天无理由。', '数码电子', 3899.00, 4599.00, NULL, NULL, 18, @seller1, '{\"colors\":[\"午夜色\",\"蓝色\"],\"sizes\":[\"128G\"]}', 1, 3699.00, NOW(), DATE_ADD(NOW(), INTERVAL 10 DAY), 1, 1, NOW()),
('小米 13 Ultra 12+256', '全套配件齐全，外观成色良好，影像旗舰机型。', '数码电子', 3299.00, 3999.00, NULL, NULL, 15, @seller1, '{\"colors\":[\"黑色\",\"白色\"],\"sizes\":[\"256G\"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
('戴森吹风机 HD15', '官方授权渠道，含柔和风嘴与顺滑风嘴，支持正品验真。', '家用电器', 2799.00, 3199.00, NULL, NULL, 9, @seller2, '{\"colors\":[\"紫红色\"],\"sizes\":[\"标准版\"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
('美的 1.5 匹变频空调', '一级能效，包安装（标准安装范围内），下单后 48 小时内联系。', '家用电器', 2399.00, 2799.00, NULL, NULL, 12, @seller2, '{\"colors\":[\"白色\"],\"sizes\":[\"1.5匹\"]}', 1, 2199.00, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, 1, NOW()),
('Nike Pegasus 41 跑鞋', '透气网面，缓震升级，适合通勤慢跑。', '服饰鞋帽', 699.00, 899.00, NULL, NULL, 30, @seller1, '{\"colors\":[\"黑白\",\"灰蓝\"],\"sizes\":[\"41\",\"42\",\"43\",\"44\"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
('云南冰糖蓝莓 2kg', '冷链直发，颗粒饱满，坏果包赔。', '食品生鲜', 89.00, 129.00, NULL, NULL, 60, @seller2, '{\"colors\":[],\"sizes\":[\"2kg\"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
('《人类简史》新版', '纸张与印刷品质优，适合收藏与送礼。', '图书文创', 58.00, 79.00, NULL, NULL, 45, @seller1, '{\"colors\":[],\"sizes\":[\"平装\"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
('露营折叠桌椅套装', '铝合金骨架，轻便耐用，适合自驾露营。', '运动户外', 329.00, 459.00, NULL, NULL, 20, @seller2, '{\"colors\":[\"卡其色\"],\"sizes\":[\"四人套装\"]}', 0, NULL, NULL, NULL, 1, 1, NOW());

-- =====================================================
-- 5) 地址、领券、收藏、评价、购物车、订单
-- =====================================================
SET @u1 = (SELECT id FROM sys_user WHERE username = 'buyer_chen' LIMIT 1);
SET @u2 = (SELECT id FROM sys_user WHERE username = 'buyer_lin' LIMIT 1);
SET @u3 = (SELECT id FROM sys_user WHERE username = 'buyer_wang' LIMIT 1);
SET @u4 = (SELECT id FROM sys_user WHERE username = 'buyer_sun' LIMIT 1);

INSERT INTO user_address(user_id, receiver, phone, province, city, district, detail_address, is_default, create_time) VALUES
(@u1, '陈雨桐', '13711110001', '上海市', '上海市', '浦东新区', '锦绣路 88 弄 6 号 1201', 1, NOW()),
(@u2, '林子皓', '13711110002', '浙江省', '杭州市', '西湖区', '文三路 199 号 2 单元 901', 1, NOW()),
(@u3, '王可欣', '13711110003', '四川省', '成都市', '高新区', '天府三街 120 号 3 栋 1502', 1, NOW()),
(@u4, '孙浩然', '13711110004', '江苏省', '南京市', '建邺区', '江东中路 68 号 8 栋 702', 1, NOW());

INSERT INTO user_coupon(user_id, coupon_id, use_status, use_time, create_time)
SELECT @u1, c.id, 0, NULL, NOW() FROM coupon c WHERE c.title = '全品类满199减30';
INSERT INTO user_coupon(user_id, coupon_id, use_status, use_time, create_time)
SELECT @u2, c.id, 0, NULL, NOW() FROM coupon c WHERE c.title = '数码专享满999减120';

INSERT INTO product_favorite(user_id, product_id, create_time)
SELECT @u1, p.id, NOW() FROM product p WHERE p.name = 'Apple iPhone 14 128G（在保）'
UNION ALL
SELECT @u1, p.id, NOW() FROM product p WHERE p.name = 'Nike Pegasus 41 跑鞋'
UNION ALL
SELECT @u2, p.id, NOW() FROM product p WHERE p.name = '戴森吹风机 HD15';

INSERT INTO product_review(product_id, user_id, rating, content, image_url, create_time)
SELECT p.id, @u1, 5, '发货很快，包装完整，机器运行流畅，客服回复也及时。', NULL, NOW() FROM product p WHERE p.name = '小米 13 Ultra 12+256'
UNION ALL
SELECT p.id, @u2, 4, '鞋子尺码标准，脚感舒适，跑步一周体验不错。', NULL, NOW() FROM product p WHERE p.name = 'Nike Pegasus 41 跑鞋'
UNION ALL
SELECT p.id, @u3, 5, '蓝莓新鲜度高，冷链到家，孩子很喜欢。', NULL, NOW() FROM product p WHERE p.name = '云南冰糖蓝莓 2kg';

INSERT INTO cart(username, product_id, quantity, selected_spec)
SELECT 'buyer_wang', p.id, 1, '黑白/42' FROM product p WHERE p.name = 'Nike Pegasus 41 跑鞋'
UNION ALL
SELECT 'buyer_wang', p.id, 2, '2kg' FROM product p WHERE p.name = '云南冰糖蓝莓 2kg';

SET @p1 = (SELECT id FROM product WHERE name = 'Apple iPhone 14 128G（在保）' LIMIT 1);
SET @p2 = (SELECT id FROM product WHERE name = '戴森吹风机 HD15' LIMIT 1);
SET @p3 = (SELECT id FROM product WHERE name = '《人类简史》新版' LIMIT 1);

INSERT INTO orders(order_no, buyer_id, product_id, buy_count, selected_spec, coupon_id, coupon_title, discount_amount, total_amount, order_status, payment_method, pay_time, pay_txn_no, create_time) VALUES
('JM202605090001', @u1, @p1, 1, '午夜色/128G', NULL, NULL, NULL, 3899.00, 1, 'ALIPAY', NOW(), 'TXNJM0001', NOW()),
('JM202605090002', @u2, @p2, 1, '紫红色/标准版', NULL, NULL, NULL, 2799.00, 2, 'WECHAT', DATE_SUB(NOW(), INTERVAL 1 DAY), 'TXNJM0002', DATE_SUB(NOW(), INTERVAL 1 DAY)),
('JM202605090003', @u3, @p3, 1, '平装', NULL, NULL, NULL, 58.00, 3, 'ALIPAY', DATE_SUB(NOW(), INTERVAL 3 DAY), 'TXNJM0003', DATE_SUB(NOW(), INTERVAL 3 DAY)),
('JM202605090004', @u4, @p1, 1, '蓝色/128G', NULL, NULL, NULL, 3899.00, 6, 'ALIPAY', DATE_SUB(NOW(), INTERVAL 2 DAY), 'TXNJM0004', DATE_SUB(NOW(), INTERVAL 2 DAY));

INSERT INTO order_notice(order_id, user_id, scope, notice_type, is_read, create_time)
SELECT o.id, o.buyer_id, 'BUYER', 'PAY_PENDING', 0, o.create_time FROM orders o WHERE o.order_no = 'JM202605090001'
UNION ALL
SELECT o.id, p.seller_id, 'SELLER', 'NEW_ORDER', 0, o.create_time
FROM orders o JOIN product p ON o.product_id = p.id WHERE o.order_no = 'JM202605090001';

INSERT INTO admin_notification(order_id, sender_id, preview, is_read, create_time)
SELECT o.id, o.buyer_id, '用户申请退款：订单 JM202605090004，原因：商品与描述不符。', 0, NOW()
FROM orders o WHERE o.order_no = 'JM202605090004';

SET FOREIGN_KEY_CHECKS = 1;

-- 测试账号（统一密码：123456）
-- SUPER_ADMIN / ops_admin / merch_admin / buyer_chen / buyer_lin / buyer_wang / buyer_sun
