package com.example.studentservice.service;

import com.example.studentservice.entity.Faq;
import java.util.List;

/**
 * FAQ Service · 组长 A 定义, 组员 B 实现
 * <p>
 * 除了基础 CRUD, 提供 matchQuestion 方法给 AI 问答链路调用 —— 先查本地 FAQ,
 * 命中则返回答案, 未命中才走 LLM.
 */
public interface FaqService {

    // ---- 管理员 CRUD ----
    Faq create(Faq faq);
    boolean update(Faq faq);
    boolean delete(Long id);

    Faq getById(Long id);
    List<Faq> listAll();
    List<Faq> listPage(int page, int size);
    long totalCount();

    /** 按关键词模糊搜 (管理端搜索框) */
    List<Faq> searchByKeyword(String keyword);

    /** 按分类查 */
    List<Faq> listByCategory(String category);

    // ---- AI 调用链专用 ----

    /**
     * 尝试匹配 FAQ. 匹配成功返回对应的 Faq 对象 (并自动 incrementHitCount).
     * 匹配失败返回 null.
     * 匹配逻辑建议: 对 question 和 keywords 做 LIKE '%keyword%', 按命中数排序取第一条.
     */
    Faq matchQuestion(String question);
}
