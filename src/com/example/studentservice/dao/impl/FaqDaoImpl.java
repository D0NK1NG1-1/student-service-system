package com.example.studentservice.dao.impl;

import com.example.studentservice.dao.FaqDao;
import com.example.studentservice.entity.Faq;
import com.example.studentservice.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * FAQ DAO 实现 — ⚠️ 组员 B 负责完整实现
 * <p>
 * ✅ 组长 A 给了 try-with-resources 模板 (见 UserDaoImpl.java 和 TicketLogDaoImpl.java)
 * ✅ 组长 A 在 Dao 接口里写了完整方法签名和注释
 * ✅ DBUtil.rsStr() / runGroupBy() / buildPageSql() 都可以直接用
 * <p>
 * 验收硬指标:
 *   1) 6 种 SQL 全覆盖 (INSERT/DELETE/UPDATE/SELECT/分页/LIKE)
 *   2) LIKE 搜索要在 question 和 keywords 两列都做 '%keyword%'
 *   3) 分页用 MySQL LIMIT offset, size
 *   4) incrementHitCount 用 UPDATE faq SET hit_count = hit_count + 1 WHERE id = ?
 */
public class FaqDaoImpl implements FaqDao {

    // ============================================
    // ✅ TODO: 组员 B 请逐个实现, 参考 UserDaoImpl 的写法
    // ============================================

    @Override
    public long insert(Faq faq) {
        // TODO: INSERT INTO faq (question, answer, category, keywords, hit_count, status) VALUES (?,?,?,?,0,?)
        String sql = "INSERT INTO faq (question, answer, category, keywords, hit_count, status) " +
                     "VALUES (?, ?, ?, ?, 0, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, faq.getQuestion());
            ps.setString(2, faq.getAnswer());
            ps.setString(3, faq.getCategory());
            ps.setString(4, faq.getKeywords());
            ps.setInt(5, faq.getStatus() != null ? faq.getStatus() : 1);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    @Override
    public int update(Faq faq) {
        // TODO: 动态更新, 参考 UserDaoImpl.update 的写法
        return 0;
    }

    @Override
    public int deleteById(Long id) {
        // TODO: DELETE FROM faq WHERE id = ?
        String sql = "DELETE FROM faq WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) { return 0; }
    }

    @Override
    public Faq selectById(Long id) {
        // TODO: SELECT * FROM faq WHERE id = ?
        String sql = "SELECT * FROM faq WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {}
        return null;
    }

    @Override
    public List<Faq> selectAll() {
        // TODO: SELECT * FROM faq WHERE status = 1 ORDER BY hit_count DESC
        String sql = "SELECT * FROM faq WHERE status = 1 ORDER BY hit_count DESC";
        return queryList(sql);
    }

    @Override
    public List<Faq> selectByKeyword(String keyword) {
        // TODO: LIKE 搜索, question 和 keywords 都要匹配
        // SQL: SELECT * FROM faq WHERE status = 1 AND (question LIKE ? OR keywords LIKE ?)
        //      ORDER BY hit_count DESC
        String sql = "SELECT * FROM faq WHERE status = 1 " +
                     "AND (question LIKE ? OR keywords LIKE ?) ORDER BY hit_count DESC";
        List<Faq> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {}
        return list;
    }

    @Override
    public List<Faq> selectByCategory(String category) {
        String sql = "SELECT * FROM faq WHERE category = ? AND status = 1 ORDER BY create_time DESC";
        List<Faq> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {}
        return list;
    }

    @Override
    public List<Faq> selectPage(int page, int size) {
        String sql = "SELECT * FROM faq WHERE status = 1 ORDER BY create_time DESC LIMIT ?, ?";
        List<Faq> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, (page - 1) * size);
            ps.setInt(2, size);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {}
        return list;
    }

    @Override
    public long count() {
        String sql = "SELECT COUNT(*) FROM faq WHERE status = 1";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {}
        return 0;
    }

    @Override
    public List<Object[]> countByCategory() {
        String sql = "SELECT category, COUNT(*) AS cnt FROM faq WHERE status = 1 GROUP BY category";
        return DBUtil.runGroupBy(sql);
    }

    @Override
    public int incrementHitCount(Long id) {
        // TODO: UPDATE faq SET hit_count = hit_count + 1 WHERE id = ?
        String sql = "UPDATE faq SET hit_count = hit_count + 1 WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) { return 0; }
    }

    // ============ 辅助 ============

    private List<Faq> queryList(String sql, Object... args) {
        List<Faq> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {}
        return list;
    }

    private Faq map(ResultSet rs) throws SQLException {
        Faq f = new Faq();
        f.setId(rs.getLong("id"));
        f.setQuestion(DBUtil.rsStr(rs, "question"));
        f.setAnswer(DBUtil.rsStr(rs, "answer"));
        f.setCategory(DBUtil.rsStr(rs, "category"));
        f.setKeywords(DBUtil.rsStr(rs, "keywords"));
        f.setHitCount(rs.getInt("hit_count"));
        f.setStatus(rs.getInt("status"));
        Timestamp t1 = rs.getTimestamp("create_time");
        if (t1 != null) f.setCreateTime(t1.toLocalDateTime());
        Timestamp t2 = rs.getTimestamp("update_time");
        if (t2 != null) f.setUpdateTime(t2.toLocalDateTime());
        return f;
    }
}
