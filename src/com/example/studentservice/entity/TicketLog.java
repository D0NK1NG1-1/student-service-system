package com.example.studentservice.entity;

import java.time.LocalDateTime;

/**
 * 工单操作日志 — 对应 ticket_log 表
 */
public class TicketLog {
    private Long id;
    private Long ticketId;
    /** 操作人 user.id */
    private Long operatorId;
    /** create / assign / process / reply / close / reopen / update_priority */
    private String action;
    private String oldValue;
    private String newValue;
    private String remark;
    private LocalDateTime createTime;

    public TicketLog() {}

    // ---- getter / setter ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    @Override
    public String toString() {
        return "TicketLog{id=" + id + ", ticketId=" + ticketId + ", operator=" + operatorId
                + ", action='" + action + "'}";
    }
}
