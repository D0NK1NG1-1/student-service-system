package com.example.studentservice.service.impl;

import com.example.studentservice.dao.TicketLogDao;
import com.example.studentservice.dao.impl.TicketLogDaoImpl;
import com.example.studentservice.entity.TicketLog;
import com.example.studentservice.service.TicketLogService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 工单日志 Service — 组长 A 完整写
 * <p>
 * 全组 Service 的基础设施, 任何工单状态变更都必须调 writeLog.
 */
public class TicketLogServiceImpl implements TicketLogService {

    private final TicketLogDao dao = new TicketLogDaoImpl();

    @Override
    public void writeLog(Long ticketId, Long operatorId, String action,
                         String oldValue, String newValue, String remark) {
        TicketLog log = new TicketLog();
        log.setTicketId(ticketId);
        log.setOperatorId(operatorId);
        log.setAction(action);
        log.setOldValue(oldValue);
        log.setNewValue(newValue);
        log.setRemark(remark);
        dao.insert(log);
        // 成功返回就不打日志了, 失败 DAO 自己会 e.printStackTrace()
    }

    @Override
    public List<TicketLog> listByTicketId(Long ticketId) {
        return dao.selectByTicketId(ticketId);
    }

    @Override
    public List<TicketLog> listByOperatorId(Long operatorId) {
        return dao.selectByOperatorId(operatorId);
    }
}
