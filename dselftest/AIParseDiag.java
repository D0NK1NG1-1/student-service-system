import java.lang.reflect.Method;
import com.example.studentservice.util.AIUtil;

/**
 * D 组员 AI 解析诊断 (临时文件)
 * 目的: 证明 AIUtil.extractContent 的正则在"content 里嵌 JSON"时会被转义引号截断.
 */
public class AIParseDiag {

    // 真实 LLM 返回的原始报文: content 里是一段 JSON, 内部的引号被转义成 \"
    private static final String RAW =
            "{\"id\":\"1\",\"choices\":[{\"message\":{\"role\":\"assistant\","
            + "\"content\":\"{\\\"answer\\\": \\\"建议先到失物招领处登记\\\", "
            + "\\\"need_human\\\": false, \\\"confidence\\\": 0.82}\"}}]}";

    public static void main(String[] args) throws Exception {
        System.out.println("原始报文:");
        System.out.println("  " + RAW);

        Method m = AIUtil.class.getDeclaredMethod("extractContent", String.class);
        m.setAccessible(true);
        Object got = m.invoke(null, RAW);
        System.out.println("\n[现状] AIUtil.extractContent 抽出的是:");
        System.out.println("  [" + got + "]   <-- 长度 " + String.valueOf(got).length());

        System.out.println("\n[修复方案] 逐字符扫描, 遇到 \\\" 不结束, 遇到 \\\\ 跳过下一个字符:");
        System.out.println("  [" + extractContentFixed(RAW) + "]");

        System.out.println("\n[结论] 现状正则 \"content\"\\s*:\\s*\"(.*?)\" 是非贪婪匹配,");
        System.out.println("       一碰到 content 里第一个转义引号就提前收尾, 所以 answer/confidence 全丢了.");
    }

    /** 修复版: 正确处理转义字符的扫描式解析 */
    private static String extractContentFixed(String rawJson) {
        if (rawJson == null) return null;
        String key = "\"content\"";
        int i = rawJson.indexOf(key);
        if (i < 0) return rawJson;
        int p = rawJson.indexOf('"', i + key.length());   // 开始引号
        if (p < 0) return rawJson;
        StringBuilder sb = new StringBuilder();
        for (int j = p + 1; j < rawJson.length(); j++) {
            char c = rawJson.charAt(j);
            if (c == '\\') {                              // 转义: 连下一个字符一起收
                if (j + 1 < rawJson.length()) { sb.append(rawJson.charAt(j + 1)); j++; }
                continue;
            }
            if (c == '"') break;                          // 未转义的引号才是结束
            sb.append(c);
        }
        return sb.toString();
    }
}
