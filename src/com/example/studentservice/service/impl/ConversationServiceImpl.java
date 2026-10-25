package com.example.studentservice.service.impl;

import com.example.studentservice.dao.ConversationDao;
import com.example.studentservice.dao.impl.ConversationDaoImpl;
import com.example.studentservice.entity.Conversation;
import com.example.studentservice.service.ConversationService;

import java.math.BigDecimal;
import java.util.List;

/**
 * 对话 Service — ⚠️ 组员 C 可以实现, 或组长 A 自己写
 */
public class ConversationServiceImpl implements ConversationService {

    private final ConversationDao dao = new ConversationDaoImpl();

    @Override
    public Conversation saveMessage(Long ticketId, Long userId, String role,
                                    String content, String source,
                                    BigDecimal confidence, boolean needHuman) {
        Conversation c = new Conversation();
        c.setTicketId(ticketId);
        c.setUserId(userId);
        c.setRole(role);
        c.setContent(content);
        c.setSource(source);
        c.setConfidence(confidence);
        c.setNeedHuman(needHuman ? 1 : 0);
        long id = dao.insert(c);
        c.setId(id);
        return c;
    }

    @Override
    public List<Conversation> listByTicketId(Long ticketId) { return dao.selectByTicketId(ticketId); }

    @Override
    public List<Conversation> listRecent(Long ticketId, int n) { return dao.selectRecentByTicketId(ticketId, n); }

    @Override
    public List<Conversation> listSelfChatByUser(Long userId) { return dao.selectByUserId(userId); }
}
