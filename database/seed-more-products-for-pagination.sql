-- 补充商品测试数据（分页专项）
-- 用途：快速生成 24 个在售商品，方便测试商城每页 8 / 12 条分页
-- 执行：
-- mysql -uroot -p123456 my_system < database/seed-more-products-for-pagination.sql

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 选一个管理员作为卖家（优先 merch_admin）
SET @seller := (
  SELECT id FROM sys_user
  WHERE username IN ('merch_admin','ops_admin','SUPER_ADMIN')
  ORDER BY FIELD(username,'merch_admin','ops_admin','SUPER_ADMIN')
  LIMIT 1
);

-- 清理旧的分页测试商品（可重复执行）
DELETE FROM product WHERE name LIKE '【分页测试】%';

INSERT INTO product
  (name, description, category, price, original_price, image, image_url, stock, seller_id, spec_json, is_seckill, seckill_price, seckill_start_time, seckill_end_time, audit_status, publish_status, create_time)
VALUES
  ('【分页测试】蓝牙耳机 A1', '分页测试样例：续航 24 小时，支持通话降噪。', '数码电子', 129.00, 199.00, NULL, NULL, 30, @seller, '{"colors":["黑色","白色"],"sizes":["标准"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】蓝牙耳机 A2', '分页测试样例：半入耳设计，低延迟模式。', '数码电子', 149.00, 219.00, NULL, NULL, 28, @seller, '{"colors":["深空灰","奶油白"],"sizes":["标准"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】机械键盘 K1', '分页测试样例：87 键，热插拔轴体。', '数码电子', 239.00, 329.00, NULL, NULL, 20, @seller, '{"colors":["黑色"],"sizes":["87键"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】机械键盘 K2', '分页测试样例：RGB 背光，PBT 键帽。', '数码电子', 289.00, 389.00, NULL, NULL, 18, @seller, '{"colors":["白色"],"sizes":["98键"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】显示器支架 M1', '分页测试样例：铝合金材质，稳固承重。', '生活用品', 99.00, 139.00, NULL, NULL, 40, @seller, '{"colors":["银色"],"sizes":["单臂"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】显示器支架 M2', '分页测试样例：双屏扩展，走线隐藏。', '生活用品', 159.00, 229.00, NULL, NULL, 35, @seller, '{"colors":["黑色"],"sizes":["双臂"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】运动短袖 T1', '分页测试样例：速干透气，轻量面料。', '服饰鞋帽', 59.00, 89.00, NULL, NULL, 50, @seller, '{"colors":["白色","蓝色"],"sizes":["M","L","XL"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】运动短袖 T2', '分页测试样例：宽松版型，吸湿排汗。', '服饰鞋帽', 69.00, 99.00, NULL, NULL, 45, @seller, '{"colors":["黑色","灰色"],"sizes":["M","L","XL"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】休闲卫衣 H1', '分页测试样例：加绒保暖，春秋可穿。', '服饰鞋帽', 139.00, 199.00, NULL, NULL, 32, @seller, '{"colors":["燕麦色","深灰"],"sizes":["L","XL","2XL"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】休闲卫衣 H2', '分页测试样例：连帽抽绳，日常百搭。', '服饰鞋帽', 149.00, 219.00, NULL, NULL, 30, @seller, '{"colors":["藏青","黑色"],"sizes":["M","L","XL"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】不粘锅 P1', '分页测试样例：麦饭石涂层，少油烟。', '家用电器', 119.00, 169.00, NULL, NULL, 36, @seller, '{"colors":["灰色"],"sizes":["28cm"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】电热水壶 E1', '分页测试样例：304 不锈钢内胆，自动断电。', '家用电器', 89.00, 129.00, NULL, NULL, 44, @seller, '{"colors":["白色"],"sizes":["1.7L"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】收纳箱 B1', '分页测试样例：可叠放设计，防尘防潮。', '生活用品', 39.00, 59.00, NULL, NULL, 70, @seller, '{"colors":["透明","米白"],"sizes":["45L"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】收纳箱 B2', '分页测试样例：滑轮款，搬运更省力。', '生活用品', 49.00, 69.00, NULL, NULL, 64, @seller, '{"colors":["透明"],"sizes":["60L"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】阅读台灯 L1', '分页测试样例：三档调光，护眼无频闪。', '生活用品', 79.00, 119.00, NULL, NULL, 42, @seller, '{"colors":["白色"],"sizes":["标准"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】阅读台灯 L2', '分页测试样例：触控调光，支持夜灯模式。', '生活用品', 99.00, 149.00, NULL, NULL, 38, @seller, '{"colors":["黑色","白色"],"sizes":["标准"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】双肩背包 BPK1', '分页测试样例：防泼水面料，电脑夹层。', '服饰鞋帽', 109.00, 159.00, NULL, NULL, 26, @seller, '{"colors":["黑色","灰色"],"sizes":["20L"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】双肩背包 BPK2', '分页测试样例：通勤轻便，背负舒适。', '服饰鞋帽', 119.00, 179.00, NULL, NULL, 24, @seller, '{"colors":["藏蓝","卡其"],"sizes":["22L"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】数据线 C1', '分页测试样例：快充协议，耐弯折。', '数码电子', 29.00, 49.00, NULL, NULL, 120, @seller, '{"colors":["黑色","白色"],"sizes":["1m","2m"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】数据线 C2', '分页测试样例：编织外被，抗拉扯。', '数码电子', 35.00, 55.00, NULL, NULL, 110, @seller, '{"colors":["黑色"],"sizes":["1m","2m"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】保温杯 W1', '分页测试样例：316L 内胆，保温 12 小时。', '生活用品', 69.00, 99.00, NULL, NULL, 48, @seller, '{"colors":["银色","粉色"],"sizes":["480ml"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】保温杯 W2', '分页测试样例：一键弹盖，防漏设计。', '生活用品', 79.00, 109.00, NULL, NULL, 46, @seller, '{"colors":["黑色","蓝色"],"sizes":["520ml"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】旅行收纳袋 S1', '分页测试样例：六件套，分区整理。', '生活用品', 45.00, 69.00, NULL, NULL, 66, @seller, '{"colors":["灰色","米色"],"sizes":["六件套"]}', 0, NULL, NULL, NULL, 1, 1, NOW()),
  ('【分页测试】旅行收纳袋 S2', '分页测试样例：轻薄防水，可机洗。', '生活用品', 55.00, 79.00, NULL, NULL, 58, @seller, '{"colors":["灰色"],"sizes":["七件套"]}', 0, NULL, NULL, NULL, 1, 1, NOW());

SET FOREIGN_KEY_CHECKS = 1;
