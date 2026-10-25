package com.example.studentservice.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * 数据库工具类 · 组长 A 维护
 * <p>
 * 所有组员的 DAO 实现类统一用以下模板:
 * <pre>
 *   try (Connection conn = DBUtil.getConnection();
 *        PreparedStatement ps = conn.prepareStatement(sql);
 *        ResultSet rs = ps.executeQuery()) {
 *       while (rs.next()) { ... }
 *   }
 * </pre>
 * 本项目使用 try-with-resources 自动关闭连接, 无需手动 close.
 * 如果是 INSERT/UPDATE/DELETE 没有 ResultSet, 省略 ResultSet 即可.
 */
public class DBUtil {

    private static String DRIVER;
    private static String URL;
    private static String USER;
    private static String PASSWORD;

    // 静态代码块: 类加载时只执行一次, 读配置文件 + 注册驱动
    static {
        Properties props = new Properties();
        // 先尝试从 classpath 加载 (开发时走 resources/db.properties)
        try (InputStream is = DBUtil.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            System.err.println("[DBUtil] 加载 db.properties 失败: " + e.getMessage());
        }

        // 如果 classpath 没读到, 用默认值 (方便没有配置文件时直接跑)
        DRIVER  = props.getProperty("jdbc.driver",  "com.mysql.cj.jdbc.Driver");
        URL     = props.getProperty("jdbc.url",     "jdbc:mysql://localhost:3306/student_cs_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true");
        USER    = props.getProperty("jdbc.username","root");
        PASSWORD= props.getProperty("jdbc.password","123456");

        try {
            Class.forName(DRIVER);
            System.out.println("[DBUtil] JDBC 驱动加载成功: " + DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("[DBUtil] JDBC 驱动未找到, 请把 mysql-connector-java-x.x.x.jar 加到 lib");
            e.printStackTrace();
        }
    }

    /** 获取数据库连接 — 所有组员统一调用这个方法 */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }

    /**
     * 手动关闭所有资源 (try-with-resources 场景下不需要调,
     * 只有老代码或不方便用 try-with-resources 时用).
     */
    public static void close(AutoCloseable... closeables) {
        for (AutoCloseable c : closeables) {
            if (c != null) {
                try {
                    c.close();
                } catch (Exception e) {
                    // 关闭失败只打日志, 不影响主流程
                    System.err.println("[DBUtil] 关闭资源失败: " + e.getMessage());
                }
            }
        }
    }

    /** 测试数据库是否连通 */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn.isValid(3);
        } catch (SQLException e) {
            System.err.println("[DBUtil] 数据库连接失败: " + e.getMessage());
            return false;
        }
    }

    // ============ 参数查询辅助方法 (组长 A 提供给 B/C 组员参考) ============

    /**
     * 通用分页 SQL 生成 (MySQL 语法)
     * 组员 C 做复杂查询时可以用这个方法.
     */
    public static String buildPageSql(String baseSql, int page, int size) {
        int offset = (page - 1) * size;
        return baseSql + " LIMIT " + offset + ", " + size;
    }

    /** 把 ResultSet 某列安全地读成 String, 遇到 null 返回 "" */
    public static String rsStr(ResultSet rs, String col) throws SQLException {
        String v = rs.getString(col);
        return v == null ? "" : v;
    }

    /** 把 ResultSet 某列安全地读成 Long, 遇到 null 返回 null */
    public static Long rsLong(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }

    /**
     * 执行 GROUP BY / COUNT 类查询, 返回 [key, count] 列表
     * 所有 DAO 的 countByXxx() 方法都可以复用这个方法.
     * 示例 SQL: SELECT status, COUNT(*) AS cnt FROM ticket GROUP BY status
     * 返回: [["open", 5L], ["processing", 3L], ...]
     */
    public static List<Object[]> runGroupBy(String sql) {
        List<Object[]> result = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Object[] row = new Object[2];
                row[0] = rs.getObject(1);  // 分组 key
                row[1] = rs.getLong(2);    // count
                result.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return result;
    }

    /** 执行单值聚合查询, 返回 Double (用于 AVG) */
    public static Double runAggregateDouble(String sql) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                double v = rs.getDouble(1);
                return rs.wasNull() ? null : v;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
