package com.example.studentservice.dao;

import com.example.studentservice.entity.Conversation;
import java.util.List;

/**
 * 对话 DAO 接口 · 组长 A 定义
 * <p>
 * AI 问答和人工对话都会落库, 此表只增不改不删 (append-only).
 * 组员 C 可以一起实现, 或组长 A 自己先写一个简单版本.
 */
public interface ConversationDao {

    /** 插入一条消息, 返回自增 id */
    long insert(Conversation conversation);

    /** 按工单查全部对话 (按 create_time ASC) */
    List<Conversation> selectByTicketId(Long ticketId);

    /** 按用户查全部对话 */
    List<Conversation> selectByUserId(Long userId);

    /** 取某工单最近 N 条对话 (用于上下文拼接) */
    List<Conversation> selectRecentByTicketId(Long ticketId, int n);

    /** 按来源统计 (faq/llm/manual) */
    List<Object[]> countBySource();

    /** 统计某工单对话条数 */
    long countByTicketId(Long ticketId);
}
