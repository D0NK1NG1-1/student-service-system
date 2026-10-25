package com.example.studentservice.entity;

import java.time.LocalDateTime;

/**
 * 常见问题 FAQ — 对应 faq 表
 */
public class Faq {
    private Long id;
    private String question;
    private String answer;
    private String category;
    /** 搜索关键词 (逗号分隔), LIKE 匹配用 */
    private String keywords;
    /** 被命中次数 */
    private Integer hitCount;
    /** 1启用 0禁用 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Faq() {}

    // ---- getter / setter ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getQuestion() { return question; }
    public void setQuestion(String question) { this.question = question; }
    public String getAnswer() { return answer; }
    public void setAnswer(String answer) { this.answer = answer; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getKeywords() { return keywords; }
    public void setKeywords(String keywords) { this.keywords = keywords; }
    public Integer getHitCount() { return hitCount; }
    public void setHitCount(Integer hitCount) { this.hitCount = hitCount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    @Override
    public String toString() {
        return "Faq{id=" + id + ", question='" + question + "', category='" + category
                + "', hitCount=" + hitCount + "}";
    }
}
