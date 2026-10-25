# 智能学生客服系统

> Java SE 课程设计 · 主题2
> 技术栈：**Java SE 8 + Swing + JDBC + MySQL 8 + LLM(OpenAI 兼容接口)**
> 纯 JDK 实现，不引入 Spring / MyBatis / Lombok / 任何第三方 UI 库。

一个面向高校场景的智能客服工单系统：学生先用 AI 自助问答，AI 答不了再转人工工单，
客服处理、管理员看统计，全流程有状态机和操作日志。

---

## ✨ 功能特性

| 模块 | 说明 |
|---|---|
| **用户与权限** | 注册 / 登录 / 三种角色（学生、客服、管理员），账号可启用禁用 |
| **AI 智能问答** | 双层链路：先匹配本地 FAQ（2/3-gram 滑窗分词 + LIKE），未命中再调 LLM |
| **情感分析** | 工单创建时调 LLM 判情绪，负面情绪自动把优先级升为 `urgent` |
| **工单流转** | `open → processing → pending_customer → closed` 状态机，每步写操作日志 |
| **高级查询** | 多条件动态拼 SQL（标题模糊 / 类别 / 状态 / 优先级）+ 分页 |
| **数据统计** | 4 个 GROUP BY 聚合 + AVG(TIMESTAMPDIFF) 平均响应时长 |
| **图形界面** | Swing 四窗口（登录 / 学生 / 客服 / 管理员），AI 调用走 SwingWorker 不卡界面 |

---

## 🗂 项目结构

```
.
├── src/com/example/studentservice/
│   ├── Main.java                 # 程序入口（安装主题 + 测试数据库 + 启动登录窗）
│   ├── Global.java               # Service 单例 + 当前登录用户
│   ├── entity/                   # 5 个实体类
│   ├── dao/  + dao/impl/         # 数据访问层（接口 + 实现）
│   ├── service/ + service/impl/  # 业务层（状态机、权限校验、AI 链路）
│   ├── util/                     # DBUtil / HttpUtil / AIUtil
│   └── ui/                       # Swing 界面层
│       ├── UITheme.java          # 统一配色/字号/组件样式
│       ├── RoundedButton.java    # 自绘圆角按钮
│       ├── RoundedPanel.java     # 自绘圆角卡片
│       ├── ChatPanel.java        # 气泡聊天面板
│       ├── LoginFrame.java       # 登录 / 注册（CardLayout 切换）
│       ├── StudentFrame.java     # 学生工作台
│       ├── CsFrame.java          # 客服工作台
│       └── AdminFrame.java       # 管理员工作台
├── resources/
│   ├── schema.sql                # 【事实源】建表 + 种子数据
│   └── db.properties.example     # 配置模板（复制成 db.properties 后填写）
├── lib/                          # 需自行放入 mysql-connector-j-8.0.33.jar
├── dselftest/                    # 自检脚本（AI 链路 / 功能审计 / UI 截图）
├── prompts.md                    # 全组共享 AI 提示词 + AI 错误档案
├── 组员对接指南.md               # 任务分工与验收清单
└── 项目验收评分报告.md           # 代码审计与评分
```

---

## 🚀 快速开始

### 1. 初始化数据库

```bash
mysql -u root -p --default-character-set=utf8mb4 < resources/schema.sql
```

脚本会创建 `student_cs_db` 库、5 张表和种子数据（5 个账号 / 6 条 FAQ / 3 条工单）。

### 2. 配置连接与 API Key

```bash
cp resources/db.properties.example resources/db.properties
```

然后编辑 `resources/db.properties`：
- `jdbc.password` 改成你本机 MySQL 的密码
- `llm.api.key` 填你的 API Key（任意 OpenAI 兼容接口均可，默认阿里云百炼千问）

> `db.properties` 已在 `.gitignore` 里，不会被提交。

### 3. 放 MySQL 驱动

`lib/` 下已附带 `mysql-connector-j-8.0.33.jar`。如果缺失，自行下载后放到 `lib/`，
并在 IDE 里把它加到模块依赖：

```
File → Project Structure → Modules → Dependencies → + → JARs or directories → 选 lib 下的 jar
```

同时把 `resources` 标记为 **Resources Root**（否则读不到 `db.properties`）。

### 4. 运行

运行 `com.example.studentservice.Main`，弹出登录窗口。

| 角色 | 账号 | 密码 |
|---|---|---|
| 管理员 | `admin01` | `123456` |
| 客服 | `cs001` | `123456` |
| 学生 | `student01` | `123456` |

---

## 🗄 数据库设计

| 表 | 说明 |
|---|---|
| `user` | 用户（角色 student/cs/admin，status 1 启用 0 禁用） |
| `faq` | 常见问题（keywords 逗号分隔，供 n-gram 匹配） |
| `ticket` | 工单（状态机 + 优先级 + 情绪 + 处理人） |
| `conversation` | AI 对话记录（来源 faq/llm/manual + 置信度 + 是否转人工） |
| `ticket_log` | 工单操作日志（create/assign/process/reply/close/reopen/update_priority） |

外键：`ticket.creator_id → user.id`、`conversation.ticket_id → ticket.id`（级联删除）等。

---

## 🧠 AI 链路设计

```
学生提问
  ├─ 1. FaqService.matchQuestion()
  │      完整句子 LIKE → 未命中 → 2/3-gram 滑窗分词逐个 LIKE
  │      命中 → 直接返回 FAQ 答案 (source=faq, confidence=0.95)
  │
  └─ 2. 未命中 → AIUtil.generateAnswer() 调 LLM
         ├─ 构造 OpenAI 兼容 JSON body
         ├─ HttpUtil.doPostJson (Bearer 鉴权 + 5s/15s 超时)
         ├─ 逐字符扫描解析 content（处理转义引号 + ```json 围栏）
         └─ 返回 AnswerResult(answer, needHuman, confidence, source=llm)
```

**为什么要双层？** 本地 FAQ 零成本零延迟、答案可控；LLM 兜底覆盖长尾问题；
LLM 判 `need_human=true` 时引导用户转人工，避免编造答案。

---

## 👥 模块分工

| 成员 | 负责模块 |
|---|---|
| A | 架构设计、Swing UI 层、状态机、统计聚合 |
| B | 用户模块、FAQ 模块（CRUD + n-gram 匹配） |
| C | 工单模块（动态查询、JOIN、GROUP BY） |
| D | AI 问答、情感分析、Prompt 调优、HTTP 调用 |

---

## ❓ 常见问题

**`ClassNotFoundException: com.mysql.cj.jdbc.Driver`**
驱动 jar 没加到模块依赖里，见上面「3. 放 MySQL 驱动」。

**`Error 1452: Cannot add or update a child row`**
种子数据外键不匹配，重新执行最新的 `resources/schema.sql`（脚本开头会 DROP 重建）。

**`Error 1045: Access denied`**
`db.properties` 里的 `jdbc.password` 不对。

**AI 问答一直返回「这个问题我转交人工客服」**
`llm.api.key` 没填或填错，看控制台的 `[AIUtil] LLM 接口返回错误` 日志。

**管理员把自己账号禁用了，登不进去**
```sql
UPDATE user SET status = 1 WHERE username = 'admin01';
```
（新版本已禁止管理员禁用自己，并保证禁用账号仍出现在用户管理列表里可恢复）

---

## 📄 License

课程作业项目，仅用于学习交流。
