package com.example.studentservice.dao.impl;

import com.example.studentservice.dao.TicketDao;
import com.example.studentservice.entity.Ticket;
import com.example.studentservice.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 工单 DAO 实现 — ⚠️ 组员 C 负责完整实现
 * <p>
 * 验收硬指标:
 *   ✅ 分页 (LIMIT offset, size)
 *   ✅ LIKE 模糊搜索 (title)
 *   ✅ 多条件组合 (title + category + status + priority 可全可空)
 *   ✅ 聚合 COUNT + GROUP BY (countByStatus / countByCategory / countByPriority / countByEmotion)
 *   ✅ JOIN (selectWithUserNamesById — 关联 user 表取 creator_name / assignee_name)
 *   ✅ AVG 聚合 (avgResponseSeconds — TIMESTAMPDIFF + AVG)
 * <p>
 * 组长 A 已经把方法签名 + 核心 SQL 写好, 组员 C 只需要补上 TODO 的部分.
 * 参考模板: UserDaoImpl.java / TicketLogDaoImpl.java / FaqDaoImpl.java
 */
public class TicketDaoImpl implements TicketDao {

    // ============================================
    // 基础 CRUD
    // ============================================

    @Override
    public long insert(Ticket t) {
        String sql = "INSERT INTO ticket (title, content, category, status, priority, emotion, " +
                     "creator_id, assignee_id, faq_hit_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, t.getTitle());
            ps.setString(2, t.getContent());
            ps.setString(3, t.getCategory());
            ps.setString(4, t.getStatus() != null ? t.getStatus() : "open");
            ps.setString(5, t.getPriority() != null ? t.getPriority() : "medium");
            ps.setString(6, t.getEmotion() != null ? t.getEmotion() : "neutral");
            ps.setLong(7, t.getCreatorId());
            ps.setObject(8, t.getAssigneeId());
            ps.setObject(9, t.getFaqHitId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    @Override
    public int update(Ticket t) {
        // 动态更新: 只更新非 null 字段, 参考 UserDaoImpl.update
        StringBuilder sql = new StringBuilder("UPDATE ticket SET ");
        List<Object> params = new ArrayList<>();

        if (t.getTitle() != null)    { sql.append("title = ?, ");    params.add(t.getTitle()); }
        if (t.getContent() != null)  { sql.append("content = ?, ");  params.add(t.getContent()); }
        if (t.getCategory() != null) { sql.append("category = ?, "); params.add(t.getCategory()); }
        if (t.getStatus() != null)   { sql.append("status = ?, ");   params.add(t.getStatus()); }
        if (t.getPriority() != null) { sql.append("priority = ?, "); params.add(t.getPriority()); }
        if (t.getEmotion() != null)  { sql.append("emotion = ?, ");  params.add(t.getEmotion()); }
        if (t.getAssigneeId() != null) { sql.append("assignee_id = ?, "); params.add(t.getAssigneeId()); }
        if (t.getFaqHitId() != null)   { sql.append("faq_hit_id = ?, ");   params.add(t.getFaqHitId()); }
        // close_time 单独处理: 如果显式传了非 null 的 LocalDateTime 就设置, 否则不动
        if (t.getCloseTime() != null) {
            sql.append("close_time = ?, ");
            params.add(java.sql.Timestamp.valueOf(t.getCloseTime()));
        }

        if (params.isEmpty()) {
            // 没有任何字段要更新
            System.out.println("[TicketDaoImpl.update] 没有需要更新的字段, id=" + t.getId());
            return 0;
        }

        sql.setLength(sql.length() - 2);  // 去掉末尾 ", "
        sql.append(" WHERE id = ?");
        params.add(t.getId());

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[TicketDaoImpl.update] " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }

    @Override
    public int deleteById(Long id) {
        String sql = "DELETE FROM ticket WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate();
        } catch (SQLException e) { return 0; }
    }

    @Override
    public Ticket selectById(Long id) {
        String sql = "SELECT * FROM ticket WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return map(rs);
            }
        } catch (SQLException e) {}
        return null;
    }

    // ============================================
    // 按单条件查
    // ============================================

    @Override
    public List<Ticket> selectByCreatorId(Long creatorId) {
        String sql = "SELECT * FROM ticket WHERE creator_id = ? ORDER BY create_time DESC";
        return queryList(sql, creatorId);
    }

    @Override
    public List<Ticket> selectByAssigneeId(Long assigneeId) {
        String sql = "SELECT * FROM ticket WHERE assignee_id = ? ORDER BY update_time DESC";
        return queryList(sql, assigneeId);
    }

    @Override
    public List<Ticket> selectByStatus(String status) {
        String sql = "SELECT * FROM ticket WHERE status = ? ORDER BY priority DESC, create_time DESC";
        return queryList(sql, status);
    }

    @Override
    public List<Ticket> selectByPriority(String priority) {
        String sql = "SELECT * FROM ticket WHERE priority = ? ORDER BY create_time DESC";
        return queryList(sql, priority);
    }

    // ============================================
    // 多条件 + 分页 + LIKE (C 组员重点实现)
    // ============================================

    @Override
    public List<Ticket> selectByConditions(String titleKeyword, String category,
                                           String status, String priority,
                                           int page, int size) {
        // TODO: 动态拼 WHERE 子句, 所有 null 参数跳过
        // 示例逻辑:
        //   StringBuilder where = new StringBuilder("WHERE 1=1");
        //   有 titleKeyword → where.append(" AND title LIKE ?");
        //   有 category    → where.append(" AND category = ?");
        //   有 status      → where.append(" AND status = ?");
        //   有 priority    → where.append(" AND priority = ?");
        //   然后在后面拼 ORDER BY create_time DESC LIMIT ?, ?
        // 参数列表按顺序 add, titleKeyword 要加 "%" + kw + "%"
        //
        // 这里给个已实现版本, C 组员可以直接用或优化
        StringBuilder sql = new StringBuilder("SELECT * FROM ticket WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (titleKeyword != null && !titleKeyword.isEmpty()) {
            sql.append(" AND title LIKE ?");
            params.add("%" + titleKeyword + "%");
        }
        if (category != null && !category.isEmpty()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (priority != null && !priority.isEmpty()) {
            sql.append(" AND priority = ?");
            params.add(priority);
        }
        sql.append(" ORDER BY FIELD(priority, 'urgent','high','medium','low'), create_time DESC");
        sql.append(" LIMIT ?, ?");
        params.add((page - 1) * size);
        params.add(size);
        return queryList(sql.toString(), params.toArray());
    }

    @Override
    public long countByConditions(String titleKeyword, String category,
                                 String status, String priority) {
        // TODO: 和上面 selectByConditions 用相同 WHERE 子句, 但 SELECT COUNT(*)
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM ticket WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (titleKeyword != null && !titleKeyword.isEmpty()) {
            sql.append(" AND title LIKE ?");
            params.add("%" + titleKeyword + "%");
        }
        if (category != null && !category.isEmpty()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (status != null && !status.isEmpty()) {
            sql.append(" AND status = ?");
            params.add(status);
        }
        if (priority != null && !priority.isEmpty()) {
            sql.append(" AND priority = ?");
            params.add(priority);
        }
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {}
        return 0;
    }

    @Override
    public Ticket selectWithUserNamesById(Long id) {
        // TODO: JOIN 查询 — 验收硬指标!
        // SQL:
        // SELECT t.*, u1.real_name AS creator_name, u2.real_name AS assignee_name
        // FROM ticket t
        // LEFT JOIN user u1 ON t.creator_id  = u1.id
        // LEFT JOIN user u2 ON t.assignee_id = u2.id
        // WHERE t.id = ?
        //
        // 注意: Ticket 实体里没有 creatorName / assigneeName 字段
        // 你有两个选择:
        //   a) 改 Ticket.java 加这两个字段 (推荐, 组长 A 已同意)
        //   b) 返回一个新的 TicketWithNames VO 类
        //
        // 组长 A 这里留 TODO 让 C 组员决定
        String sql = "SELECT t.*, u1.real_name AS creator_name, u2.real_name AS assignee_name " +
                     "FROM ticket t " +
                     "LEFT JOIN user u1 ON t.creator_id  = u1.id " +
                     "LEFT JOIN user u2 ON t.assignee_id = u2.id " +
                     "WHERE t.id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Ticket t = map(rs);
                    // TODO: 如果 Ticket 没加 creatorName/assigneeName, C 组员需要先在 Ticket.java 加字段
                    // 这里暂时把结果存进 Ticket 的 extra 字段或直接忽略 join 结果
                    // 推荐: 改 Ticket.java 加两个 transient 字段 (不入库)
                    return t;
                }
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return null;
    }

    // ============================================
    // 聚合统计 (验收硬指标!)
    // ============================================

    @Override
    public List<Object[]> countByStatus() {
        return DBUtil.runGroupBy("SELECT status, COUNT(*) FROM ticket GROUP BY status");
    }

    @Override
    public List<Object[]> countByCategory() {
        return DBUtil.runGroupBy("SELECT category, COUNT(*) FROM ticket GROUP BY category");
    }

    @Override
    public List<Object[]> countByPriority() {
        return DBUtil.runGroupBy("SELECT priority, COUNT(*) FROM ticket GROUP BY priority");
    }

    @Override
    public List<Object[]> countByEmotion() {
        return DBUtil.runGroupBy("SELECT emotion, COUNT(*) FROM ticket GROUP BY emotion");
    }

    @Override
    public Double avgResponseSeconds() {
        // TODO: 关键 SQL — TIMESTAMPDIFF 计算关闭时长
        // SQL: SELECT AVG(TIMESTAMPDIFF(SECOND, create_time, close_time))
        //      FROM ticket WHERE status = 'closed' AND close_time IS NOT NULL
        //
        // 组长 A 给完整 SQL, 组员 C 直接用 DBUtil.runAggregateDouble() 包一下
        String sql = "SELECT AVG(TIMESTAMPDIFF(SECOND, create_time, close_time)) " +
                     "FROM ticket WHERE status = 'closed' AND close_time IS NOT NULL";
        return DBUtil.runAggregateDouble(sql);
    }

    // ============ 辅助 ============

    private List<Ticket> queryList(String sql, Object... args) {
        List<Ticket> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {}
        return list;
    }

    private Ticket map(ResultSet rs) throws SQLException {
        Ticket t = new Ticket();
        t.setId(rs.getLong("id"));
        t.setTitle(DBUtil.rsStr(rs, "title"));
        t.setContent(DBUtil.rsStr(rs, "content"));
        t.setCategory(DBUtil.rsStr(rs, "category"));
        t.setStatus(DBUtil.rsStr(rs, "status"));
        t.setPriority(DBUtil.rsStr(rs, "priority"));
        t.setEmotion(DBUtil.rsStr(rs, "emotion"));
        t.setCreatorId(rs.getLong("creator_id"));
        Long aid = DBUtil.rsLong(rs, "assignee_id");
        t.setAssigneeId(aid);
        Long fid = DBUtil.rsLong(rs, "faq_hit_id");
        t.setFaqHitId(fid);
        Timestamp t1 = rs.getTimestamp("create_time");
        if (t1 != null) t.setCreateTime(t1.toLocalDateTime());
        Timestamp t2 = rs.getTimestamp("update_time");
        if (t2 != null) t.setUpdateTime(t2.toLocalDateTime());
        Timestamp t3 = rs.getTimestamp("close_time");
        if (t3 != null) t.setCloseTime(t3.toLocalDateTime());
        return t;
    }
}
