import com.example.studentservice.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/** 临时工具: 查看所有用户状态, 并可选恢复 admin01 (参数 fix 时执行) */
public class CheckUsers {
    public static void main(String[] args) throws Exception {
        System.out.println("--- 当前 user 表 (不带 status 过滤) ---");
        try (Connection c = DBUtil.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT id, username, role, status FROM user ORDER BY id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                System.out.printf("  id=%d  %-12s %-8s status=%d%s%n",
                        rs.getLong("id"), rs.getString("username"), rs.getString("role"),
                        rs.getInt("status"), rs.getInt("status") == 0 ? "   <== 已禁用" : "");
            }
        }
        if (args.length >= 2 && ("disable".equals(args[0]) || "enable".equals(args[0]))) {
            int st = "disable".equals(args[0]) ? 0 : 1;
            try (Connection c = DBUtil.getConnection();
                 PreparedStatement ps = c.prepareStatement("UPDATE user SET status = ? WHERE username = ?")) {
                ps.setInt(1, st);
                ps.setString(2, args[1]);
                System.out.println("更新 " + args[1] + " -> status=" + st + ", 影响 " + ps.executeUpdate() + " 行");
            }
        } else if (args.length > 0 && "fix".equals(args[0])) {
            try (Connection c = DBUtil.getConnection();
                 PreparedStatement ps = c.prepareStatement("UPDATE user SET status = 1 WHERE status = 0")) {
                System.out.println("已恢复被禁用账号数: " + ps.executeUpdate());
            }
        }
    }
}
