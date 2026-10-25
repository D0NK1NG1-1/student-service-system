import com.example.studentservice.dao.TicketDao;
import com.example.studentservice.dao.impl.TicketDaoImpl;
import com.example.studentservice.entity.Faq;
import com.example.studentservice.entity.Ticket;
import com.example.studentservice.entity.User;
import com.example.studentservice.service.FaqService;
import com.example.studentservice.service.StatisticsService;
import com.example.studentservice.service.TicketService;
import com.example.studentservice.service.UserService;
import com.example.studentservice.service.impl.FaqServiceImpl;
import com.example.studentservice.service.impl.StatisticsServiceImpl;
import com.example.studentservice.service.impl.TicketServiceImpl;
import com.example.studentservice.service.impl.UserServiceImpl;
import com.example.studentservice.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Map;

/** 项目功能审计（临时文件，跑完自动清理测试数据） */
public class ProjectAudit {

    private static int pass = 0, fail = 0;

    private static void check(String name, boolean ok, String detail) {
        if (ok) { pass++; System.out.println("  [PASS] " + name + (detail.isEmpty() ? "" : "  (" + detail + ")")); }
        else    { fail++; System.out.println("  [FAIL] " + name + "  -> " + detail); }
    }

    public static void main(String[] args) {
        System.out.println("================ 项目功能审计 ================");

        // ---------- 1. 用户模块 ----------
        System.out.println("\n[1] 用户模块");
        UserService userService = new UserServiceImpl();
        User admin = userService.login("admin01", "123456");
        check("admin 登录", admin != null && "admin".equals(admin.getRole()), admin == null ? "返回 null" : admin.getRealName());
        User cs = userService.login("cs001", "123456");
        check("客服登录", cs != null && "cs".equals(cs.getRole()), cs == null ? "返回 null" : cs.getRealName());
        User stu = userService.login("student01", "123456");
        check("学生登录", stu != null && "student".equals(stu.getRole()), stu == null ? "返回 null" : stu.getRealName());
        check("错误密码被拒", userService.login("student01", "wrongpwd") == null, "");
        check("分页查询用户", userService.listByRolePage("student", 1, 10).size() > 0, "student 第一页");
        check("按角色查用户", userService.listByRole("cs").size() >= 2, "cs 共 " + userService.listByRole("cs").size() + " 人");

        // ---------- 2. FAQ 模块 ----------
        System.out.println("\n[2] FAQ 模块（B 负责）");
        FaqService faqService = new FaqServiceImpl();
        List<Faq> faqs = faqService.listAll();
        check("selectAll", faqs.size() > 0, faqs.size() + " 条");
        check("selectById", faqService.getById(3L) != null, "");
        check("LIKE 关键词搜索", faqService.searchByKeyword("图书馆").size() > 0, "命中 " + faqService.searchByKeyword("图书馆").size());
        check("分类查询", faqService.listByCategory("宿舍").size() > 0, "");
        check("n-gram 模糊匹配", faqService.matchQuestion("图书馆时间") != null, "「图书馆时间」→ 命中");

        Faq origin = faqService.getById(6L);
        if (origin != null) {
            Faq upd = new Faq();
            upd.setId(6L);
            upd.setAnswer(origin.getAnswer());   // 值不变，只为测 UPDATE 是否真的执行
            upd.setQuestion(origin.getQuestion());
            boolean ok = faqService.update(upd);
            check("FAQ update（B 的 TODO）", ok, ok ? "" : "update 返回 false —— FaqDaoImpl.update 仍是空壳 return 0");
        }
        check("分页", faqService.listPage(1, 3).size() <= 3, "");

        // ---------- 3. 工单模块 + 状态机 ----------
        System.out.println("\n[3] 工单模块 + 状态机（C 负责）");
        TicketService ticketService = new TicketServiceImpl();
        Ticket t = null;
        try {
            t = ticketService.createTicket(stu.getId(), "审计测试工单", "宿舍漏水很着急没人管，催了好几次", "宿舍");
            check("创建工单", t != null && t.getId() != null, t == null ? "null" : "id=" + t.getId());
            check("情感分析自动升 urgent", t != null && "urgent".equals(t.getPriority()),
                    t == null ? "" : "emotion=" + t.getEmotion() + ", priority=" + t.getPriority());

            if (t != null) {
                check("状态机 open→processing", ticketService.startProcessing(t.getId(), cs.getId()), "");
                check("状态机 processing→pending_customer", ticketService.replyToCustomer(t.getId(), cs.getId(), "已安排维修"), "");
                check("状态机 pending_customer→closed", ticketService.closeTicket(t.getId(), cs.getId()), "");
                check("closed 后禁止 reopen（业务保护）", !ticketService.reopenTicket(t.getId(), cs.getId()), "");
                check("非法流转被拒（closed 再 close）", !ticketService.closeTicket(t.getId(), cs.getId()), "");
            }
        } catch (Exception e) {
            check("工单主流程", false, e.toString());
        }

        // JOIN 硬指标（Ticket 若无 creatorName 字段则说明 C 的 TODO 未完成）
        TicketDao ticketDao = new TicketDaoImpl();
        Ticket joined = ticketDao.selectWithUserNamesById(1L);
        String creatorName = null, assigneeName = null;
        boolean hasField = false;
        try {
            creatorName  = (String) Ticket.class.getMethod("getCreatorName").invoke(joined);
            assigneeName = (String) Ticket.class.getMethod("getAssigneeName").invoke(joined);
            hasField = true;
        } catch (NoSuchMethodException e) {
            hasField = false;
        } catch (Exception e) {
            hasField = true;  // 字段在但取值异常
        }
        check("JOIN 查询解析出创建人姓名（C 的 TODO）", hasField && creatorName != null && !creatorName.isEmpty(),
                !hasField ? "Ticket 实体没有 creatorName 字段 —— JOIN 结果被丢弃"
                          : "creatorName=" + creatorName + ", assigneeName=" + assigneeName);

        check("多条件+分页查询", ticketService.searchTickets(null, null, null, null, 1, 10).size() > 0, "");
        check("countByConditions", ticketService.searchCount(null, null, null, null) > 0, "");

        // ---------- 4. 统计 ----------
        System.out.println("\n[4] 统计模块");
        StatisticsService stat = new StatisticsServiceImpl();
        Map<String, Long> byStatus = stat.ticketCountByStatus();
        check("GROUP BY status", !byStatus.isEmpty(), byStatus.toString());
        check("GROUP BY priority", !stat.ticketCountByPriority().isEmpty(), stat.ticketCountByPriority().toString());
        check("GROUP BY category", !stat.ticketCountByCategory().isEmpty(), "");
        check("GROUP BY emotion", !stat.ticketCountByEmotion().isEmpty(), "");
        check("AVG 响应时长(TIMESTAMPDIFF)", stat.avgResponseSeconds() != null, "avg=" + stat.avgResponseSeconds());
        check("GROUP BY conversation.source", !stat.conversationCountBySource().isEmpty(), "");
        check("近7天趋势", !stat.dailyTicketCreatedLast7Days().isEmpty(), "");
        check("TOP 客服", stat.topAssignees(3) != null, "");

        // ---------- 5. 清理 ----------
        System.out.println("\n[5] 清理测试数据");
        if (t != null && t.getId() != null) {
            cleanup(t.getId());
            System.out.println("  已删除测试工单 id=" + t.getId());
        }

        System.out.println("\n================ 结果: " + pass + " 通过 / " + fail + " 失败 ================");
    }

    private static void cleanup(long ticketId) {
        try (Connection conn = DBUtil.getConnection()) {
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM conversation WHERE ticket_id = ?")) { ps.setLong(1, ticketId); ps.executeUpdate(); }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ticket_log WHERE ticket_id = ?"))   { ps.setLong(1, ticketId); ps.executeUpdate(); }
            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM ticket WHERE id = ?"))             { ps.setLong(1, ticketId); ps.executeUpdate(); }
        } catch (Exception e) {
            System.out.println("  清理失败: " + e.getMessage());
        }
    }
}
