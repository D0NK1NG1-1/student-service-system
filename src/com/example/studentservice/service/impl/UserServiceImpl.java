package com.example.studentservice.service.impl;

import com.example.studentservice.dao.UserDao;
import com.example.studentservice.dao.impl.UserDaoImpl;
import com.example.studentservice.entity.User;
import com.example.studentservice.service.UserService;

import java.util.List;

/**
 * 用户 Service 实现 — ⚠️ 组员 B 负责完整实现
 * <p>
 * 组长 A 给了完整的方法签名 + TODO + 实现思路提示.
 * UserDaoImpl 已经完整写好, B 组员只需要调 DAO 即可.
 * <p>
 * 业务要点:
 *   - register 时先 selectByUsername 检查重复, 重复则抛 IllegalArgumentException
 *   - login 时 selectByUsername + 比对 password, 失败返回 null
 *   - 所有 Service 方法只做校验 + 调 DAO, 不要写 SQL
 */
public class UserServiceImpl implements UserService {

    private final UserDao userDao = new UserDaoImpl();

    @Override
    public User register(String username, String password, String role, String realName) {
        // TODO (B 组员):
        // 1) if (userDao.selectByUsername(username) != null) throw new IllegalArgumentException("用户名已存在")
        // 2) User u = new User(); u.setXxx(...); u.setStatus(1)
        // 3) userDao.insert(u);
        // 4) return u
        if (userDao.selectByUsername(username) != null) {
            throw new IllegalArgumentException("用户名已存在: " + username);
        }
        User u = new User();
        u.setUsername(username);
        u.setPassword(password);  // 作业不做 MD5, 直接明文
        u.setRole(role);
        u.setRealName(realName);
        u.setStatus(1);
        long id = userDao.insert(u);
        u.setId(id);
        return u;
    }

    @Override
    public User login(String username, String password) {
        // TODO (B 组员):
        // 1) User u = userDao.selectByUsername(username)
        // 2) if (u != null && u.getPassword().equals(password)) return u
        // 3) return null
        User u = userDao.selectByUsername(username);
        if (u != null && u.getPassword() != null && u.getPassword().equals(password)) {
            return u;
        }
        return null;
    }

    @Override
    public boolean updateProfile(User user) {
        // TODO (B 组员): 调 userDao.update(user), 返回 > 0
        return userDao.update(user) > 0;
    }

    @Override
    public User createUser(User user) {
        // TODO (B 组员): admin 创建账号, 先检查 username 重复
        if (userDao.selectByUsername(user.getUsername()) != null) {
            throw new IllegalArgumentException("用户名已存在: " + user.getUsername());
        }
        user.setStatus(1);
        long id = userDao.insert(user);
        user.setId(id);
        return user;
    }

    @Override
    public boolean updateStatus(Long id, int status) {
        // TODO (B 组员): new User(); u.setId(id); u.setStatus(status); userDao.update(u)
        User u = new User();
        u.setId(id);
        u.setStatus(status);
        return userDao.update(u) > 0;
    }

    @Override
    public User getById(Long id) { return userDao.selectById(id); }

    @Override
    public List<User> listByRole(String role) { return userDao.selectByRole(role); }

    @Override
    public List<User> listByRoleIncludeDisabled(String role) {
        return userDao.selectByRoleIncludeDisabled(role);
    }

    @Override
    public List<User> listByRolePage(String role, int page, int size) {
        return userDao.selectByRolePage(role, page, size);
    }
}
