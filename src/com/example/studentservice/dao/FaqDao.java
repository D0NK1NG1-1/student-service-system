package com.example.studentservice.dao;

import com.example.studentservice.entity.Faq;
import java.util.List;

/**
 * FAQ DAO 接口 · 组长 A 定义, 组员 B 在 dao.impl 里实现
 * <p>
 * ⚠️ 验收硬指标 — B 组员必须覆盖:
 *   LIKE 模糊搜索 (question + keywords)    ✅ selectByKeyword
 *   分页                                    ✅ selectPage
 *   命中次数自增更新                        ✅ incrementHitCount
 *   聚合 countByCategory                    ✅
 */
public interface FaqDao {

    // ---- 基础 CRUD ----
    long insert(Faq faq);
    int  update(Faq faq);
    int  deleteById(Long id);
    Faq  selectById(Long id);
    List<Faq> selectAll();

    // ---- 业务查询 ----

    /**
     * 关键词模糊匹配 — 用于 AI 回答前先查本地 FAQ
     * 在 question 和 keywords 两个字段上做 LIKE '%keyword%'
     * 返回结果按 hit_count 降序 (热门问题优先)
     */
    List<Faq> selectByKeyword(String keyword);

    /** 按分类查 */
    List<Faq> selectByCategory(String category);

    /** 分页查询 (按 create_time DESC) */
    List<Faq> selectPage(int page, int size);

    /** 总数 (配合分页) */
    long count();

    /** 按分类统计数量 (聚合) */
    List<Object[]> countByCategory();

    /** 命中次数自增 1 — AI 或手动命中 FAQ 时调 */
    int incrementHitCount(Long id);
}
