package com.example.studentservice.dao;

import com.example.studentservice.entity.TicketLog;
import java.util.List;

/**
 * 工单日志 DAO 接口 · 组长 A 定义
 * <p>
 * append-only 表, 只 insert + select, 没有 update/delete.
 * 任何工单状态变更都必须写日志.
 */
public interface TicketLogDao {

    /** 新增一条日志 */
    long insert(TicketLog log);

    /** 按工单查全部日志 (按 create_time ASC) */
    List<TicketLog> selectByTicketId(Long ticketId);

    /** 按操作人查日志 */
    List<TicketLog> selectByOperatorId(Long operatorId);

    /** 按动作类型查 (create/assign/close...) */
    List<TicketLog> selectByAction(String action);

    /** 统计近 N 天各 action 的次数 */
    List<Object[]> countByAction();
}
