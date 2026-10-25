package com.example.studentservice.service.impl;

import com.example.studentservice.entity.Conversation;
import com.example.studentservice.entity.Faq;
import com.example.studentservice.service.AIService;
import com.example.studentservice.service.ConversationService;
import com.example.studentservice.service.FaqService;
import com.example.studentservice.util.AIUtil;

import java.math.BigDecimal;

/**
 * AI 业务 Service — ⚠️ 组员 D 负责完整实现 (AI 协作负责人!)
 * <p>
 * 完整 AI 问答链路 (组长 A 已给出伪代码, D 组员直接用):
 *
 *   学生提问 → FaqService.matchQuestion(question)
 *                ├─ 命中 → ConversationService.saveMessage(role=user, source=null)
 *                │         ConversationService.saveMessage(role=assistant, source=faq, content=faq.answer)
 *                │         返回 AIUtil.AnswerResult(faq.answer, false, 0.95)
 *                │
 *                └─ 未命中 → AIUtil.generateAnswer(question)  调 LLM
 *                           ConversationService.saveMessage(role=user, source=null)
 *                           ConversationService.saveMessage(role=assistant, source=llm,
 *                               content=result.answer, confidence=result.confidence,
 *                               needHuman=result.needHuman)
 *                           返回 result
 *
 * 情感分析链路: TicketServiceImpl.createTicket 里已经调了 AIUtil.analyzeSentiment
 *             这里 analyzeEmotion 是独立暴露给 Main 菜单 / 管理端调的便捷方法.
 */
public class AIServiceImpl implements AIService {

    private final FaqService faqService = new FaqServiceImpl();
    private final ConversationService convService = new ConversationServiceImpl();

    @Override
    public AIUtil.AnswerResult chat(String question, Long userId) {
        return chatInternal(question, userId, null);
    }

    @Override
    public AIUtil.AnswerResult chatOnTicket(String question, Long userId, Long ticketId) {
        return chatInternal(question, userId, ticketId);
    }

    /** 内部核心实现, 供 chat 和 chatOnTicket 复用 */
    private AIUtil.AnswerResult chatInternal(String question, Long userId, Long ticketId) {
        // 1. 先落一条 user 消息
        convService.saveMessage(ticketId, userId, "user", question,
                null, null, false);

        // 2. 先查本地 FAQ
        Faq faq = faqService.matchQuestion(question);
        if (faq != null) {
            // 命中 FAQ → 直接返回
            convService.saveMessage(ticketId, userId, "assistant", faq.getAnswer(),
                    "faq", new BigDecimal("0.95"), false);
            return new AIUtil.AnswerResult(faq.getAnswer(), false, 0.95, "faq");
        }

        // 3. 未命中 → 调 LLM
        AIUtil.AnswerResult result = AIUtil.generateAnswer(question);

        // 4. 落 LLM 回答
        convService.saveMessage(ticketId, userId, "assistant", result.answer,
                "llm", BigDecimal.valueOf(result.confidence), result.needHuman);

        // 5. 如果 LLM 要转人工, 在控制台打个提示 (UI 层可以直接读 needHuman)
        if (result.needHuman) {
            System.out.println("[AI] LLM 判定该问题需要转人工, 建议创建工单");
        }

        return result;
    }

    @Override
    public String analyzeEmotion(String text) {
        // 组长 A 已经在 AIUtil 里完整实现了 analyzeSentiment
        // D 组员可以直接调, 也可以扩展成批量分析
        return AIUtil.analyzeSentiment(text);
    }
}
