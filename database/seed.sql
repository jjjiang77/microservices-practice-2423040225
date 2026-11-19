-- 商小淘测试数据（可重复执行：INSERT IGNORE + 固定主键，重复执行不会报错、不会产生重复数据）
-- 用法: mysql -u root -p < sql/data.sql
-- 显式声明导入字符集，避免容器内 mysql 客户端默认 latin1 造成中文乱码
SET NAMES utf8mb4;

USE shangxiaotao_user;

-- 测试账号密码（库里只存 BCrypt 哈希，明文密码仅记录在 docs/security.md）：
--   2423040225 / Student@123 （STUDENT）
--   2423040999 / Student@123 （STUDENT）
--   2423040001 / Student@123 （STUDENT，低信用）
--   admin      / Admin@123   （ADMIN）
INSERT IGNORE INTO t_user (id, student_id, nickname, department, role, password_hash, credit_score, created_at, updated_at)
VALUES (1, '2423040225', '小商同学', '计算机科学与技术学院', 'STUDENT', '$2y$10$rKtFxL9LdIL2QMh/3Wb3zOgEIjfMyGo6hCHOfAGrXdqzDm/1XXIqa', 100, NOW(6), NOW(6)),
       (2, '2423040999', '淘淘买家', '统计与数学学院', 'STUDENT', '$2y$10$rKtFxL9LdIL2QMh/3Wb3zOgEIjfMyGo6hCHOfAGrXdqzDm/1XXIqa', 88, NOW(6), NOW(6)),
       (3, '2423040001', '低信用同学', '环境科学与工程学院', 'STUDENT', '$2y$10$rKtFxL9LdIL2QMh/3Wb3zOgEIjfMyGo6hCHOfAGrXdqzDm/1XXIqa', 40, NOW(6), NOW(6)),
       (4, 'admin', '平台管理员', NULL, 'ADMIN', '$2y$10$bbphrNsujMnD1T2zNe/fsub.E2Do/0y4PhKSVmSFYyj4ylD8visHC', 100, NOW(6), NOW(6));

USE shangxiaotao_product;

INSERT IGNORE INTO t_product (id, title, description, price, category, condition_level, status, seller_id, seller_nickname, created_at,
                              updated_at)
VALUES (1, '考研数学复习全书（九成新）', '校内面交，西区宿舍楼下', 45.00, 'TEXTBOOK', 'LIKE_NEW', 'ON_SALE', 1, '小商同学', NOW(6),
        NOW(6)),
       (2, '宿舍小冰箱', '毕业清仓，支持自提', 300.00, 'LARGE_ITEM', 'GOOD', 'PENDING', 1, '小商同学', NOW(6), NOW(6));
