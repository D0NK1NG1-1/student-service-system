package com.example.studentservice.entity;

import java.time.LocalDateTime;

/**
 * 用户实体 — 对应 user 表
 * 三种角色: student(学生) / cs(客服) / admin(管理员)
 */
public class User {
    private Long id;
    private String username;
    private String password;
    private String realName;
    /** 角色: student / cs / admin */
    private String role;
    private String email;
    private String phone;
    /** 1启用 0禁用 */
    private Integer status;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public User() {}

    // 常用便捷构造
    public User(Long id, String username, String password, String realName, String role) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.realName = realName;
        this.role = role;
        this.status = 1;
    }

    // ---- getter / setter ----
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }

    /** 判断是否某角色 */
    public boolean isStudent() { return "student".equals(role); }
    public boolean isCs()      { return "cs".equals(role); }
    public boolean isAdmin()   { return "admin".equals(role); }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', realName='" + realName
                + "', role='" + role + "', email='" + email + "'}";
    }
}
