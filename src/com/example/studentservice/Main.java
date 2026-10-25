package com.example.studentservice;

import com.example.studentservice.ui.UITheme;
import com.example.studentservice.util.DBUtil;
import com.example.studentservice.ui.LoginFrame;

import javax.swing.SwingUtilities;

/**
 * 智能学生客服系统 · Swing 图形界面版入口
 * <p>
 * 启动流程:
 *   1. 安装全局视觉主题 (UITheme: 配色 / 字号 / 圆角 / 表格样式)
 *   2. 控制台打印 banner + 测试数据库连接
 *   3. SwingUtilities.invokeLater 启动 LoginFrame
 */
public class Main {

    public static void main(String[] args) {
        // ========== 全局视觉主题 ==========
        // 统一在 UITheme 里维护字号/配色, 不再在这里散写 UIManager.put
        UITheme.install();

        printBanner();

        System.out.println("\n[系统] 正在测试数据库连接...");
        if (!DBUtil.testConnection()) {
            System.err.println("[系统] ❌ 数据库连接失败! 请检查 db.properties 和 MySQL 是否启动.");
            System.err.println("[系统] 如果是第一次运行, 请先在 MySQL 里执行 resources/schema.sql 建表.");
        } else {
            System.out.println("[系统] ✅ 数据库连接成功!\n");
        }

        // Swing 必须在 EDT 里启动
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new LoginFrame().setVisible(true);
            }
        });
    }

    private static void printBanner() {
        System.out.println("======================================================");
        System.out.println("       智能学生客服系统  (JavaSE + Swing + JDBC + AI)");
        System.out.println("       主题2 · 组长 A 架构 · JDK 8 · MySQL 8.0");
        System.out.println("======================================================");
    }
}
