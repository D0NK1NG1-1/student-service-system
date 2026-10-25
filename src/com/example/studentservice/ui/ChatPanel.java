package com.example.studentservice.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 聊天气泡面板 —— 学生端 AI 问答 / 客服端 AI 辅助共用.
 * <p>
 * 用 JPanel + BoxLayout 纵向堆叠气泡, 右侧蓝色 = 用户, 左侧浅灰 = AI, 居中灰字 = 系统提示.
 * 纯自绘, 不依赖任何第三方库.
 */
public class ChatPanel extends JPanel {

    private final JPanel list;
    private final JScrollPane scroll;
    private JComponent typing;      // "AI 正在思考…" 占位

    public ChatPanel() {
        super(new BorderLayout());
        setOpaque(false);

        list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setBackground(UITheme.CARD_BG);
        list.setBorder(new EmptyBorder(14, 14, 14, 14));

        scroll = new JScrollPane(list);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.getViewport().setBackground(UITheme.CARD_BG);
        scroll.getVerticalScrollBar().setUnitIncrement(18);
        add(scroll, BorderLayout.CENTER);
    }

    // ==================== 对外接口 ====================

    /** 用户消息 (右侧, 蓝色气泡) */
    public void addUser(String text) {
        addBubble(text, true, null);
    }

    /** AI 消息 (左侧, 浅灰气泡); meta 是灰色小字注释, 可传 null */
    public void addAI(String text, String meta) {
        addBubble(text, false, meta);
    }

    /** 系统提示 (居中灰字, 无气泡) */
    public void addSystem(String text) {
        JLabel l = new JLabel("<html><div style='text-align:center;'>" + esc(text) + "</div></html>");
        l.setFont(UITheme.FONT_SMALL);
        l.setForeground(UITheme.TEXT_MUTED);
        l.setHorizontalAlignment(SwingConstants.CENTER);
        l.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 4));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(l);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));
        list.add(row);
        list.add(Box.createVerticalStrut(4));
        revalidate();
        scrollToBottom();
    }

    /** 显示/隐藏 "正在思考" 占位 */
    public void setTyping(boolean on) {
        if (on) {
            if (typing != null) return;
            JLabel l = new JLabel("AI 正在思考…");
            l.setFont(UITheme.FONT_SMALL);
            l.setForeground(UITheme.TEXT_MUTED);
            typing = l;
            JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
            row.setOpaque(false);
            row.setAlignmentX(Component.LEFT_ALIGNMENT);
            row.add(l);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
            list.add(row);
            revalidate();
            scrollToBottom();
        } else if (typing != null) {
            Container row = typing.getParent();
            list.remove(row);
            typing = null;
            revalidate();
            repaint();
        }
    }

    // ==================== 内部 ====================

    private void addBubble(String text, boolean isUser, String meta) {
        // 1) 先按文本真实像素宽度决定气泡宽度 (短消息不铺满整行)
        JTextArea ta = new JTextArea(text);
        ta.setLineWrap(true);
        ta.setWrapStyleWord(true);
        ta.setEditable(false);
        ta.setOpaque(false);
        ta.setFont(UITheme.FONT_BASE);
        ta.setForeground(isUser ? Color.WHITE : UITheme.TEXT);
        ta.setBorder(new EmptyBorder(0, 0, 0, 0));

        int maxW = 620;
        FontMetrics fm = ta.getFontMetrics(UITheme.FONT_BASE);
        int natural = 0;
        for (String line : text.split("\n")) {
            natural = Math.max(natural, fm.stringWidth(line));
        }
        int w = Math.min(natural + 8, maxW);
        if (w < 60) w = 60;

        ta.setSize(new Dimension(w, Short.MAX_VALUE));
        Dimension pref = ta.getPreferredSize();
        int h = pref.height;
        Dimension fixed = new Dimension(w, h);
        ta.setPreferredSize(fixed);
        ta.setMinimumSize(fixed);
        ta.setMaximumSize(fixed);

        // 2) 气泡本体 (说明文字放在气泡内部下方, 保证跟气泡同宽同侧)
        RoundedPanel bubble = new RoundedPanel(new BorderLayout(0, 4));
        bubble.setFill(isUser ? UITheme.PRIMARY : new Color(0xF1F5F9));
        bubble.setStroke(isUser ? UITheme.PRIMARY : new Color(0xE2E8F0));
        bubble.setBorder(new EmptyBorder(10, 14, 10, 14));
        bubble.add(ta, BorderLayout.CENTER);

        if (meta != null && !meta.isEmpty()) {
            JLabel m = new JLabel(meta);
            m.setFont(UITheme.FONT_SMALL);
            m.setForeground(isUser ? new Color(0xD6E4FF) : UITheme.TEXT_MUTED);
            bubble.add(m, BorderLayout.SOUTH);
        }

        // 3) 整行 (左右对齐)
        JPanel row = new JPanel(new FlowLayout(isUser ? FlowLayout.RIGHT : FlowLayout.LEFT, 0, 2));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.add(bubble);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height));

        list.add(row);
        list.add(Box.createVerticalStrut(10));
        revalidate();
        scrollToBottom();
    }

    private void scrollToBottom() {
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = scroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
