package com.example.studentservice.dao.impl;

import com.example.studentservice.dao.ConversationDao;
import com.example.studentservice.entity.Conversation;
import com.example.studentservice.util.DBUtil;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 对话 DAO — 组长 A 写
 */
public class ConversationDaoImpl implements ConversationDao {

    @Override
    public long insert(Conversation c) {
        String sql = "INSERT INTO conversation (ticket_id, user_id, role, content, source, confidence, need_human) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setObject(1, c.getTicketId());   // Long → nullable
            ps.setLong(2, c.getUserId());
            ps.setString(3, c.getRole());
            ps.setString(4, c.getContent());
            ps.setString(5, c.getSource());
            ps.setObject(6, c.getConfidence());
            ps.setInt(7, c.getNeedHuman() != null ? c.getNeedHuman() : 0);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return -1;
    }

    @Override
    public List<Conversation> selectByTicketId(Long ticketId) {
        String sql = "SELECT * FROM conversation WHERE ticket_id = ? ORDER BY create_time ASC";
        return queryList(sql, ticketId);
    }

    @Override
    public List<Conversation> selectByUserId(Long userId) {
        String sql = "SELECT * FROM conversation WHERE user_id = ? ORDER BY create_time DESC";
        return queryList(sql, userId);
    }

    @Override
    public List<Conversation> selectRecentByTicketId(Long ticketId, int n) {
        String sql = "SELECT * FROM conversation WHERE ticket_id = ? ORDER BY create_time DESC LIMIT ?";
        List<Conversation> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            ps.setInt(2, n);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {}
        // 翻转一下, 让结果按时间 ASC
        java.util.Collections.reverse(list);
        return list;
    }

    @Override
    public List<Object[]> countBySource() {
        String sql = "SELECT source, COUNT(*) AS cnt FROM conversation GROUP BY source";
        return DBUtil.runGroupBy(sql);
    }

    @Override
    public long countByTicketId(Long ticketId) {
        String sql = "SELECT COUNT(*) FROM conversation WHERE ticket_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {}
        return 0;
    }

    private List<Conversation> queryList(String sql, Object... args) {
        List<Conversation> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) {}
        return list;
    }

    private Conversation map(ResultSet rs) throws SQLException {
        Conversation c = new Conversation();
        c.setId(rs.getLong("id"));
        Long tid = DBUtil.rsLong(rs, "ticket_id");
        c.setTicketId(tid);
        c.setUserId(rs.getLong("user_id"));
        c.setRole(DBUtil.rsStr(rs, "role"));
        c.setContent(DBUtil.rsStr(rs, "content"));
        c.setSource(DBUtil.rsStr(rs, "source"));
        BigDecimal bd = rs.getBigDecimal("confidence");
        c.setConfidence(bd);
        c.setNeedHuman(rs.getInt("need_human"));
        Timestamp t = rs.getTimestamp("create_time");
        if (t != null) c.setCreateTime(t.toLocalDateTime());
        return c;
    }
}
