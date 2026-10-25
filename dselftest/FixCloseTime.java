import com.example.studentservice.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;

/** 一次性数据修复: 给已关闭但没有 close_time 的工单补上关闭时间 (临时文件) */
public class FixCloseTime {
    public static void main(String[] args) throws Exception {
        String sql = "UPDATE ticket SET close_time = DATE_ADD(create_time, INTERVAL 5 HOUR) "
                   + "WHERE status = 'closed' AND close_time IS NULL";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int n = ps.executeUpdate();
            System.out.println("已补齐 close_time 的工单数: " + n);
        }
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT COUNT(*), AVG(TIMESTAMPDIFF(SECOND, create_time, close_time)) FROM ticket WHERE close_time IS NOT NULL");
             java.sql.ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                System.out.println("有 close_time 的工单: " + rs.getInt(1) + ", 平均响应秒数: " + rs.getDouble(2));
            }
        }
    }
}
