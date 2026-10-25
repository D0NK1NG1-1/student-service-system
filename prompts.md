# 📝 智能学生客服系统 · 全组共享提示词 (prompts.md)

> 组长 A 维护 · 组员 B/C/D 每人都要用 AI 写代码时复用这里的模板
> 核心原则: **给角色、给约束、给上下文、给任务边界** — 否则 AI 大概率给你 MyBatis 或 Java 11 的代码

---

## 一、把 AI 当结对编程伙伴 (写 Java 代码时)

### 1.1 通用 Java 约束 (必须加到每次 AI 提问里)

```
角色: 你是一名资深 Java 工程师, 擅长 Java SE 8 + 原生 JDBC + 接口分层架构.

项目背景: 我在做一个【智能学生客服系统】, 技术约束如下:
- 纯 Java SE, 禁止用 MyBatis / JPA / Spring / Lombok;
- JDK 8 严格兼容 — 禁止 var / HttpClient.newHttpClient() / List.of() / """ 文本块;
- 所有 DAO / Service 必须先定义 interface, 再写 impl;
- 数据库 MySQL 8, 连接统一走 com.example.studentservice.util.DBUtil.getConnection();
- 第三方/LLM 调用统一走 com.example.studentservice.util.HttpUtil (doGet / doPostJson);
- 包结构: com.example.studentservice.{entity, dao, dao.impl, service, service.impl, util};

数据库 schema: (参考 resources/schema.sql)
- user (id, username, password, real_name, role[student/cs/admin], email, phone, status, create_time, update_time)
- ticket (id, title, content, category, status[open/processing/pending_customer/closed], priority[low/medium/high/urgent], emotion[positive/neutral/negative], creator_id, assignee_id, faq_hit_id, create_time, update_time, close_time)
- faq (id, question, answer, category, keywords, hit_count, status, create_time, update_time)
- conversation (id, ticket_id, user_id, role[user/assistant], content, source[faq/llm/manual], confidence, need_human, create_time)
- ticket_log (id, ticket_id, operator_id, action[create/assign/process/reply/close/reopen/update_priority], old_value, new_value, remark, create_time)
```

### 1.2 DAO 层模板 (组员 B/C 写实现类时)

```
任务: 请实现 XxxDao 接口的 Yyy 方法.
要求:
1. 用 PreparedStatement + ? 占位符 (防 SQL 注入);
2. 用 try-with-resources 关闭 Connection / PreparedStatement / ResultSet;
3. 只输出方法体, 不要改类名、不要新增 import 之外的类;
4. INSERT 方法要用 Statement.RETURN_GENERATED_KEYS 取自增 id;
5. 遇到 ResultSet 里的 null 字段, 用 DBUtil.rsStr(rs, "col") 或 DBUtil.rsLong(rs, "col");
6. SELECT 多条件查询, 动态拼 WHERE 子句, 所有 null 参数跳过;
7. 分页统一用 MySQL LIMIT offset, size 语法.
请先说明思路, 再给代码.
```

### 1.3 Service 层模板

```
任务: 请实现 XxxService 接口.
要求:
1. Service 只做业务校验 + 调 DAO, 不要写 SQL;
2. 注入 DAO 用 new XxxDaoImpl() (我们不引 Spring);
3. 状态变更要写日志 (调 TicketLogService.writeLog);
4. 情感分析调 AIUtil.analyzeSentiment(content);
5. 返回值用接口类型 (List<Ticket>, 不要 ArrayList<Ticket>);
请先说明业务逻辑, 再给代码.
```

---

## 二、应用内 LLM 提示词 (运行时给 AI 的 System Prompt)

### 2.1 AI 学生客服问答 (组员 D 用)

组长 A 已写在 AIUtil.SYSTEM_PROMPT_QA, D 组员直接用. 如果要改:

```
你是一个高校学生智能客服助手. 严格遵守:
1. 只回答校园业务 (选课/宿舍/图书馆/缴费/学生证/校园卡/食堂) 相关问题;
2. 如果问题不在知识范围内, 必须输出固定文案 "这个问题我转交人工客服",
   并把 need_human 设为 true, 严禁编造;
3. 输出必须是严格的 JSON, 不要任何多余文字或解释:
   {"answer": "字符串", "need_human": true或false, "confidence": 0.00到1.00}
4. 回答使用简体中文, 不超过 80 字;
5. confidence 表示你对这个回答的把握程度, 0.9 以上表示非常确定.
```

### 2.2 情感分析 (组员 D 用)

组长 A 已写在 AIUtil.SYSTEM_PROMPT_SENTIMENT. 如果要改:

```
你是一个情感分析助手. 给定一段用户提问, 判断其情绪.
严格只输出以下 JSON, 不要任何多余文字:
{"emotion": "positive" 或 "neutral" 或 "negative"}
判断规则:
- 负面: 抱怨/愤怒/着急/不满/求助语气强烈 (如"怎么办""急""没人管");
- 正面: 感谢/表扬/满意;
- 中性: 普通询问.
```

---

## 三、AI 代码审查清单 (拿到 AI 代码必过这 6 关)

| # | 审查维度      | 典型 AI 翻车信号                                            |
|---|---------------|-------------------------------------------------------------|
| 1 | API 真实性    | 调用了不存在的 StringUtils.xxx、DBUtil.batchInsert          |
| 2 | SQL 正确性    | 字段名/表名和 schema.sql 对不上、JOIN 条件写错              |
| 3 | 资源泄漏      | 没关 Connection/Statement/ResultSet, 或只关了一半          |
| 4 | 安全性        | 用字符串拼接 SQL, 没用 ? 占位符                             |
| 5 | 错误处理      | catch 里空处理或只 e.printStackTrace()                      |
| 6 | 版本错配      | 用 var / HttpClient / List.of (JDK 8 不支持)               |

---

## 四、AI 错误档案模板 (每个人都要写, 验收加分项)

| 日期       | AI 出错的代码 | 错在哪 (定位)                     | 怎么改 (修正)                                | 根因 (下次如何避免)          |
|------------|---------------|-----------------------------------|---------------------------------------------|-----------------------------|
| 2026-10-23 | var u = user; | JDK 10+ 语法, corretto-1.8 不支持 | 改成 User u = ...                            | 每次先给 AI 明确写 "JDK 8"  |
| 2026-10-23 | "SELECT ..."  | 字符串拼 SQL, 有注入风险          | 改成 PreparedStatement + ?                   | 强制加 "用占位符" 约束       |

> 组长 A 会在 Code Review 时重点审这个表, 每个人至少要有 3 条真实的 AI 错误记录.

---

## 五、🅳️ 组员 D 的 AI 错误档案 (2026-10-24 补充, 5 条真实案例)

> 以下 5 条都是 2026-10-24 本地跑自检时**实际复现**的问题, 不是编的.
> 复现方式: `javac` 编译 `dselftest/AIQuickCheck.java` + `python dselftest/mock_llm_server.py` 起本地模拟 LLM.

| # | AI 出错的代码 | 错在哪 (定位) | 怎么改 (修正) | 根因 (下次如何避免) |
|---|---|---|---|---|
| 1 | `AIUtil.extractContent` 正则 `"content"\s*:\s*"(.*?)"` | LLM 返回的 content 里嵌着 JSON (prompt 就要求它输出 JSON), 内部引号被转义成 `\"`; 非贪婪正则在**第一个 `\"` 就提前收尾**, 实测只抽出 `{\` 两个字符, 于是 answer / confidence 全丢, 所有回答都退化成"转人工 + confidence=0.0" | 改成逐字符扫描: 遇到 `\` 就连同下一个字符一起收, 只把**未转义的 `"`** 当作字符串结束 | LLM 返回是**嵌套 JSON**, 手写解析不能靠简单正则, 必须处理转义字符 |
| 2 | 解析前没处理 ` ```json ... ``` ` 代码围栏 | 模型经常把 JSON 包在 markdown 围栏里返回, 字段提取前多了围栏字符 | 新增 `stripCodeFence()`, 解析前先剥壳 | LLM 输出格式不可靠, 必须做**防御性解析** (prompts.md 原模板里就预判了这条) |
| 3 | 兜底逻辑"静默失败" | 千问 qwen-plus 对"我手机丢在操场了"只回了固定文案、没按 JSON 输出, raw 里没有 `answer` 字段, 程序静默降级成 confidence=0.0, 控制台完全看不出原因 | 加"解析失败就把原始返回打出来"的日志, 兜底默认 `needHuman=true` | 兜底必须**可观测**, 否则演示时只看到"转人工"却查不到为什么 |
| 4 | `HttpUtil` 非 2xx 时把 errorStream 当正常返回体 | API Key 无效 / 余额不足 / 限流时, 错误 JSON 被当成 content 继续往下解析, 静默失败 | `callLLM` 里检测 `"error"` 字段并打印原始错误 | 调外部接口必须**区分业务返回和错误返回**, 不能只看"有没有拿到字符串" |
| 5 | `AIUtil.java` 第 144 行注释出现乱码串字 | AI 生成注释时串了字 (`affluent naz Welcome美好舒适...`), 不影响编译但代码很脏, Code Review 会被抓 | 重写为规范 Javadoc | AI 生成的**注释也要逐行 review**, 不能因为"只是注释"就跳过 |

### 复现证据 (修复前 vs 修复后)

```
修复前 (模拟 LLM 返回合法 JSON):
   Q: 我手机丢在操场了 -> answer=这个问题我转交人工客服, confidence=0.0   ❌
   T: 宿舍302漏水一周了很着急没人管 -> emotion=neutral                    ❌

修复后:
   Q: 我手机丢在操场了 -> answer=建议先到操场失物招领处登记…, confidence=0.82  ✅
   Q: 怎么查询我的成绩 -> confidence=0.95 (这个是 ```json 围栏, 也剥对了)     ✅
   Q: 奖学金评定结果   -> needHuman=true, confidence=0.30                   ✅
   T: 宿舍302漏水…     -> emotion=negative                                 ✅
   T: 谢谢老师…        -> emotion=positive                                 ✅
   T: 图书馆几点开门   -> emotion=neutral                                  ✅
```
