package com.example.studentservice.service;

import com.example.studentservice.util.AIUtil;

/**
 * AI 业务 Service · 组长 A 定义, 组员 D 实现
 * <p>
 * 把 AIUtil (底层 HTTP 调用) 和 FaqService (本地 FAQ 匹配) 组合起来,
 * 形成完整的 AI 问答链路:
 *   学生提问 → 匹配本地 FAQ → 命中则返回答案 (source=faq)
 *                           → 未命中 → 调 LLM → 存对话 → 返回 (source=llm / source=llm+need_human)
 * <p>
 * 情感分析链路:
 *   工单创建 → 调 AIUtil.analyzeSentiment → 如果 emotion=negative 自动升优先级 urgent
 */
public interface AIService {

    /**
     * AI 自助问答 (学生在没有工单的情况下直接问)
     * D 组员实现逻辑:
     *   1) FaqService.matchQuestion(question) 尝试本地匹配
     *   2) 命中 → 存对话 (source=faq) + 返回
     *   3) 未命中 → AIUtil.generateAnswer(question) 调 LLM
     *   4) 存对话 (source=llm, need_human=结果.needHuman)
     *   5) 返回 AIUtil.AnswerResult
     *
     * @param question 学生问题
     * @param userId   用户 id (存对话用)
     * @return AIUtil.AnswerResult 可直接用 .answer / .needHuman / .confidence
     */
    AIUtil.AnswerResult chat(String question, Long userId);

    /**
     * AI 问答 — 绑定工单 (对话落 ticket_id, 供客服查上下文)
     */
    AIUtil.AnswerResult chatOnTicket(String question, Long userId, Long ticketId);

    /**
     * 情感分析 — 单独调用, 返回 positive/neutral/negative
     * 内部直接调 AIUtil.analyzeSentiment(text)
     */
    String analyzeEmotion(String text);
}
