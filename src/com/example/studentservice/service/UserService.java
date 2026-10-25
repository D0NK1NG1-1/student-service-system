package com.example.studentservice.service;

import com.example.studentservice.entity.User;
import java.util.List;

/**
 * 用户 Service 接口 · 组长 A 定义, 组员 B 实现
 * <p>
 * Service 负责业务校验 + 调 DAO. 不允许在 ServiceImpl 里写 SQL.
 * 所有实现统一注入 UserDao (可先用 new UserDaoImpl(), 后续如果加 Spring 再改).
 */
public interface UserService {

    /**
     * 注册新用户. 校验: username 不能重复.
     * @throws IllegalArgumentException 用户名已存在
     */
    User register(String username, String password, String role, String realName);

    /**
     * 登录校验. 返回 null 表示账号或密码错误
     */
    User login(String username, String password);

    /** 更新个人信息 (密码/邮箱/手机) */
    boolean updateProfile(User user);

    /** 管理员: 创建账号 */
    User createUser(User user);

    /** 管理员: 禁用/启用账号 */
    boolean updateStatus(Long id, int status);

    User getById(Long id);
    List<User> listByRole(String role);

    /** 按角色查询, 包含已禁用账号 (管理端用户管理列表用, 保证禁用后还能找回) */
    List<User> listByRoleIncludeDisabled(String role);
    List<User> listByRolePage(String role, int page, int size);
}
