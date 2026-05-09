-- 迁移 v11：修复“超级管理员”账号角色
-- 执行：
-- mysql -uroot -p123456 my_system < database/migration-v11-super-admin-role-fix.sql

UPDATE sys_user
SET role = 'SUPER_ADMIN'
WHERE nickname = '超级管理员'
   OR username = 'super_admin';

-- 如果你的历史初始账号是 admin 且昵称是超级管理员，也一并修正
UPDATE sys_user
SET role = 'SUPER_ADMIN'
WHERE username = 'admin'
  AND nickname = '超级管理员';
