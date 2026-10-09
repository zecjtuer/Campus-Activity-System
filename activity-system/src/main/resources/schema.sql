-- ============================================================
-- 校园活动管理系统 V2.0 建库建表脚本 (MySQL 8)
-- 勇闯天涯组
-- V2.0 变更点:
--   1. registration.status 扩展为 4 态: ENROLLED/WAITING/CANCELLED/INVALID
--      新增 wait_order (候补序号, 可空)
--   2. activity 新增 target_grade (允许报名年级, 可空=不限, 如 "2024,2025")
--   3. sys_user.role 新增 ADMIN 取值; status 0=停用 1=启用 (V1.0 已有)
--   4. 新增 notifications 表: 站内通知 (5 类事件自动写入)
-- ============================================================

CREATE DATABASE IF NOT EXISTS campus_activity
  DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE campus_activity;

-- ------------------------------------------------------------
-- 清表 (顺序: 先删子表再删父表)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS notifications;
DROP TABLE IF EXISTS registration;
DROP TABLE IF EXISTS activity;
DROP TABLE IF EXISTS sys_user;

-- ------------------------------------------------------------
-- 1) 用户表：学生 / 教师 / 管理员共用, role 区分
--    V2.0: role 新增 ADMIN; status 0=停用 1=启用
-- ------------------------------------------------------------
CREATE TABLE sys_user (
  account  VARCHAR(20)  NOT NULL COMMENT '登录账号（学号/工号）',
  password VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
  name     VARCHAR(30)  NOT NULL COMMENT '姓名',
  role     VARCHAR(10)  NOT NULL COMMENT '角色：STUDENT/TEACHER/ADMIN',
  user_no  VARCHAR(20)  NOT NULL COMMENT '学号/工号 (与 account 一致, 用于业务展示)',
  status   TINYINT      NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  PRIMARY KEY (account),
  UNIQUE KEY uk_user_no (user_no),
  CONSTRAINT chk_role   CHECK (role IN ('STUDENT','TEACHER','ADMIN')),
  CONSTRAINT chk_status CHECK (status IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ------------------------------------------------------------
-- 2) 活动表
--    V2.0 新增 target_grade: 允许报名的年级, 逗号分隔, 空=不限
--    删除走逻辑删除 (deleted=1)
-- ------------------------------------------------------------
CREATE TABLE activity (
  activity_id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '活动ID',
  title            VARCHAR(100) NOT NULL COMMENT '活动名称',
  activity_time    DATETIME     NOT NULL COMMENT '活动举办时间',
  location         VARCHAR(100) NOT NULL COMMENT '地点',
  description      VARCHAR(500) DEFAULT NULL COMMENT '活动简介',
  max_participants INT          NOT NULL COMMENT '报名人数上限',
  signup_deadline  DATETIME     NOT NULL COMMENT '报名截止时间',
  teacher_account  VARCHAR(20)  NOT NULL COMMENT '发布教师账号',
  target_grade     VARCHAR(100) DEFAULT NULL COMMENT '允许报名年级, 逗号分隔如 2024,2025; 空=不限',
  publish_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发布时间',
  deleted          TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除：0正常 1已删除',
  PRIMARY KEY (activity_id),
  KEY idx_teacher (teacher_account),
  KEY idx_publish_time (publish_time),
  CONSTRAINT fk_act_teacher FOREIGN KEY (teacher_account)
    REFERENCES sys_user(account),
  CONSTRAINT chk_max CHECK (max_participants > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动表';

-- ------------------------------------------------------------
-- 3) 报名记录表
--    V2.0: status 四态 (ENROLLED/WAITING/CANCELLED/INVALID)
--          新增 wait_order 候补序号
--    退选不删记录, 状态置 CANCELLED; 活动删除时批量置 INVALID
-- ------------------------------------------------------------
CREATE TABLE registration (
  record_id       BIGINT      NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  student_account VARCHAR(20) NOT NULL COMMENT '学生账号',
  activity_id     BIGINT      NOT NULL COMMENT '活动ID',
  status          VARCHAR(10) NOT NULL DEFAULT 'ENROLLED'
                  COMMENT 'ENROLLED已报名/WAITING候补中/CANCELLED已退选/INVALID已失效',
  wait_order      INT         DEFAULT NULL COMMENT '候补序号 (WAITING 状态下分配, 递补后置空)',
  signup_time     DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报名时间',
  PRIMARY KEY (record_id),
  KEY idx_student (student_account),
  KEY idx_activity (activity_id),
  KEY idx_status_activity_wait (activity_id, status, wait_order),
  CONSTRAINT fk_reg_student FOREIGN KEY (student_account)
    REFERENCES sys_user(account),
  CONSTRAINT fk_reg_activity FOREIGN KEY (activity_id)
    REFERENCES activity(activity_id),
  CONSTRAINT chk_reg_status CHECK (status IN ('ENROLLED','WAITING','CANCELLED','INVALID'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报名记录表';

-- ------------------------------------------------------------
-- 关键约束：同一学生同一活动只能有一条"有效"记录
-- V2.0: 有效状态扩展为 ENROLLED 和 WAITING (候补也算占用名额)
-- 退选(CANCELLED)或失效(INVALID)后可再次报名
-- 函数唯一索引：仅对 ENROLLED/WAITING 行生效
-- ------------------------------------------------------------
CREATE UNIQUE INDEX uk_active_reg
  ON registration ((CASE WHEN status IN ('ENROLLED','WAITING')
                         THEN CONCAT(student_account,'#',activity_id) END));

-- ------------------------------------------------------------
-- 4) 通知表 (V2.0 新增)
--    5 类事件自动写入: 报名成功/取消报名/候补递补/活动取消/活动信息变更
--    学生收到状态通知, 教师收到本人活动名单变动通知
-- ------------------------------------------------------------
CREATE TABLE notifications (
  id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  receiver_account VARCHAR(20)  NOT NULL COMMENT '接收人账号',
  receiver_role    VARCHAR(10)  NOT NULL COMMENT '接收人角色: STUDENT/TEACHER/ADMIN',
  activity_id      BIGINT       DEFAULT NULL COMMENT '关联活动ID (可空, 便于联表)',
  type             VARCHAR(20)  NOT NULL COMMENT '通知类型: ENROLLED/CANCELLED/PROMOTED/ACTIVITY_CANCELLED/ACTIVITY_UPDATED',
  content          VARCHAR(500) NOT NULL COMMENT '通知内容',
  is_read          TINYINT      NOT NULL DEFAULT 0 COMMENT '0未读 1已读',
  created_at       DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY (id),
  KEY idx_receiver (receiver_account, is_read),
  KEY idx_activity (activity_id),
  CONSTRAINT chk_noti_role CHECK (receiver_role IN ('STUDENT','TEACHER','ADMIN')),
  CONSTRAINT chk_noti_type CHECK (type IN ('ENROLLED','CANCELLED','PROMOTED','ACTIVITY_CANCELLED','ACTIVITY_UPDATED')),
  CONSTRAINT chk_noti_read CHECK (is_read IN (0,1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内通知表';
