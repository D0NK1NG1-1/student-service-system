package com.example.studentservice.service;

import com.example.studentservice.entity.Conversation;
import java.util.List;

/**
 * 对话 Service · 组长 A 定义, 组员 C 或组长自己实现
 * <p>
 * 每次 AI 问答 / 人工回复都必须落一条 conversation.
 */
public interface ConversationService {

    /** 保存一条消息 */
    Conversation saveMessage(Long ticketId, Long userId, String role,
                            String content, String source,
                            java.math.BigDecimal confidence,
                            boolean needHuman);

    /** 某工单的完整对话历史 */
    List<Conversation> listByTicketId(Long ticketId);

    /** 取某工单最近 N 条 (用于拼 LLM 上下文) */
    List<Conversation> listRecent(Long ticketId, int n);

    /** 某用户的全部自助对话 (ticket_id 为 null 的) */
    List<Conversation> listSelfChatByUser(Long userId);
}
