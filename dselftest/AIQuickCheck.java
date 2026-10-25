import com.example.studentservice.entity.Faq;
import com.example.studentservice.service.FaqService;
import com.example.studentservice.service.impl.FaqServiceImpl;
import com.example.studentservice.util.AIUtil;
import com.example.studentservice.util.DBUtil;

/**
 * D 组员 AI 模块自检程序 (临时文件, 验收前可删)
 * 用法:
 *   1) 先起模拟 LLM:  python dselftest/mock_llm_server.py
 *   2) 编译:          javac -encoding UTF-8 -cp "out/production/test1;lib/mysql-connector-j-8.0.33.jar" -d out dselftest/AIQuickCheck.java
 *   3) 运行(走模拟):   java -cp "dselftest_mockconf;out;resources;lib/mysql-connector-j-8.0.33.jar" AIQuickCheck
 *   4) 运行(走真实配置): java -cp "out;resources;lib/mysql-connector-j-8.0.33.jar" AIQuickCheck
 */
public class AIQuickCheck {

    public static void main(String[] args) {
        System.out.println("=========== D 组员 AI 模块自检 ===========");

        System.out.println("\n[1] 数据库连通性");
        System.out.println("    " + (DBUtil.testConnection()
                ? "OK  -> FAQ 命中链路可用" : "FAIL -> FAQ 命中不可用, 全部会掉到 LLM"));

        System.out.println("\n[2] 本地 FAQ 命中 (AI 链路第一跳)");
        FaqService faqService = new FaqServiceImpl();
        String[] faqQs = {"图书馆开放时间", "图书馆时间", "宿舍怎么申请换寝", "我手机丢在操场了"};
        for (String q : faqQs) {
            Faq f;
            try {
                f = faqService.matchQuestion(q);
            } catch (Exception e) {
                System.out.println("    Q: " + q + " -> 异常 " + e);
                continue;
            }
            System.out.println("    Q: " + q + " -> "
                    + (f == null ? "未命中 (将走 LLM)"
                                 : "命中 FAQ#" + f.getId() + " [" + f.getCategory() + "] " + f.getQuestion()));
        }

        System.out.println("\n[3] LLM 问答 (AI 链路第二跳)");
        String[] llmQs = {"我手机丢在操场了", "怎么查询我的成绩", "帮我查一下我的奖学金评定结果"};
        for (String q : llmQs) {
            AIUtil.AnswerResult r = AIUtil.generateAnswer(q);
            System.out.println("    Q: " + q);
            System.out.println("       -> answer    = " + r.answer);
            System.out.println("       -> needHuman = " + r.needHuman);
            System.out.println("       -> confidence= " + r.confidence);
            System.out.println("       -> source    = " + r.source);
        }

        System.out.println("\n[4] 情感分析 (工单自动升 urgent 的依据)");
        String[] texts = {"宿舍302漏水一周了很着急没人管", "谢谢老师, 问题已经解决了", "请问图书馆几点开门"};
        for (String t : texts) {
            System.out.println("    T: " + t + " -> emotion = " + AIUtil.analyzeSentiment(t));
        }

        System.out.println("\n=========== 自检结束 ===========");
    }
}
