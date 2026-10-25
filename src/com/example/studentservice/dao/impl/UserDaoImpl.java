package com.example.studentservice.dao.impl;

import com.example.studentservice.dao.UserDao;
import com.example.studentservice.entity.User;
import com.example.studentservice.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户 DAO 实现 — 组长 A 完整写, 给 B 组员当模板
 * <p>
 * ✅ INSERT / DELETE / UPDATE / SELECT / 分页 / 聚合 全覆盖
 * B 组员写自己的 FaqDaoImpl / TicketDaoImpl 时直接照着 try-with-resources 结构.
 */
public class UserDaoImpl implements UserDao {

    @Override
    public long insert(User user) {
        String sql = "INSERT INTO user (username, password, real_name, role, email, phone, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getRealName());
            ps.setString(4, user.getRole());
            ps.setString(5, user.getEmail());
            ps.setString(6, user.getPhone());
            ps.setInt(7, user.getStatus() != null ? user.getStatus() : 1);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            System.err.println("[UserDaoImpl.insert] " + e.getMessage());
        }
        return -1;
    }

    @Override
    public int update(User user) {
        // 动态更新: 只更新非 null 字段
        StringBuilder sql = new StringBuilder("UPDATE user SET ");
        List<Object> params = new ArrayList<>();
        if (user.getPassword() != null)    { sql.append("password = ?, ");   params.add(user.getPassword()); }
        if (user.getRealName() != null)    { sql.append("real_name = ?, ");   params.add(user.getRealName()); }
        if (user.getEmail() != null)       { sql.append("email = ?, ");      params.add(user.getEmail()); }
        if (user.getPhone() != null)       { sql.append("phone = ?, ");      params.add(user.getPhone()); }
        if (user.getStatus() != null)      { sql.append("status = ?, ");     params.add(user.getStatus()); }
        sql.setLength(sql.length() - 2);  // 去掉末尾 ", "
        sql.append(" WHERE id = ?");
        params.add(user.getId());

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[UserDaoImpl.update] " + e.getMessage());
            return 0;
        }
    }

    @Override
    public int deleteById(Long id) {
        String sql = "DELETE FROM user WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) { return 0; }
    }

    @Override
    public User selectById(Long id) {
        String sql = "SELECT * FROM user WHERE id = ?";
        return selectOne(sql, id);
    }

    @Override
    public List<User> selectAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM user ORDER BY id ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) {}
        return list;
    }

    @Override
    public User selectByUsername(String username) {
        // 不过滤 status: 用户名是唯一键, 登录时由上层判断是否被禁用并给出明确提示
        String sql = "SELECT * FROM user WHERE username = ?";
        return selectOne(sql, username);
    }

    @Override
    public List<User> selectByRole(String role) {
        String sql = "SELECT * FROM user WHERE role = ? AND status = 1 ORDER BY id ASC";
        return selectList(sql, role);
    }

    @Override
    public List<User> selectByRoleIncludeDisabled(String role) {
        // 管理端用户列表: 禁用的也要列出来, 否则禁用后无法恢复
        String sql = "SELECT * FROM user WHERE role = ? ORDER BY status DESC, id ASC";
        return selectList(sql, role);
    }

    @Override
    public List<User> selectByRolePage(String role, int page, int size) {
        String sql = "SELECT * FROM user WHERE role = ? AND status = 1 ORDER BY id ASC LIMIT ?, ?";
        List<User> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setInt(2, (page - 1) * size);
            ps.setInt(3, size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {}
        return list;
    }

    @Override
    public long countByRole(String role) {
        String sql = "SELECT COUNT(*) FROM user WHERE role = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {}
        return 0;
    }

    // ============ 内部辅助方法 ============

    private User selectOne(String sql, Object... args) {
        List<User> list = selectList(sql, args);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<User> selectList(String sql, Object... args) {
        List<User> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(DBUtil.rsStr(rs, "username"));
        u.setPassword(DBUtil.rsStr(rs, "password"));
        u.setRealName(DBUtil.rsStr(rs, "real_name"));
        u.setRole(DBUtil.rsStr(rs, "role"));
        u.setEmail(DBUtil.rsStr(rs, "email"));
        u.setPhone(DBUtil.rsStr(rs, "phone"));
        u.setStatus(rs.getInt("status"));
        Timestamp t = rs.getTimestamp("create_time");
        if (t != null) u.setCreateTime(t.toLocalDateTime());
        Timestamp t2 = rs.getTimestamp("update_time");
        if (t2 != null) u.setUpdateTime(t2.toLocalDateTime());
        return u;
    }
}
