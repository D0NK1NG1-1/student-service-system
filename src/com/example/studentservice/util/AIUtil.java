package com.example.studentservice.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI / LLM 工具类 · 组长 A 维护, D 组员主要使用
 * <p>
 * 封装统一的 LLM 调用流程: 读配置 → 组 JSON body → 发 POST → 解析返回.
 * 不依赖任何第三方 JSON 库 (纯 JDK 手写 JSON 构造和简单解析).
 * <p>
 * 系统提示词 (System Prompt) 统一在此类里维护, 组员 D 如需修改请同步修改 prompts.md.
 */
public class AIUtil {

    private static String LLM_URL;
    private static String LLM_KEY;
    private static String LLM_MODEL;
    private static String SENTIMENT_URL;
    private static String SENTIMENT_KEY;
    private static String SENTIMENT_MODEL;

    static {
        Properties props = new Properties();
        try (InputStream is = AIUtil.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (is != null) { props.load(is); }
        } catch (IOException ignored) {}

        LLM_URL       = props.getProperty("llm.api.url",       "https://api.deepseek.com/v1/chat/completions");
        LLM_KEY       = props.getProperty("llm.api.key",       "");
        LLM_MODEL     = props.getProperty("llm.model",         "deepseek-chat");
        SENTIMENT_URL = props.getProperty("sentiment.api.url",  LLM_URL);
        SENTIMENT_KEY = props.getProperty("sentiment.api.key",  LLM_KEY);
        SENTIMENT_MODEL = props.getProperty("sentiment.model", LLM_MODEL);
    }

    // =========== 应用级系统提示词 (组长 A 统一维护) ===========

    /** AI 学生客服助手 — 问答用系统提示词 */
    public static final String SYSTEM_PROMPT_QA =
            "你是一个高校学生智能客服助手. 严格遵守以下规则:\n" +
            "1. 只回答校园业务 (选课/宿舍/图书馆/缴费/学生证/校园卡/食堂) 相关问题;\n" +
            "2. 如果问题不在知识范围内, 必须输出固定文案 '这个问题我转交人工客服', " +
            "并把 need_human 设为 true, 严禁编造;\n" +
            "3. 输出必须是严格的 JSON, 不要任何多余文字或解释:\n" +
            "   {\"answer\": \"字符串\", \"need_human\": true或false, \"confidence\": 0.00到1.00}\n" +
            "4. 回答使用简体中文, 不超过 80 字;\n" +
            "5. confidence 表示你对这个回答的把握程度, 0.9 以上表示非常确定.";

    /** 情感分析用系统提示词 (组员 D 调优: 补充校园特有负面词) */
    public static final String SYSTEM_PROMPT_SENTIMENT =
            "你是一个情感分析助手. 给定一段用户提问, 判断其情绪.\n" +
            "严格只输出以下 JSON, 不要任何多余文字:\n" +
            "{\"emotion\": \"positive\" 或 \"neutral\" 或 \"negative\"}\n" +
            "判断规则:\n" +
            "- 负面: 抱怨/愤怒/着急/不满/求助语气强烈, 尤其是这些校园高频负面表述:\n" +
            "  '怎么办' '很急' '着急' '没人管' '没人修' '找不到' '催了好几次' '一直没回应'\n" +
            "  '投诉' '又坏了' '漏水' '影响了上课/考试' '重复提交好几次';\n" +
            "- 正面: 感谢/表扬/满意 (如'谢谢''解决了''很赞');\n" +
            "- 中性: 普通询问, 只是在问流程或时间.\n" +
            "注意: 只要用户表达了催促、反复无果或影响正常学习生活, 一律判 negative.";

    // =========== 通用调用方法 ===========

    /**
     * 通用 LLM 对话调用 (最底层, D 组员可以直接用)
     * @param systemPrompt 系统提示词
     * @param userPrompt   用户输入
     * @return LLM 原始返回文本 (已截取 choices[0].message.content)
     */
    public static String callLLM(String systemPrompt, String userPrompt) {
        if (LLM_KEY == null || LLM_KEY.isEmpty() || LLM_KEY.contains("your")) {
            System.err.println("[AIUtil] LLM API Key 未配置, 请在 db.properties 填写 llm.api.key");
            return null;
        }
        try {
            Map<String,String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + LLM_KEY);
            headers.put("Content-Type", "application/json");

            String body = buildChatBody(LLM_MODEL, systemPrompt, userPrompt);
            String raw  = HttpUtil.doPostJson(LLM_URL, body, headers);
            // 防御: 接口报错 (key 无效/余额不足/限流) 时把原始错误打出来, 不要静默失败
            if (raw != null && raw.contains("\"error\"")) {
                System.err.println("[AIUtil] LLM 接口返回错误: " + raw);
            }
            return extractContent(raw);
        } catch (IOException e) {
            System.err.println("[AIUtil] LLM 调用失败: " + e.getMessage());
            return null;
        }
    }

    /** AI 问答 — 返回解析好的 AnswerResult */
    public static AnswerResult generateAnswer(String question) {
        String raw = callLLM(SYSTEM_PROMPT_QA, question);
        if (raw == null) {
            return new AnswerResult("抱歉, 服务暂不可用, 请稍后再试或联系人工客服.", true, 0.0);
        }
        raw = stripCodeFence(raw);   // LLM 常把 JSON 包在 ```json ... ``` 里, 先剥壳
        // 尝试解析 JSON, 失败给兜底值
        String answer     = extractString(raw, "answer", "这个问题我转交人工客服");
        boolean needHuman = extractBool(raw, "need_human", true);
        double confidence = extractDouble(raw, "confidence", 0.0);
        if (!raw.contains("answer")) {
            // 解析彻底失败时把原文打出来, 方便定位 (否则只会看到"转人工"却不知道为什么)
            System.err.println("[AIUtil] LLM 返回无法解析, 已降级转人工. 原始内容: " + raw);
        }
        return new AnswerResult(answer, needHuman, confidence);
    }

    /** 情感分析 — 返回解析好的情绪字符串 */
    public static String analyzeSentiment(String text) {
        if (text == null || text.trim().isEmpty()) return "neutral";
        String raw = callLLM(SYSTEM_PROMPT_SENTIMENT, text);
        if (raw == null) return "neutral";
        raw = stripCodeFence(raw);
        String emotion = extractString(raw, "emotion", "neutral").toLowerCase();
        if (!raw.contains("emotion")) {
            System.err.println("[AIUtil] 情感分析返回无法解析, 已降级 neutral. 原始内容: " + raw);
        }
        // 兜底: 只允许三种值
        if (!emotion.equals("positive") && !emotion.equals("negative")) {
            emotion = "neutral";
        }
        return emotion;
    }

    // =========== 内部: 手写 JSON 构造 & 简单解析 (避免依赖 fastjson/Gson) ===========

    /** 构造 OpenAI 兼容的 /v1/chat/completions body */
    private static String buildChatBody(String model, String systemPrompt, String userPrompt) {
        return "{"
                + "\"model\":\"" + esc(model) + "\","
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":\"" + esc(systemPrompt) + "\"},"
                + "{\"role\":\"user\",\"content\":\"" + esc(userPrompt) + "\"}"
                + "],"
                + "\"temperature\":0.3"
                + "}";
    }

    /**
     * 从 OpenAI 风格返回里抽取 choices[0].message.content
     * <p>
     * ⚠️ 不能用正则 "content"\s*:\s*"(.*?)": LLM 返回的 content 里如果嵌了 JSON
     * (本项目 prompt 就要求它输出 JSON), 内部引号会被转义成 \", 非贪婪正则会
     * 在第一个 \" 处提前收尾, 抽出来只剩 "{\", 后面 answer/confidence 全丢.
     * 所以这里改成逐字符扫描, 遇到转义符就把下一个字符一起收下.
     */
    private static String extractContent(String rawJson) {
        if (rawJson == null) return null;
        String key = "\"content\"";
        int keyIdx = rawJson.indexOf(key);
        while (keyIdx >= 0) {
            int colon = rawJson.indexOf(':', keyIdx + key.length());
            if (colon < 0) return rawJson;
            int p = colon + 1;
            while (p < rawJson.length() && Character.isWhitespace(rawJson.charAt(p))) p++;
            if (p >= rawJson.length()) return rawJson;
            if (rawJson.charAt(p) != '"') {                 // content 是 null/数字等情况, 看下一个
                keyIdx = rawJson.indexOf(key, keyIdx + key.length());
                continue;
            }
            StringBuilder sb = new StringBuilder();
            for (int j = p + 1; j < rawJson.length(); j++) {
                char c = rawJson.charAt(j);
                if (c == '\\') {                            // 转义字符: 连下一个一起保留
                    sb.append(c);
                    if (j + 1 < rawJson.length()) { sb.append(rawJson.charAt(j + 1)); j++; }
                    continue;
                }
                if (c == '"') return unesc(sb.toString());  // 未转义的引号才是真正的结束
                sb.append(c);
            }
            return unesc(sb.toString());
        }
        return rawJson; // 找不到就直接返回原文
    }

    /** 剥掉 LLM 常见的 ```json ... ``` 代码围栏 */
    private static String stripCodeFence(String s) {
        if (s == null) return null;
        String t = s.trim();
        if (!t.contains("```")) return t;
        int start = t.indexOf('\n', t.indexOf("```"));
        int end   = t.lastIndexOf("```");
        if (start >= 0 && end > start) {
            return t.substring(start + 1, end).trim();
        }
        return t.replace("```json", "").replace("```", "").trim();
    }

    /** 从 content 里按 key 取字符串值 */
    private static String extractString(String raw, String key, String def) {
        if (raw == null) return def;
        // 匹配 "key":"value"
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*\"(.*?)\"", Pattern.DOTALL);
        Matcher m = p.matcher(raw);
        if (m.find()) return unesc(m.group(1));
        return def;
    }

    private static boolean extractBool(String raw, String key, boolean def) {
        if (raw == null) return def;
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*(true|false)");
        Matcher m = p.matcher(raw);
        if (m.find()) return Boolean.parseBoolean(m.group(1));
        return def;
    }

    private static double extractDouble(String raw, String key, double def) {
        if (raw == null) return def;
        Pattern p = Pattern.compile("\"" + Pattern.quote(key) + "\"\\s*:\\s*([0-9.]+)");
        Matcher m = p.matcher(raw);
        if (m.find()) {
            try { return Double.parseDouble(m.group(1)); } catch (NumberFormatException e) { return def; }
        }
        return def;
    }

    /** JSON 字符串逃逸 (构造 body 时用) */
    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    /** JSON 字符串反转义 (解析时用) */
    private static String unesc(String s) {
        if (s == null) return "";
        return s.replace("\\\"", "\"").replace("\\\\", "\\")
                .replace("\\n", "\n").replace("\\r", "\r").replace("\\t", "\t");
    }

    // =========== 内部数据结构 ===========

    /** AI 问答结果 —— D 组员和 UI 层都用这个结构 */
    public static class AnswerResult {
        public final String answer;
        public final boolean needHuman;
        public final double confidence;
        public final String source;  // "faq" = 本地FAQ命中, "llm" = LLM返回

        public AnswerResult(String answer, boolean needHuman, double confidence) {
            this(answer, needHuman, confidence, "llm");
        }

        public AnswerResult(String answer, boolean needHuman, double confidence, String source) {
            this.answer = answer;
            this.needHuman = needHuman;
            this.confidence = confidence;
            this.source = source;
        }

        @Override
        public String toString() {
            return "AnswerResult{answer='" + answer + "', needHuman=" + needHuman +
                    ", confidence=" + confidence + ", source='" + source + "'}";
        }
    }
}
