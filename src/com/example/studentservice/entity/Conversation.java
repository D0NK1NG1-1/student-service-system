package com.example.studentservice.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI 对话 / 消息记录 — 对应 conversation 表
 */
public class Conversation {
    private Long id;
    /** 关联工单 ID, 自助问答时可空 */
    private Long ticketId;
    /** 发起用户 ID */
    private Long userId;
    /** user / assistant */
    private String role;
    private String content;
    /** faq / llm / manual */
    private String source;
    /** AI 置信度 0.00 - 1.00 */
    private BigDecimal confidence;
    /** 是否需要转人工: 1是 0否 */
    private Integer needHuman;
    private LocalDateTime createTime;

    public Conversation() {}

    // ---- getter / setter ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }
    public Integer getNeedHuman() { return needHuman; }
    public void setNeedHuman(Integer needHuman) { this.needHuman = needHuman; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    @Override
    public String toString() {
        return "Conversation{id=" + id + ", ticketId=" + ticketId + ", role='" + role
                + "', content='" + (content == null ? "" : content.substring(0, Math.min(30, content.length())))
                + "', source='" + source + "'}";
    }
}
