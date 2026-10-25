package com.example.studentservice.dao;

import com.example.studentservice.entity.User;
import java.util.List;

/**
 * 用户 DAO 接口 · 组长 A 定义, 组员 B 在 dao.impl 里实现
 * <p>
 * 所有实现统一用 {@code DBUtil.getConnection()} + PreparedStatement + try-with-resources.
 * 必须用占位符 ? , 禁止字符串拼 SQL (防注入).
 */
public interface UserDao {

    // ---- 基础 CRUD ----

    /** 新增用户, 返回自增 id */
    long insert(User user);

    /** 更新用户 (id 必填, 按 null 字段自动跳过) */
    int update(User user);

    /** 按 id 删除 */
    int deleteById(Long id);

    /** 按 id 查询 */
    User selectById(Long id);

    /** 查询全部 (不分页, 测试用) */
    List<User> selectAll();

    // ---- 业务查询 ----

    /**
     * 按登录账号查 (登录时用, 返回 null 表示不存在).
     * <p>
     * ⚠️ 注意: 这里**不过滤 status**. 用户名是唯一键, 查人就应该查得到;
     * 是否被禁用由 Service/UI 层判断后给用户明确提示 ("账号已被禁用"),
     * 否则禁用账号登录只会笼统提示"用户名或密码错误", 而且管理端也看不到它、无法恢复.
     */
    User selectByUsername(String username);

    /** 按角色查询 (student/cs/admin), 只返回启用账号 */
    List<User> selectByRole(String role);

    /**
     * 按角色查询, **包含已禁用账号** —— 管理端用户管理列表专用.
     * <p>
     * 如果这里也过滤掉 status=0, 管理员禁用一个账号后它就彻底从列表消失,
     * 再也没有入口把它恢复 (之前"禁用自己后无法恢复"的 bug 就是这么来的).
     */
    List<User> selectByRoleIncludeDisabled(String role);

    /** 按角色分页查询 */
    List<User> selectByRolePage(String role, int page, int size);

    /** 统计指定角色的人数 */
    long countByRole(String role);
}
