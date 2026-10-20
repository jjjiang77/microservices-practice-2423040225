-- 商小淘测试数据（可重复执行：INSERT IGNORE + 固定主键，重复执行不会报错、不会产生重复数据）
-- 用法: mysql -u root -p < sql/data.sql
-- 显式声明导入字符集，避免容器内 mysql 客户端默认 latin1 造成中文乱码
SET NAMES utf8mb4;

USE shangxiaotao;

INSERT IGNORE INTO t_user (id, student_id, nickname, department, role, credit_score, created_at, updated_at)
VALUES (1, '2423040225', '小商同学', '计算机科学与技术学院', 'STUDENT', 100, NOW(6), NOW(6)),
       (2, '2423040999', '淘淘买家', '统计与数学学院', 'STUDENT', 88, NOW(6), NOW(6)),
       (3, '2423040001', '低信用同学', '环境科学与工程学院', 'STUDENT', 40, NOW(6), NOW(6));

INSERT IGNORE INTO t_product (id, title, description, price, category, condition_level, status, seller_id, created_at,
                              updated_at)
VALUES (1, '考研数学复习全书（九成新）', '校内面交，西区宿舍楼下', 45.00, 'TEXTBOOK', 'LIKE_NEW', 'ON_SALE', 1, NOW(6),
        NOW(6)),
       (2, '宿舍小冰箱', '毕业清仓，支持自提', 300.00, 'LARGE_ITEM', 'GOOD', 'PENDING', 1, NOW(6), NOW(6));
