package com.example.studentservice.entity;

import java.time.LocalDateTime;

/**
 * 工单实体 — 对应 ticket 表
 */
public class Ticket {
    private Long id;
    private String title;
    private String content;
    private String category;
    /** open/processing/pending_customer/closed */
    private String status;
    /** low/medium/high/urgent */
    private String priority;
    /** positive/neutral/negative */
    private String emotion;
    /** 创建人(学生) user.id */
    private Long creatorId;
    /** 处理人(客服) user.id, 可空 */
    private Long assigneeId;
    /** 首次命中的 FAQ id, 可空 */
    private Long faqHitId;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private LocalDateTime closeTime;

    public Ticket() {}

    // ---- getter / setter ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public String getEmotion() { return emotion; }
    public void setEmotion(String emotion) { this.emotion = emotion; }
    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }
    public Long getAssigneeId() { return assigneeId; }
    public void setAssigneeId(Long assigneeId) { this.assigneeId = assigneeId; }
    public Long getFaqHitId() { return faqHitId; }
    public void setFaqHitId(Long faqHitId) { this.faqHitId = faqHitId; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public LocalDateTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalDateTime closeTime) { this.closeTime = closeTime; }

    @Override
    public String toString() {
        return "Ticket{id=" + id + ", title='" + title + "', status='" + status
                + "', priority='" + priority + "', emotion='" + emotion
                + "', creator=" + creatorId + ", assignee=" + assigneeId + "}";
    }
}
