-- ============================================================
-- 智能学生客服系统 · 数据库初始化脚本
-- 数据库: MySQL 8.0   字符集: utf8mb4
-- 组长 A 维护 · 任何人改字段都必须先改此文件
-- ============================================================

CREATE DATABASE IF NOT EXISTS student_cs_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE student_cs_db;

-- ---------- 1. 用户表 user ----------
DROP TABLE IF EXISTS ticket_log;
DROP TABLE IF EXISTS conversation;
DROP TABLE IF EXISTS ticket;
DROP TABLE IF EXISTS faq;
DROP TABLE IF EXISTS user;

CREATE TABLE user (
                      id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '用户ID',
                      username        VARCHAR(50)     NOT NULL                COMMENT '登录账号(唯一)',
                      password        VARCHAR(100)    NOT NULL                COMMENT '密码(MD5或明文, 作业用)',
                      real_name       VARCHAR(50)     DEFAULT NULL            COMMENT '真实姓名',
                      role            VARCHAR(20)     NOT NULL                COMMENT '角色: student(学生) / cs(客服) / admin(管理员)',
                      email           VARCHAR(100)    DEFAULT NULL            COMMENT '邮箱',
                      phone           VARCHAR(20)     DEFAULT NULL            COMMENT '手机号',
                      status          TINYINT         NOT NULL DEFAULT 1      COMMENT '状态: 1启用 0禁用',
                      create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                      update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                      PRIMARY KEY (id),
                      UNIQUE KEY uk_username (username),
                      KEY idx_role (role)
) ENGINE=InnoDB COMMENT='用户表';


-- ---------- 2. 常见问题表 faq ----------
CREATE TABLE faq (
                     id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT 'FAQ ID',
                     question        VARCHAR(300)    NOT NULL                COMMENT '问题',
                     answer          TEXT            NOT NULL                COMMENT '标准答案',
                     category        VARCHAR(50)     DEFAULT NULL            COMMENT '分类: 选课/宿舍/图书馆/缴费/其他',
                     keywords        VARCHAR(200)    DEFAULT NULL            COMMENT '搜索关键词(逗号分隔), 用于LIKE匹配',
                     hit_count       INT             NOT NULL DEFAULT 0      COMMENT '被命中次数(统计用)',
                     status          TINYINT         NOT NULL DEFAULT 1      COMMENT '状态: 1启用 0禁用',
                     create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                     update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                     PRIMARY KEY (id),
                     KEY idx_category (category),
                     KEY idx_hit_count (hit_count)
) ENGINE=InnoDB COMMENT='常见问题FAQ表';


-- ---------- 3. 工单表 ticket ----------
CREATE TABLE ticket (
                        id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '工单ID',
                        title           VARCHAR(200)    NOT NULL                COMMENT '工单标题',
                        content         TEXT            DEFAULT NULL            COMMENT '详细描述',
                        category        VARCHAR(50)     DEFAULT NULL            COMMENT '类别: 选课/宿舍/图书馆/缴费/其他',
                        status          VARCHAR(20)     NOT NULL DEFAULT 'open' COMMENT '状态: open(待处理)/processing(处理中)/pending_customer(待用户)/closed(已关闭)',
                        priority        VARCHAR(20)     NOT NULL DEFAULT 'medium' COMMENT '优先级: low/medium/high/urgent',
                        emotion         VARCHAR(20)     NOT NULL DEFAULT 'neutral' COMMENT '情绪: positive/neutral/negative',
                        creator_id      BIGINT          NOT NULL                COMMENT '创建者(学生) user.id',
                        assignee_id     BIGINT          DEFAULT NULL            COMMENT '处理人(客服) user.id',
                        faq_hit_id      BIGINT          DEFAULT NULL            COMMENT 'AI首次命中的FAQ id, 可空',
                        create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        update_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        close_time      DATETIME        DEFAULT NULL            COMMENT '关闭时间, 用于计算响应时长',
                        PRIMARY KEY (id),
                        KEY idx_creator (creator_id),
                        KEY idx_assignee (assignee_id),
                        KEY idx_status (status),
                        KEY idx_priority (priority),
                        KEY idx_create_time (create_time),
                        KEY idx_creator_status (creator_id, status),
                        CONSTRAINT fk_ticket_creator  FOREIGN KEY (creator_id)  REFERENCES user(id) ON DELETE RESTRICT,
                        CONSTRAINT fk_ticket_assignee FOREIGN KEY (assignee_id) REFERENCES user(id) ON DELETE SET NULL,
                        CONSTRAINT fk_ticket_faq      FOREIGN KEY (faq_hit_id)  REFERENCES faq(id)     ON DELETE SET NULL
) ENGINE=InnoDB COMMENT='工单表';


-- ---------- 4. AI 对话记录表 conversation ----------
CREATE TABLE conversation (
                              id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '对话ID',
                              ticket_id       BIGINT          DEFAULT NULL            COMMENT '关联工单ID, 自助问答时可空',
                              user_id         BIGINT          NOT NULL                COMMENT '发起用户 user.id',
                              role            VARCHAR(20)     NOT NULL                COMMENT '消息角色: user(学生提问) / assistant(AI/客服回答)',
                              content         TEXT            NOT NULL                COMMENT '消息内容',
                              source          VARCHAR(20)     DEFAULT NULL            COMMENT '回答来源: faq(FAQ命中)/llm(LLM生成)/manual(人工)',
                              confidence      DECIMAL(3,2)    DEFAULT NULL            COMMENT 'AI回答置信度 0.00~1.00',
                              need_human      TINYINT         NOT NULL DEFAULT 0      COMMENT '是否转人工: 1是 0否',
                              create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              PRIMARY KEY (id),
                              KEY idx_ticket (ticket_id),
                              KEY idx_user (user_id),
                              KEY idx_ticket_time (ticket_id, create_time),
                              CONSTRAINT fk_conv_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(id) ON DELETE CASCADE,
                              CONSTRAINT fk_conv_user   FOREIGN KEY (user_id)   REFERENCES user(id)   ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='AI对话/消息记录表';


-- ---------- 5. 工单日志表 ticket_log ----------
CREATE TABLE ticket_log (
                            id              BIGINT          NOT NULL AUTO_INCREMENT COMMENT '日志ID',
                            ticket_id       BIGINT          NOT NULL                COMMENT '工单ID',
                            operator_id     BIGINT          NOT NULL                COMMENT '操作人 user.id',
                            action          VARCHAR(50)     NOT NULL                COMMENT '动作: create/assign/process/reply/close/reopen/update_priority',
                            old_value       VARCHAR(500)    DEFAULT NULL            COMMENT '原值(JSON或简单字符串)',
                            new_value       VARCHAR(500)    DEFAULT NULL            COMMENT '新值',
                            remark          VARCHAR(500)    DEFAULT NULL            COMMENT '备注说明',
                            create_time     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            PRIMARY KEY (id),
                            KEY idx_ticket (ticket_id),
                            KEY idx_operator (operator_id),
                            KEY idx_ticket_time (ticket_id, create_time),
                            CONSTRAINT fk_log_ticket   FOREIGN KEY (ticket_id)   REFERENCES ticket(id) ON DELETE CASCADE,
                            CONSTRAINT fk_log_operator FOREIGN KEY (operator_id) REFERENCES user(id)   ON DELETE RESTRICT
) ENGINE=InnoDB COMMENT='工单操作日志表';


-- ============================================================
-- 初始化测试数据
-- ============================================================

-- 用户: 1 admin, 2 cs(客服), 3 student, 4 student
-- 密码统一 123456, 这里用简单明文(作业演示, 不做MD5)
INSERT INTO user (username, password, real_name, role, email, phone) VALUES
                                                                         ('admin01',  '123456', '系统管理员', 'admin',  'admin@school.edu',  '13800000000'),
                                                                         ('cs001',    '123456', '小李客服',   'cs',     'cs001@school.edu',  '13800000001'),
                                                                         ('student01','123456', '张三',       'student','zs@school.edu',      '13900000001'),
                                                                         ('student02','123456', '李四',       'student','ls@school.edu',      '13900000002'),
                                                                         ('cs002',    '123456', '小王客服',   'cs',     'cs002@school.edu',  '13800000002');

-- FAQ 种子数据
INSERT INTO faq (question, answer, category, keywords) VALUES
                                                           ('如何选课?',
                                                            '登录教务系统 → 选课中心 → 选择对应学期课程 → 点击"加入选课篮" → 确认提交。注意每学期有固定选课时间。',
                                                            '选课',
                                                            '选课,怎么选课,如何选课,选课流程'),
                                                           ('忘记密码怎么办?',
                                                            '可通过登录页"找回密码"，使用绑定邮箱或手机号重置。若仍不行，请携带学生证到教务处现场办理。',
                                                            '其他',
                                                            '密码,忘记密码,重置密码'),
                                                           ('图书馆开放时间?',
                                                            '图书馆周一至周日 8:00 - 22:00，法定节假日开放时间请关注图书馆公告。',
                                                            '图书馆',
                                                            '图书馆,开放时间,几点开门'),
                                                           ('宿舍怎么申请换寝?',
                                                            '向辅导员提出书面申请 → 辅导员审核 → 宿管中心审批 → 审批通过后统一安排，每学期统一办理一次。',
                                                            '宿舍',
                                                            '换寝,宿舍申请,调宿舍'),
                                                           ('学费如何缴纳?',
                                                            '登录学校缴费系统 (pay.school.edu) → 选择对应账单 → 支持微信/支付宝/银行卡支付。请在每学期开学前完成。',
                                                            '缴费',
                                                            '学费,缴费,交钱,怎么交学费'),
                                                           ('学生证丢失如何补办?',
                                                            '携带本人身份证到学生处 (行政楼201) 填写补办申请 → 缴纳工本费 → 约7个工作日后领取。',
                                                            '其他',
                                                            '学生证,补办,丢了');

-- 示例工单
-- ⚠️ 2026-10-25 补充: 显式给 create_time / close_time 赋值.
--    原来 closed 工单的 close_time 是 NULL, 导致 AVG(TIMESTAMPDIFF(close_time, create_time)) 算不出值,
--    统计面板的「平均响应时长」永远显示"暂无数据".
INSERT INTO ticket (title, content, category, status, priority, emotion, creator_id, assignee_id, faq_hit_id, create_time, close_time) VALUES
('选不上高数怎么办', '高数课容量已满，我选了三次都没选上，很着急', '选课', 'processing', 'high', 'negative', 3, 2, 1, NOW() - INTERVAL 2 DAY, NULL),
('宿舍漏水', '宿舍302的天花板漏水一周了没人修', '宿舍', 'open', 'urgent', 'negative', 4, NULL, NULL, NOW() - INTERVAL 1 DAY, NULL),
('图书馆续借问题', '上次借的书今天到期，请问能续借吗', '图书馆', 'closed', 'low', 'neutral', 3, 5, 3, NOW() - INTERVAL 3 DAY, NOW() - INTERVAL 3 DAY + INTERVAL 5 HOUR);

-- 示例对话
-- ⚠️ 2026-10-24 修复: assistant 行的 user_id 原来是 0, 违反 fk_conv_user 外键 (user 表没有 id=0),
--    导致整条 INSERT 报 Error 1452. 改成 3 (学生张三, 和代码里 AIServiceImpl 写 conversation 的行为一致).
INSERT INTO conversation (ticket_id, user_id, role, content, source, confidence, need_human) VALUES
                                                                                                 (1, 3, 'user',      '选不上高数怎么办',                    NULL,  NULL, 0),
                                                                                                 (1, 3, 'assistant', '登录教务系统 → 选课中心 → 选择...',    'faq', 0.95, 0),
                                                                                                 (NULL, 3, 'user',    '图书馆几点开门?',                     NULL,  NULL, 0),
                                                                                                 (NULL, 3, 'assistant','图书馆周一至周日 8:00 - 22:00...',   'faq', 0.92, 0);

-- 示例日志
INSERT INTO ticket_log (ticket_id, operator_id, action, old_value, new_value, remark) VALUES
                                                                                          (1, 1, 'create',  NULL,    NULL,          '学生张三创建工单'),
                                                                                          (1, 1, 'assign',   NULL,    '2',           '管理员分配给客服小李'),
                                                                                          (1, 2, 'process',  'open',  'processing',  '客服小李开始处理'),
                                                                                          (2, 4, 'create',   NULL,    NULL,          '学生李四创建工单'),
                                                                                          (3, 1, 'close',    'processing', 'closed', '工单已解决并关闭');
