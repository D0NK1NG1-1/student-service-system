package com.example.studentservice;

import com.example.studentservice.entity.User;
import com.example.studentservice.service.*;
import com.example.studentservice.service.impl.*;

/**
 * 全局共享状态 —— 所有 Swing Frame 通过这个类访问 Service 单例和当前登录用户.
 * 控制台版 (Main.java) 和 Swing 版 (ui/*.java) 共用同一套 Service 实现.
 */
public class Global {

    // ========== Service 实例 (全部用接口类型引用, 验收 "接口分层") ==========
    public static final UserService userService        = new UserServiceImpl();
    public static final FaqService faqService          = new FaqServiceImpl();
    public static final TicketService ticketService    = new TicketServiceImpl();
    public static final AIService aiService            = new AIServiceImpl();
    public static final StatisticsService statsService  = new StatisticsServiceImpl();
    public static final ConversationService convService = new ConversationServiceImpl();

    // ========== 当前登录用户 ==========
    public static volatile User currentUser;
}
