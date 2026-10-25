# 🅳️ 组员 D · 任务清单与代码自检报告

> 生成时间: 2026-10-24 ｜ 自检环境: JDK 17 以 `--release 8` 编译 + 本机 MySQL(3306) + 阿里云百炼 qwen-plus

---

## 一、你的 5 项任务 + 当前完成情况

| # | 任务 (来自组员对接指南) | 状态 | 说明 |
|---|------------------------|------|------|
| ① | 填真实 LLM API Key | ✅ 已完成 | `resources/db.properties` 已换成千问 (DashScope OpenAI 兼容模式), 实测调用成功 |
| ② | 调优两份 System Prompt | ✅ 部分完成 | 情感 prompt 已补校园负面词; **问答 prompt 建议再补一条"必须输出裸 JSON"的约束** (见第四节) |
| ③ | 跑通三种 AI 场景 (FAQ命中 / 走LLM / 转人工) | ✅ 已完成 | 三个场景全部实测通过, 见第三节 |
| ④ | 写 AI 错误档案 ≥3 条 | ✅ 已完成 | 写了 **5 条真实案例**, 已追加到 `prompts.md` 第五节 |
| ⑤ | 加分扩展 (多轮上下文 / 自动弹创建工单) | ⬜ 未做 | 可选, 时间够再做 |

**另外（额外发现）:** 组长 A 的 `AIUtil.extractContent` 有一个致命解析 bug，已修复并验证，见第二节。

---

## 二、发现并修复的 Bug（重要，群里同步给组长 A）

### 问题：`AIUtil.extractContent` 用非贪婪正则抽 content，会被转义引号截断

```java
// 修复前
Matcher m = Pattern.compile("\"content\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL).matcher(rawJson);
```

- 我们的 prompt **要求 LLM 在 content 里返回 JSON**，所以真实报文长这样：
  `"content":"{\"answer\": \"建议先到失物招领处登记\", \"confidence\": 0.82}"`
- 正则 `(.*?)` 非贪婪，一碰到内容里第一个转义引号 `\"` 就收尾 → **只抽出 `{\` 两个字符**
- 后果：`answer` / `confidence` / `need_human` 全部取不到 → **所有 LLM 回答都退化成"转人工 + confidence=0.0"**，情感分析也永远返回 `neutral`
- 演示时老师会看到"AI 问答点了没反应 / 永远转人工"，**这是致命的，必须修**

### 修复内容（3 处，都在 `AIUtil.java`）

1. `extractContent` 改成**逐字符扫描**：遇 `\` 连下一个字符一起收，只把未转义的 `"` 当结束
2. 新增 `stripCodeFence()`：解析前剥掉 LLM 常见的 ` ```json ... ``` ` 围栏
3. `callLLM` 里检测接口返回的 `"error"` 字段并打印原始错误（Key 无效/余额不足时不再静默失败）
4. 顺手清理了 `AIUtil.java` 第 144 行一处 AI 生成时串字的乱码注释

---

## 三、自检实测结果

### 编译
```
javac -encoding UTF-8 --release 8 → 38 个源文件, 0 错误 0 警告   ✅
```

### ① 本地 FAQ 命中（链路第一跳，走真实 MySQL）
| 输入 | 结果 |
|------|------|
| 图书馆开放时间 | ✅ 命中 FAQ#3 [图书馆] |
| **图书馆时间**（缺字） | ✅ 命中 FAQ#3 — n-gram 滑窗分词生效 |
| 宿舍怎么申请换寝 | ✅ 命中 FAQ#4 [宿舍] |
| 我手机丢在操场了 | ✅ 未命中 → 正确落到 LLM |

### ② 真实 LLM 问答（qwen-plus，HTTP 全链路）
| 场景 | 输入 | 实测结果 |
|------|------|----------|
| 走 LLM | 怎么查询我的成绩 | ✅ `登录教务系统或"智慧校园"APP…`，confidence **0.95** |
| 转人工 | 帮我查一下我的奖学金评定结果 | ✅ `needHuman=true`，confidence 0.85 |
| 走 LLM | 我手机丢在操场了 | ⚠️ 模型只回了裸文本没按 JSON 输出 → 命中兜底转人工（已加日志，行为正确） |

同时验证：`Authorization: Bearer xxx` 请求头正常发出，POST body 正常，超时 5s/15s 已设置 → **验收点"API 鉴权 + 超时 + 异常处理"稳了**。

### ③ 情感分析（工单自动升 urgent 的依据）
| 输入 | 情绪 |
|------|------|
| 宿舍302漏水一周了很着急没人管 | ✅ **negative** → 会自动升 urgent |
| 谢谢老师, 问题已经解决了 | ✅ positive |
| 请问图书馆几点开门 | ✅ neutral |

`TicketServiceImpl.createTicket` 第 127-135 行已确认：`analyzeSentiment` → negative → `priority=urgent` 并写日志，链路完整。

---

## 四、还需要你做的 3 件事

1. **在 IDEA 里完整跑一遍 Swing 演示**（我只能跑 DAO/Service 层，图形界面要你手动点）
   `Run Main.java` → student01 登录 → AI 问答"图书馆开放时间" → 建工单"宿舍漏水很着急没人管" → 看是否自动 urgent
2. **问答 prompt 再加一条硬约束**（`AIUtil.SYSTEM_PROMPT_QA`）：
   建议在第 3 条后面补一句 → `不要使用 markdown 代码围栏, 不要输出除 JSON 以外的任何字符`
   （实测千问偶发只回裸文本，加了这条能显著减少兜底概率；改完记得同步 `prompts.md` 第二节）
3. **群里同步给组长 A**：`AIUtil.java` 我改了 4 处 + `db.properties` 换成了千问端点，请他 Code Review 一下

### ⚠️ 两个注意事项
- `db.properties` 现在**含真实 API Key**，别 push 到公开 Git 仓库 / 别发群里
- 自检跑过几次 FAQ 命中，FAQ 的 `hit_count` 被真实累加了（这是正常功能行为，不是脏数据）

---

## 五、自检程序怎么用（可复跑，验收前可删 `dselftest/` 目录）

```bash
# 1) 起本地模拟 LLM（不想烧真实额度时用）
python dselftest/mock_llm_server.py

# 2) 编译
javac -encoding UTF-8 -cp "lib/mysql-connector-j-8.0.33.jar" -d dselftest/out \
      $(find src -name "*.java") dselftest/AIQuickCheck.java dselftest/AIParseDiag.java

# 3a) 跑真实千问
java -Dfile.encoding=UTF-8 -cp "dselftest/out;resources;lib/mysql-connector-j-8.0.33.jar" AIQuickCheck

# 3b) 跑本地模拟（把 mock 配置放 classpath 最前面覆盖 db.properties）
java -Dfile.encoding=UTF-8 -cp "dselftest/dselftest_mockconf;dselftest/out;lib/mysql-connector-j-8.0.33.jar" AIQuickCheck

# 3c) 单独看解析 bug 的复现证据
java -Dfile.encoding=UTF-8 -cp "dselftest/out" AIParseDiag
```

---

---

## 六、运行环境排障（2026-10-24 追加）

### 问题 A：IDEA 运行报 `ClassNotFoundException: com.mysql.cj.jdbc.Driver`

**根因：IDEA 打开的是外层目录 `D:\qq文件\test1`，不是项目根目录 `D:\qq文件\test1\test1`。**
外层那个自动生成的 `D:\qq文件\test1\test1.iml` 里只有 `test1/src` 一个源码目录，**既没引用 mysql 驱动库，也没把 resources 标成资源目录** → classpath 里只有 `out\production\test1`，自然找不到驱动。

（驱动库本身是定义好的，在 `D:\qq文件\test1\.idea\libraries\mysql_connector_j_8_0_33.xml`，只是模块没引用它。）

**已修** `D:\qq文件\test1\test1.iml`（外层），补了三行：
- `<orderEntry type="library" name="mysql-connector-j-8.0.33" level="project" />` ← 解决 ClassNotFoundException
- `<sourceFolder url=".../test1/resources" type="java-resource" />` ← 否则 `db.properties` 不进 classpath，AIUtil 读不到 LLM Key
- `<excludeFolder url=".../test1/dselftest" />` ← 自检目录不参与编译

**建议做法**：更干净的方案是 IDEA 里 `File → Open` 直接打开 `D:\qq文件\test1\test1`（内层 `.iml` 本来就配好了 src + resources + 驱动），我已在内层 `.iml` 也加了 excludeFolder。

> 改完 `.iml` 需要 IDEA 重新加载：File → Reload All from Disk（或重启 IDEA）后重新 Run Main。

### 问题 B：MySQL `Error Code 1452` 外键冲突

`INSERT INTO conversation` 里 assistant 那两行的 `user_id` 写的是 **0**，而 `user` 表 id 从 1 开始（没有 id=0 的用户），撞上 `fk_conv_user` 外键。

**已修** `resources/schema.sql`：两处 `0` → `3`（学生张三，和 `AIServiceImpl` 实际写 conversation 的行为一致）。

重新执行后 5 张表全部初始化成功：

| 表 | 行数 |
|---|---|
| user | 5 |
| faq | 6 |
| ticket | 3 |
| conversation | 4 |
| ticket_log | 5 |

执行命令（已验证 0 报错）：
```bash
"D:\MySQL Workbench 8.0\mysql.exe" -h 127.0.0.1 -u root -p123456 \
    --default-character-set=utf8mb4 < resources/schema.sql
```
⚠️ 脚本开头有 `DROP TABLE`，会清空重建，别在有重要数据的库上跑。

### 附：JDK 版本
IDEA 当前用的是 `openjdk-26.0.1`，实测 mysql-connector-j 8.0.33 + 本项目代码在 JDK 26 下**能正常加载驱动、连库、调 LLM**，不需要强求 JDK 8（但验收说是 JDK 8 项目，建议还是换回 8/17 更稳）。

---

## 七、验收口述准备（老师大概率问这几个）

- **HTTP 调用流程**：`AIUtil.callLLM` → 组 OpenAI 格式 JSON body → `HttpUtil.doPostJson` → `HttpURLConnection` 设 Bearer 头 + 5s连接/15s读超时 → 读响应 → `extractContent` 抽 content → 手写正则取 answer/need_human/confidence
- **为什么要 SwingWorker**：LLM 调用要 5-15 秒，Swing 的 UI 刷新在 EDT 单线程里，直接在按钮事件里调 HTTP 会把整个窗口卡死；`SwingWorker.doInBackground` 里发请求，`done()` 里回写 `JTextArea`
- **双层链路设计**：先查本地 FAQ（n-gram 滑窗分词 + LIKE，0 成本、0 延迟、答案可控）→ 未命中才调 LLM（省钱 + 避免幻觉）→ LLM 判 need_human 就转人工
- **情感分析怎么落库**：`createTicket` 里 `analyzeSentiment(title + content)` → negative → `priority` 升 urgent → 写 `ticket_log`
