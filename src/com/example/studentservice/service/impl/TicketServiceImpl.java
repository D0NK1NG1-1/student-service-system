package com.example.studentservice.service.impl;

import com.example.studentservice.dao.TicketDao;
import com.example.studentservice.dao.impl.TicketDaoImpl;
import com.example.studentservice.entity.Ticket;
import com.example.studentservice.service.TicketLogService;
import com.example.studentservice.service.TicketService;
import com.example.studentservice.util.AIUtil;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单 Service — ⚠️ 组员 C 负责完整实现 (状态机, 验收硬指标!)
 * <p>
 * 组长 A 已经把 TicketService 接口 + TicketDaoImpl 完整写好.
 * C 组员需要把每个业务方法的状态流转 + 日志写入补全.
 * <p>
 * 工单状态机:
 *   open → (assign) → processing → (close) → closed
 *                 ↑
 *           (reopen) ← pending_customer ← (reply)
 * <p>
 * 任何状态变更必须:
 *   1) update ticket 表 (ticketDao.update)
 *   2) 写日志 (ticketLogService.writeLog)
 *   3) close 时设置 close_time = NOW()
 * <p>
 * 权限规则:
 *   admin    → 可以操作任何工单 (分配/改优先级/关闭)
 *   cs 客服  → 只能操作 assignee_id = 自己的工单
 *   student  → 只能关闭自己创建的工单, 可以 reopen pending_customer 的
 */
public class TicketServiceImpl implements TicketService {

    private final TicketDao ticketDao = new TicketDaoImpl();
    private final TicketLogService ticketLogService = new TicketLogServiceImpl();

    // ============ 权限校验辅助方法 ============

    /**
     * 判断 operator 是否有权操作这个工单 (管理员豁免).
     * admin 返回 true, cs 只能操作分配给自己的, student 只能操作自己创建的.
     */
    private boolean canOperatorHandle(Ticket t, Long operatorId, String role) {
        if (t == null) return false;
        if ("admin".equals(role)) return true;
        if ("cs".equals(role)) {
            return t.getAssigneeId() != null && t.getAssigneeId().equals(operatorId);
        }
        if ("student".equals(role)) {
            return t.getCreatorId() != null && t.getCreatorId().equals(operatorId);
        }
        return false;
    }

    // 注意: 原来的方法签名里没有 role 参数, 我在接口里也没加.
    // 先假设所有调这些方法的都是有权限的 (演示场景),
    // 真正上线需要从登录上下文拿 role, 这里留 TODO 给组员 C 扩展.
    // 下面先用简化逻辑: cs 只能操作 assignee_id=自己的工单.

    /** 简化版: 检查工单已分配给指定客服 (或还没分配时允许认领) */
    private boolean canCsHandle(Ticket t, Long csId) {
        if (t == null) return false;
        // 已关闭的工单任何人都不能操作 (除了重新打开)
        if ("closed".equals(t.getStatus())) return false;
        // 已分配的, 只能分配的客服来操作
        if (t.getAssigneeId() != null) {
            return t.getAssigneeId().equals(csId);
        }
        // 还没分配 → 允许认领 (自动 assign)
        return true;
    }

    /** 简化版: 检查工单是指定学生创建的 */
    private boolean canStudentHandle(Ticket t, Long studentId) {
        if (t == null) return false;
        if (!"closed".equals(t.getStatus())) {
            // 只有学生自己创建的才能关闭
            return t.getCreatorId() != null && t.getCreatorId().equals(studentId);
        }
        // 已关闭的不允许学生再操作
        return false;
    }

    /** 简化版: 检查状态流转是否合法 */
    private boolean isStatusTransitionValid(String from, String to) {
        if (from == null || to == null) return false;
        if (from.equals(to)) return true;  // 同一个状态也算合法 (比如 admin 改优先级不换状态)
        switch (from) {
            case "open":              return "processing".equals(to) || "closed".equals(to);
            case "processing":        return "pending_customer".equals(to) || "closed".equals(to);
            case "pending_customer":  return "processing".equals(to) || "closed".equals(to);
            case "closed":            return false;  // closed 不能流转到任何状态
            default:                  return false;
        }
    }

    @Override
    public Ticket createTicket(Long creatorId, String title, String content, String category) {
        // TODO (C 组员):
        // 1) Ticket t = new Ticket(); 填字段, status=open, priority=medium 默认, emotion 先 neutral
        // 2) t = ticketDao.insert(t) 后拿到 id
        // 3) ticketLogService.writeLog(t.getId(), creatorId, "create", null, null, "学生创建工单")
        // 4) ⚠️ 调情感分析! emotion = AIUtil.analyzeSentiment(content + " " + title)
        //    如果 emotion = "negative" → 自动把 priority 升为 "urgent", 并写日志
        // 5) 再次 update(t) 把 emotion/priority 落库
        // 6) return t

        Ticket t = new Ticket();
        t.setTitle(title);
        t.setContent(content);
        t.setCategory(category);
        t.setStatus("open");
        t.setPriority("medium");
        t.setEmotion("neutral");
        t.setCreatorId(creatorId);

        long id = ticketDao.insert(t);
        t.setId(id);

        // 写 create 日志
        ticketLogService.writeLog(id, creatorId, "create", null, null, "学生创建工单");

        // ⚠️ 情感分析 (D 组员的 AI 能力, 组长 A 已经在 AIUtil 里封装好)
        String combined = (title == null ? "" : title) + " " + (content == null ? "" : content);
        String emotion = AIUtil.analyzeSentiment(combined);
        if (emotion != null) t.setEmotion(emotion);

        // 负面情绪 → 自动升级
        if ("negative".equals(emotion)) {
            String oldPri = t.getPriority();
            t.setPriority("urgent");
            ticketLogService.writeLog(id, creatorId, "update_priority",
                    oldPri, "urgent", "情感分析为负面, 自动升优先级");
        }

        // 更新 emotion 和可能变化的 priority 到 DB
        Ticket update = new Ticket();
        update.setId(id);
        update.setEmotion(t.getEmotion());
        update.setPriority(t.getPriority());
        ticketDao.update(update);

        return t;
    }

    @Override
    public boolean assignTicket(Long ticketId, Long assigneeId, Long operatorId) {
        Ticket t = ticketDao.selectById(ticketId);
        if (t == null) return false;
        if ("closed".equals(t.getStatus())) {
            System.err.println("工单已关闭, 不能再分配");
            return false;
        }

        String oldStatus = t.getStatus();
        Ticket update = new Ticket();
        update.setId(ticketId);
        update.setAssigneeId(assigneeId);
        if ("open".equals(oldStatus)) update.setStatus("processing");
        ticketDao.update(update);

        ticketLogService.writeLog(ticketId, operatorId, "assign",
                t.getAssigneeId() == null ? "null" : t.getAssigneeId().toString(),
                assigneeId.toString(), "分配工单给客服");
        return true;
    }

    @Override
    public boolean startProcessing(Long ticketId, Long operatorId) {
        Ticket t = ticketDao.selectById(ticketId);
        if (t == null) return false;
        // 状态校验: closed 不能处理, 已经 processing 也不需要再 start
        if ("closed".equals(t.getStatus())) {
            System.err.println("工单已关闭, 不能再处理");
            return false;
        }
        if ("processing".equals(t.getStatus())) {
            System.out.println("工单已经在处理中");
            return false;
        }
        String old = t.getStatus();
        Ticket update = new Ticket();
        update.setId(ticketId);
        update.setStatus("processing");
        ticketDao.update(update);
        ticketLogService.writeLog(ticketId, operatorId, "process", old, "processing", "客服开始处理");
        return true;
    }

    @Override
    public boolean replyToCustomer(Long ticketId, Long operatorId, String message) {
        Ticket t = ticketDao.selectById(ticketId);
        if (t == null) return false;
        if ("closed".equals(t.getStatus())) {
            System.err.println("工单已关闭, 不能回复");
            return false;
        }
        // 允许 processing 和 pending_customer 状态都能回复 (回到 pending_customer)
        String old = t.getStatus();
        Ticket update = new Ticket();
        update.setId(ticketId);
        update.setStatus("pending_customer");
        ticketDao.update(update);
        ticketLogService.writeLog(ticketId, operatorId, "reply", old, "pending_customer",
                "客服回复: " + (message.length() > 50 ? message.substring(0, 50) + "..." : message));
        return true;
    }

    @Override
    public boolean closeTicket(Long ticketId, Long operatorId) {
        Ticket t = ticketDao.selectById(ticketId);
        if (t == null) return false;
        if ("closed".equals(t.getStatus())) {
            System.out.println("工单已关闭, 无需重复关闭");
            return true;
        }
        String old = t.getStatus();
        Ticket update = new Ticket();
        update.setId(ticketId);
        update.setStatus("closed");
        update.setCloseTime(LocalDateTime.now());
        ticketDao.update(update);
        ticketLogService.writeLog(ticketId, operatorId, "close", old, "closed", "工单已关闭");
        return true;
    }

    @Override
    public boolean reopenTicket(Long ticketId, Long operatorId) {
        Ticket t = ticketDao.selectById(ticketId);
        if (t == null) return false;
        if (!"closed".equals(t.getStatus()) && !"pending_customer".equals(t.getStatus())) {
            System.err.println("只有 closed 或 pending_customer 状态的工单可以重新打开");
            return false;
        }
        String old = t.getStatus();
        Ticket update = new Ticket();
        update.setId(ticketId);
        update.setStatus("processing");
        ticketDao.update(update);
        ticketLogService.writeLog(ticketId, operatorId, "reopen", old, "processing", "重新打开工单");
        return true;
    }

    @Override
    public boolean updatePriority(Long ticketId, String priority, Long operatorId) {
        Ticket t = ticketDao.selectById(ticketId);
        if (t == null) return false;
        String old = t.getPriority();
        Ticket update = new Ticket();
        update.setId(ticketId);
        update.setPriority(priority);
        ticketDao.update(update);
        ticketLogService.writeLog(ticketId, operatorId, "update_priority", old, priority, "修改优先级");
        return true;
    }

    // ============ 查询 (组长 A 已经在 DAO 里写好) ============

    @Override
    public Ticket getById(Long id) { return ticketDao.selectById(id); }

    @Override
    public List<Ticket> listMyTickets(Long creatorId) { return ticketDao.selectByCreatorId(creatorId); }

    @Override
    public List<Ticket> listMyAssigned(Long assigneeId) { return ticketDao.selectByAssigneeId(assigneeId); }

    @Override
    public List<Ticket> searchTickets(String titleKeyword, String category,
                                      String status, String priority,
                                      int page, int size) {
        return ticketDao.selectByConditions(titleKeyword, category, status, priority, page, size);
    }

    @Override
    public long searchCount(String titleKeyword, String category,
                           String status, String priority) {
        return ticketDao.countByConditions(titleKeyword, category, status, priority);
    }

    @Override
    public Ticket getWithUserNames(Long id) { return ticketDao.selectWithUserNamesById(id); }
}
