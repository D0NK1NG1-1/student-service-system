package com.example.studentservice.dao.impl;

import com.example.studentservice.dao.TicketLogDao;
import com.example.studentservice.entity.TicketLog;
import com.example.studentservice.util.DBUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 工单日志 DAO 实现 — 组长 A 自己写 (append-only, 最简单)
 * <p>
 * 全组 DAO 实现的模板参考 (B/C 组员写自己的 DAO 时可以照抄 try-with-resources 结构).
 */
public class TicketLogDaoImpl implements TicketLogDao {

    // ============================================
    // ⚠️ 组员写 DAO 的标准模板 (复制这个结构即可):
    //
    //   try (Connection conn = DBUtil.getConnection();
    //        PreparedStatement ps = conn.prepareStatement(sql,
    //                               Statement.RETURN_GENERATED_KEYS)) {
    //       ps.setXxx(...);
    //       ps.executeUpdate();
    //       // 如果是 insert, 取自增 id
    //       try (ResultSet rs = ps.getGeneratedKeys()) { if (rs.next()) return rs.getLong(1); }
    //   }
    // ============================================

    @Override
    public long insert(TicketLog log) {
        String sql = "INSERT INTO ticket_log (ticket_id, operator_id, action, old_value, new_value, remark) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, log.getTicketId());
            ps.setLong(2, log.getOperatorId());
            ps.setString(3, log.getAction());
            ps.setString(4, log.getOldValue());
            ps.setString(5, log.getNewValue());
            ps.setString(6, log.getRemark());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public List<TicketLog> selectByTicketId(Long ticketId) {
        List<TicketLog> list = new ArrayList<>();
        String sql = "SELECT * FROM ticket_log WHERE ticket_id = ? ORDER BY create_time ASC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, ticketId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public List<TicketLog> selectByOperatorId(Long operatorId) {
        List<TicketLog> list = new ArrayList<>();
        String sql = "SELECT * FROM ticket_log WHERE operator_id = ? ORDER BY create_time DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, operatorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public List<TicketLog> selectByAction(String action) {
        List<TicketLog> list = new ArrayList<>();
        String sql = "SELECT * FROM ticket_log WHERE action = ? ORDER BY create_time DESC";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, action);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        } catch (SQLException e) { e.printStackTrace(); }
        return list;
    }

    @Override
    public List<Object[]> countByAction() {
        String sql = "SELECT action, COUNT(*) AS cnt FROM ticket_log GROUP BY action";
        return DBUtil.runGroupBy(sql);
    }

    private TicketLog map(ResultSet rs) throws SQLException {
        TicketLog l = new TicketLog();
        l.setId(rs.getLong("id"));
        l.setTicketId(rs.getLong("ticket_id"));
        l.setOperatorId(rs.getLong("operator_id"));
        l.setAction(DBUtil.rsStr(rs, "action"));
        l.setOldValue(DBUtil.rsStr(rs, "old_value"));
        l.setNewValue(DBUtil.rsStr(rs, "new_value"));
        l.setRemark(DBUtil.rsStr(rs, "remark"));
        l.setCreateTime(rs.getTimestamp("create_time").toLocalDateTime());
        return l;
    }
}
