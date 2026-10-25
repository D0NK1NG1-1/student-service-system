package com.example.studentservice.service;

import java.util.List;
import java.util.Map;

/**
 * 统计报表 Service · 组长 A 实现, 验收硬指标
 * <p>
 * 验收项: 统计报表(工单处理量、平均响应时间) — 本接口全部覆盖.
 * 组员 C 负责 DAO 层的 GROUP BY / COUNT / AVG, 组长 A 负责把原始数据包装成易读格式.
 */
public interface StatisticsService {

    /** 按状态分组统计工单数量 → 例如 {open=5, processing=3, closed=12} */
    Map<String, Long> ticketCountByStatus();

    /** 按类别分组 */
    Map<String, Long> ticketCountByCategory();

    /** 按优先级分组 */
    Map<String, Long> ticketCountByPriority();

    /** 按情绪分组 */
    Map<String, Long> ticketCountByEmotion();

    /**
     * 已关闭工单的平均响应时长 (秒)
     * 计算公式: AVG(TIMESTAMPDIFF(SECOND, create_time, close_time))
     * 返回 null 表示还没有关闭的工单
     */
    Double avgResponseSeconds();

    /** 各来源对话数 (faq/llm/manual) */
    Map<String, Long> conversationCountBySource();

    /** 近 7 天每天创建的工单数 (按 create_time 分组) */
    List<Object[]> dailyTicketCreatedLast7Days();

    /** 近 N 天客服处理量 TOP N (按 assignee + count) */
    List<Object[]> topAssignees(int topN);

    /** 近 N 天学生提问 TOP N */
    List<Object[]> topCreators(int topN);
}
