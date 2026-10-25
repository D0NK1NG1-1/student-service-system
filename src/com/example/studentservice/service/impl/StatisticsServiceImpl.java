package com.example.studentservice.service.impl;

import com.example.studentservice.dao.ConversationDao;
import com.example.studentservice.dao.TicketDao;
import com.example.studentservice.dao.impl.ConversationDaoImpl;
import com.example.studentservice.dao.impl.TicketDaoImpl;
import com.example.studentservice.service.StatisticsService;

import java.util.*;

/**
 * 统计报表 Service — 组长 A 完整写, 验收硬指标
 * <p>
 * 依赖 TicketDao 和 ConversationDao 的 GROUP BY / COUNT 方法 (组员 C 实现的 DAO 层).
 * 如果组员 C 还没写完 DAO, 可以先直接用 DBUtil.runGroupBy("SELECT ...") 快速跑起来.
 */
public class StatisticsServiceImpl implements StatisticsService {

    private final TicketDao ticketDao = new TicketDaoImpl();
    private final ConversationDao convDao = new ConversationDaoImpl();

    // ============ 按状态/类别/优先级/情绪 分组统计 ============

    @Override
    public Map<String, Long> ticketCountByStatus() {
        return toMap(ticketDao.countByStatus(),
                new String[]{"open", "processing", "pending_customer", "closed"});
    }

    @Override
    public Map<String, Long> ticketCountByCategory() {
        // 类别是自由文本 (种子数据里就有 选课/宿舍/图书馆/缴费/教学/网络...), 不预填固定 key,
        // 否则卡片里会塞 8 行 0 值还要滚动条. 只显示库里真实存在的类别.
        return toMap(ticketDao.countByCategory(), new String[0]);
    }

    @Override
    public Map<String, Long> ticketCountByPriority() {
        return toMap(ticketDao.countByPriority(),
                new String[]{"low", "medium", "high", "urgent"});
    }

    @Override
    public Map<String, Long> ticketCountByEmotion() {
        return toMap(ticketDao.countByEmotion(),
                new String[]{"positive", "neutral", "negative"});
    }

    @Override
    public Double avgResponseSeconds() {
        return ticketDao.avgResponseSeconds();
    }

    @Override
    public Map<String, Long> conversationCountBySource() {
        return toMap(convDao.countBySource(), new String[]{"faq", "llm", "manual"});
    }

    @Override
    public List<Object[]> dailyTicketCreatedLast7Days() {
        // 组长 A 直接用原生 SQL, 不依赖 TicketDao 的新方法
        // 返回 [yyyy-MM-dd, count] 列表
        String sql = "SELECT DATE(create_time) AS day, COUNT(*) AS cnt " +
                     "FROM ticket WHERE create_time >= CURDATE() - INTERVAL 6 DAY " +
                     "GROUP BY DATE(create_time) ORDER BY day ASC";
        return com.example.studentservice.util.DBUtil.runGroupBy(sql);
    }

    @Override
    public List<Object[]> topAssignees(int topN) {
        String sql = "SELECT u.real_name AS name, COUNT(*) AS cnt " +
                     "FROM ticket t JOIN user u ON t.assignee_id = u.id " +
                     "WHERE t.assignee_id IS NOT NULL " +
                     "GROUP BY t.assignee_id, u.real_name " +
                     "ORDER BY cnt DESC LIMIT " + topN;
        return com.example.studentservice.util.DBUtil.runGroupBy(sql);
    }

    @Override
    public List<Object[]> topCreators(int topN) {
        String sql = "SELECT u.real_name AS name, COUNT(*) AS cnt " +
                     "FROM ticket t JOIN user u ON t.creator_id = u.id " +
                     "GROUP BY t.creator_id, u.real_name " +
                     "ORDER BY cnt DESC LIMIT " + topN;
        return com.example.studentservice.util.DBUtil.runGroupBy(sql);
    }

    // ============ 内部: List<Object[]> → Map<String, Long> ============

    /**
     * 把 GROUP BY 结果转成 Map.
     * <p>
     * ⚠️ 修复 (2026-10-25): 原来这里把 status / priority / emotion 三套 key 全部预填,
     * 导致「按状态分布」里也会出现 high / negative 等无关分类 (报表上每张卡片都显示 11 行).
     * 现在改成按维度传入自己那套 key, 只预填本维度的.
     *
     * @param rows      GROUP BY 查询结果 [key, count]
     * @param knownKeys 本维度所有合法取值 (没数据的补 0, 保证报表完整)
     */
    private Map<String, Long> toMap(List<Object[]> rows, String[] knownKeys) {
        Map<String, Long> map = new LinkedHashMap<>();
        for (String k : knownKeys) map.put(k, 0L);

        // 填入真实数据
        if (rows != null) {
            for (Object[] row : rows) {
                String key = row[0] == null ? "(空)" : row[0].toString();
                Long cnt   = (Long) row[1];
                // 如果预填的 map 里没这个 key, 也加进去 (比如自定义 category)
                map.merge(key, cnt, Long::sum);
            }
        }
        return map;
    }

    // ============ 便捷: 打印整个统计面板 (Main 菜单里会调) ============

    /** 一次性打印所有统计数据, Main 菜单 8 会调 */
    public void printDashboard() {
        System.out.println("\n====================================");
        System.out.println("        📊 智能学生客服系统 · 统计面板");
        System.out.println("====================================");

        System.out.println("\n[工单按状态]");
        for (Map.Entry<String, Long> e : ticketCountByStatus().entrySet()) {
            System.out.printf("  %-20s %d%n", e.getKey(), e.getValue());
        }

        System.out.println("\n[工单按优先级]");
        for (Map.Entry<String, Long> e : ticketCountByPriority().entrySet()) {
            System.out.printf("  %-20s %d%n", e.getKey(), e.getValue());
        }

        System.out.println("\n[工单按情绪]");
        for (Map.Entry<String, Long> e : ticketCountByEmotion().entrySet()) {
            System.out.printf("  %-20s %d%n", e.getKey(), e.getValue());
        }

        System.out.println("\n[工单按类别]");
        for (Map.Entry<String, Long> e : ticketCountByCategory().entrySet()) {
            System.out.printf("  %-20s %d%n", e.getKey(), e.getValue());
        }

        System.out.println("\n[对话按来源]");
        for (Map.Entry<String, Long> e : conversationCountBySource().entrySet()) {
            System.out.printf("  %-20s %d%n", e.getKey(), e.getValue());
        }

        Double avg = avgResponseSeconds();
        System.out.printf("\n[已关闭工单平均响应时长] %.1f 秒%n",
                avg == null ? 0.0 : avg);
        if (avg != null) {
            double minutes = avg / 60;
            double hours   = minutes / 60;
            if (hours >= 1)      System.out.printf("  (≈ %.1f 小时)%n", hours);
            else if (minutes >= 1) System.out.printf("  (≈ %.1f 分钟)%n", minutes);
        }

        System.out.println("\n[近7天每日工单量]");
        for (Object[] row : dailyTicketCreatedLast7Days()) {
            System.out.printf("  %-12s %d%n", row[0], row[1]);
        }

        System.out.println("\n[客服处理量 TOP 5]");
        for (Object[] row : topAssignees(5)) {
            System.out.printf("  %-15s %d%n", row[0], row[1]);
        }

        System.out.println("\n[学生提问 TOP 5]");
        for (Object[] row : topCreators(5)) {
            System.out.printf("  %-15s %d%n", row[0], row[1]);
        }

        System.out.println("====================================\n");
    }
}
