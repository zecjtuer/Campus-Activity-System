-- ============================================================
-- 校园活动管理系统 V2.0 预置账号脚本 (MySQL 8)
-- V2.0 变更: 新增管理员账号 admin/123456 (role=ADMIN)
-- 所有账号初始密码均为 123456 (BCrypt 加密值)
-- 教师 2 名, 学生 6 名, 管理员 1 名
-- ============================================================
USE campus_activity;

-- ------------------------------------------------------------
-- 用户数据
-- ------------------------------------------------------------
INSERT INTO sys_user (account, password, name, role, user_no, status) VALUES
('T2024001', '$2a$10$gj9wEq4zHNkWgLMBva72Re6LlOTkuizNDJcxzAvhbEklYyLUJY.dK', '张老师', 'TEACHER', 'T2024001', 1),
('T2024002', '$2a$10$gj9wEq4zHNkWgLMBva72Re6LlOTkuizNDJcxzAvhbEklYyLUJY.dK', '李老师', 'TEACHER', 'T2024002', 1),
('S2024001', '$2a$10$gj9wEq4zHNkWgLMBva72Re6LlOTkuizNDJcxzAvhbEklYyLUJY.dK', '王小明', 'STUDENT', 'S2024001', 1),
('S2024002', '$2a$10$gj9wEq4zHNkWgLMBva72Re6LlOTkuizNDJcxzAvhbEklYyLUJY.dK', '赵小红', 'STUDENT', 'S2024002', 1),
('S2024003', '$2a$10$gj9wEq4zHNkWgLMBva72Re6LlOTkuizNDJcxzAvhbEklYyLUJY.dK', '陈强',   'STUDENT', 'S2024003', 1),
('S2024004', '$2a$10$gj9wEq4zHNkWgLMBva72Re6LlOTkuizNDJcxzAvhbEklYyLUJY.dK', '刘芳',   'STUDENT', 'S2024004', 1),
('S2024005', '$2a$10$gj9wEq4zHNkWgLMBva72Re6LlOTkuizNDJcxzAvhbEklYyLUJY.dK', '孙伟',   'STUDENT', 'S2024005', 1),
('S2024006', '$2a$10$gj9wEq4zHNkWgLMBva72Re6LlOTkuizNDJcxzAvhbEklYyLUJY.dK', '周婷',   'STUDENT', 'S2024006', 1),
('admin',    '$2a$10$gj9wEq4zHNkWgLMBva72Re6LlOTkuizNDJcxzAvhbEklYyLUJY.dK', '系统管理员', 'ADMIN', 'admin', 1);

-- ------------------------------------------------------------
-- 示例活动数据 (V2.0: 含 target_grade 限定年级演示)
-- ------------------------------------------------------------
INSERT INTO activity
  (title, activity_time, location, description, max_participants, signup_deadline, teacher_account, target_grade)
VALUES
('校园三人制篮球赛',     '2026-10-20 14:00:00', '东区体育馆',   '面向全校学生, 三人制半场对抗, 每队3-4人。', 30, '2026-10-18 23:59:59', 'T2024001', NULL),
('新生编程马拉松',       '2026-10-25 09:00:00', '信息楼A301',  '12小时极限编程挑战赛, 主题现场公布。仅限 2024 级新生。', 20, '2026-10-23 18:00:00', 'T2024001', '2024'),
('图书馆志愿讲解招募',   '2026-09-30 13:30:00', '校图书馆大厅','面向 2024/2025 级的图书馆资源使用讲解志愿者招募。', 10, '2026-09-28 20:00:00', 'T2024002', '2024,2025'),
('中秋灯谜晚会',         '2026-09-25 19:00:00', '学生活动中心','猜灯谜、品月饼, 凭报名记录入场。',         60, '2026-09-24 12:00:00', 'T2024002', NULL),
('研究生学术沙龙',       '2026-11-15 14:00:00', '图书馆报告厅', '面向 2023/2024 级的研究生学术交流沙龙。',   15, '2026-11-13 18:00:00', 'T2024001', '2023,2024');
