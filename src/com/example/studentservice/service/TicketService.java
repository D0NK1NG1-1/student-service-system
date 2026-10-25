package com.example.studentservice.service;

import com.example.studentservice.entity.Ticket;
import java.util.List;

/**
 * 工单 Service · 组长 A 定义, 组员 C 实现
 * <p>
 * 工单状态机:
 *   open(待处理)  →  assign →  processing(处理中)  →  close →  closed(已关闭)
 *                          ↓
 *                    pending_customer(待用户)  →  reopen →  processing
 * <p>
 * 任何状态变更必须:
 *   1) 更新 ticket 表对应字段
 *   2) 写一条 ticket_log (用 TicketLogService)
 *   3) 如果是创建工单, 还会触发 AI 问答链路 (调 AIService) —— 这部分 D 组员实现
 */
public interface TicketService {

    /**
     * 学生创建工单. 内部会:
     *   1. 落 ticket 表
     *   2. 写 create 日志
     *   3. 调情感分析 (AIUtil.analyzeSentiment) → 自动设 emotion 字段
     *   4. 如果 emotion=negative → 自动升 priority=urgent
     */
    Ticket createTicket(Long creatorId, String title, String content, String category);

    /** 管理员/客服: 分配工单给客服, 写 assign 日志 */
    boolean assignTicket(Long ticketId, Long assigneeId, Long operatorId);

    /** 客服: 开始处理, 状态 open → processing, 写 process 日志 */
    boolean startProcessing(Long ticketId, Long operatorId);

    /** 客服: 回应用户 (写对话 + 工单状态 pending_customer), 写 reply 日志 */
    boolean replyToCustomer(Long ticketId, Long operatorId, String message);

    /** 学生: 关闭工单 */
    boolean closeTicket(Long ticketId, Long operatorId);

    /** 客服: 重新打开 (pending_customer → processing) */
    boolean reopenTicket(Long ticketId, Long operatorId);

    /** 管理员: 改优先级 (并写日志) */
    boolean updatePriority(Long ticketId, String priority, Long operatorId);

    // ---- 查询 ----
    Ticket getById(Long id);

    /** 学生: 查我自己的工单 (含 creator_name / assignee_name JOIN) */
    List<Ticket> listMyTickets(Long creatorId);

    /** 客服: 分配给我的工单 */
    List<Ticket> listMyAssigned(Long assigneeId);

    /** 多条件组合分页 (管理端高级搜索) */
    List<Ticket> searchTickets(String titleKeyword, String category,
                              String status, String priority,
                              int page, int size);
    long searchCount(String titleKeyword, String category,
                    String status, String priority);

    Ticket getWithUserNames(Long id);
}
