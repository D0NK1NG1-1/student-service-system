package com.example.studentservice.service;

import com.example.studentservice.entity.TicketLog;
import java.util.List;

/**
 * 工单日志 Service · 组长 A 自己实现 (给 B/C/D 组员复用)
 * <p>
 * 所有工单状态变更都必须调 writeLog, 不要直接调 DAO.
 */
public interface TicketLogService {

    /** 写一条日志 (内部自动填 create_time) */
    void writeLog(Long ticketId, Long operatorId, String action,
                  String oldValue, String newValue, String remark);

    /** 查某工单的全部日志 (按时间 ASC) */
    List<TicketLog> listByTicketId(Long ticketId);

    /** 查某操作人的全部日志 */
    List<TicketLog> listByOperatorId(Long operatorId);
}
