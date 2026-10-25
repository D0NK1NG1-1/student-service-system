package com.example.studentservice.dao;

import com.example.studentservice.entity.Ticket;
import java.util.List;

/**
 * 工单 DAO 接口 · 组长 A 定义, 组员 C 在 dao.impl 里实现
 * <p>
 * ⚠️ 验收硬指标 — C 组员必须覆盖:
 *   分页查询 (LIMIT offset, size)           ✅ 方法 10
 *   LIKE 模糊搜索 (title/content)            ✅ 方法 12
 *   多条件组合过滤                           ✅ 方法 10/11
 *   聚合 COUNT + GROUP BY                    ✅ 方法 16-18
 *   至少 1 个 JOIN (关联 user 表取 creator 名字)  ✅ 方法 13
 */
public interface TicketDao {

    // ---- 基础 CRUD ----

    long insert(Ticket ticket);
    int  update(Ticket ticket);          // id 必填, 字段非 null 才更新
    int  deleteById(Long id);
    Ticket selectById(Long id);

    // ---- 按单条件查询 ----

    /** 按创建者(学生)查 */
    List<Ticket> selectByCreatorId(Long creatorId);
    /** 按处理人(客服)查 */
    List<Ticket> selectByAssigneeId(Long assigneeId);
    /** 按状态查 */
    List<Ticket> selectByStatus(String status);
    /** 按优先级查 */
    List<Ticket> selectByPriority(String priority);

    // ---- 多条件 + 分页 + LIKE (C 组员重点实现) ----

    /**
     * 多条件组合分页查询 (全部参数可传 null, null 表示忽略该条件)
     * @param titleKeyword 标题关键词, LIKE 匹配
     * @param category     类别
     * @param status       状态
     * @param priority     优先级
     * @param page         页码 (从 1 开始)
     * @param size         每页条数
     * @return 工单列表 (按 create_time DESC 排序)
     */
    List<Ticket> selectByConditions(String titleKeyword, String category,
                                    String status, String priority,
                                    int page, int size);

    /** 统计多条件组合的总数 (用于分页计算总页数) */
    long countByConditions(String titleKeyword, String category,
                           String status, String priority);

    /**
     * 复杂 JOIN 查询: 返回工单 + 创建者姓名 + 处理者姓名
     * SQL 示例 (供 C 组员参考):
     * SELECT t.*, u1.real_name AS creator_name, u2.real_name AS assignee_name
     * FROM ticket t
     * LEFT JOIN user u1 ON t.creator_id  = u1.id
     * LEFT JOIN user u2 ON t.assignee_id = u2.id
     * WHERE t.id = ?
     */
    Ticket selectWithUserNamesById(Long id);

    // ---- 聚合统计 (GROUP BY / COUNT) ----

    /** 按状态分组统计各状态工单数 → 返回 Map<String, Long> 或用 List<Object[]> 替代 */
    List<Object[]> countByStatus();

    /** 按类别分组统计 */
    List<Object[]> countByCategory();

    /** 按优先级分组统计 */
    List<Object[]> countByPriority();

    /** 按情绪分组统计 */
    List<Object[]> countByEmotion();

    /** 统计所有已关闭工单的平均响应时长 (close_time - create_time, 返回秒数的均值) */
    Double avgResponseSeconds();
}
