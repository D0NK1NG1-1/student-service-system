package com.example.studentservice.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;

/**
 * 全局视觉主题 —— 纯 JDK 自绘, 不引入任何第三方 UI 库 (项目约束: 纯 Java SE).
 * <p>
 * 统一管理: 配色 / 字号 / 圆角 / 表格样式 / 按钮样式.
 * 所有 Frame 必须从这里取颜色和字体, 禁止再写 new Color(...) / new Font("微软雅黑", ...).
 * <p>
 * 字号体系 (核心: 只保留 4 档, 不再出现 11/12/13/14/18/32 乱飞):
 *   FONT_SMALL  14  辅助提示、表头
 *   FONT_BASE   18  正文、标签、输入框、Tab
 *   FONT_TITLE  22  窗口标题、区块标题
 *   FONT_BIG    44  大数字 (统计)
 */
public final class UITheme {

    private UITheme() {}

    // ==================== 配色 ====================
    /** 主色 (按钮/选中态/强调) */
    public static final Color PRIMARY       = new Color(0x2563EB);
    public static final Color PRIMARY_DARK  = new Color(0x1D4ED8);
    public static final Color PRIMARY_SOFT  = new Color(0xEFF4FF);
    /** 成功色 (提交/通过) */
    public static final Color SUCCESS       = new Color(0x16A34A);
    public static final Color SUCCESS_DARK  = new Color(0x15803D);
    /** 危险色 (删除/关闭) */
    public static final Color DANGER        = new Color(0xDC2626);
    public static final Color DANGER_DARK   = new Color(0xB91C1C);
    /** 中性灰按钮 */
    public static final Color NEUTRAL       = new Color(0xEEF1F5);
    public static final Color NEUTRAL_HOVER = new Color(0xE2E8F0);
    public static final Color NEUTRAL_TEXT  = new Color(0x374151);

    /** 页面底色 / 卡片底色 / 描边 */
    public static final Color BG        = new Color(0xF5F7FA);
    public static final Color CARD_BG   = Color.WHITE;
    public static final Color BORDER    = new Color(0xE3E8EF);
    public static final Color HEADER_BG = Color.WHITE;

    /** 文字 */
    public static final Color TEXT       = new Color(0x1F2937);
    public static final Color TEXT_MUTED = new Color(0x6B7280);

    /** 表格 */
    public static final Color TABLE_HEADER_BG = new Color(0xF0F4F9);
    public static final Color TABLE_STRIPE     = new Color(0xFAFBFD);
    public static final Color TABLE_SELECT     = new Color(0xDBEAFE);

    // ==================== 字号 (只有 4 档) ====================
    private static final String FONT_NAME = "微软雅黑";
    public static final Font FONT_SMALL = new Font(FONT_NAME, Font.PLAIN, 14);
    public static final Font FONT_BASE  = new Font(FONT_NAME, Font.PLAIN, 18);
    public static final Font FONT_BOLD  = new Font(FONT_NAME, Font.BOLD, 18);
    public static final Font FONT_TITLE = new Font(FONT_NAME, Font.BOLD, 22);
    public static final Font FONT_BIG   = new Font(FONT_NAME, Font.BOLD, 44);

    // ==================== 安装到 UIManager ====================
    /**
     * 程序启动时调用一次 (Main 里), 统一覆盖 Swing 默认字体和配色.
     */
    public static void install() {
        // 1) 用系统 LAF 打底 (比默认 Metal 现代), 失败则退回默认
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) { }

        // 2) 把所有默认字体统一成 FONT_BASE (继承原来的粗体/斜体样式)
        java.util.Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource) {
                javax.swing.plaf.FontUIResource old = (javax.swing.plaf.FontUIResource) value;
                int style = old.getStyle() == 0 ? Font.PLAIN : old.getStyle();
                UIManager.put(key, new javax.swing.plaf.FontUIResource(FONT_NAME, style, 18));
            }
        }

        // 3) 分区微调 (有层次的字号, 而不是全部一样大)
        UIManager.put("TabbedPane.font",           FONT_BASE);
        UIManager.put("Table.font",                new Font(FONT_NAME, Font.PLAIN, 16));
        UIManager.put("TableHeader.font",          new Font(FONT_NAME, Font.BOLD, 14));
        UIManager.put("TitledBorder.font",         new Font(FONT_NAME, Font.BOLD, 15));
        UIManager.put("OptionPane.messageFont",    FONT_BASE);
        UIManager.put("OptionPane.buttonFont",     FONT_BASE);
        UIManager.put("ToolTip.font",              FONT_SMALL);

        // 4) 配色
        UIManager.put("Panel.background",        BG);
        UIManager.put("OptionPane.background",   CARD_BG);
        UIManager.put("TabbedPane.background",   BG);
        UIManager.put("TabbedPane.foreground",   TEXT_MUTED);
        UIManager.put("TabbedPane.selected",     PRIMARY_SOFT);
        UIManager.put("TabbedPane.tabAreaInsets", new Insets(6, 6, 0, 6));
        UIManager.put("TabbedPane.tabInsets",     new Insets(8, 18, 8, 18));
        UIManager.put("Table.selectionBackground", TABLE_SELECT);
        UIManager.put("Table.selectionForeground", TEXT);
        UIManager.put("Table.gridColor",           BORDER);
        UIManager.put("TableHeader.background",    TABLE_HEADER_BG);
        UIManager.put("TableHeader.foreground",    TEXT_MUTED);
        UIManager.put("TableHeader.separatorColor", BORDER);
        UIManager.put("ScrollBar.width",           12);
        UIManager.put("ScrollPane.border",         BorderFactory.createLineBorder(BORDER));
        UIManager.put("TextField.font",            FONT_BASE);
        UIManager.put("PasswordField.font",        FONT_BASE);
        UIManager.put("ComboBox.font",             FONT_BASE);
        UIManager.put("TextArea.font",             FONT_BASE);
        UIManager.put("Label.foreground",          TEXT);
    }

    // ==================== 组件工厂 ====================

    /** 页面容器: 统一底色 + 内边距 */
    public static JPanel page(LayoutManager layout, int padding) {
        JPanel p = new JPanel(layout);
        p.setBackground(BG);
        p.setBorder(BorderFactory.createEmptyBorder(padding, padding, padding, padding));
        return p;
    }

    /** 主按钮 (蓝) */
    public static RoundedButton primary(String text) {
        return new RoundedButton(text, PRIMARY, PRIMARY_DARK, Color.WHITE);
    }

    /** 成功按钮 (绿) */
    public static RoundedButton success(String text) {
        return new RoundedButton(text, SUCCESS, SUCCESS_DARK, Color.WHITE);
    }

    /** 危险按钮 (红) */
    public static RoundedButton danger(String text) {
        return new RoundedButton(text, DANGER, DANGER_DARK, Color.WHITE);
    }

    /** 次要按钮 (浅灰) —— 悬停时底色变深, 但文字保持深色 */
    public static RoundedButton ghost(String text) {
        return new RoundedButton(text, NEUTRAL, NEUTRAL_HOVER, NEUTRAL_TEXT, NEUTRAL_TEXT);
    }

    /** 顶部栏: 左侧标题 + 右侧退出按钮 */
    public static JPanel header(String leftText, Runnable onLogout) {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(HEADER_BG);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                BorderFactory.createEmptyBorder(10, 20, 10, 16)));

        JLabel left = new JLabel(leftText);
        left.setFont(FONT_BOLD);
        left.setForeground(TEXT);
        bar.add(left, BorderLayout.WEST);

        if (onLogout != null) {
            RoundedButton out = ghost("退出登录");
            out.addActionListener(e -> onLogout.run());
            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            right.setOpaque(false);
            right.add(out);
            bar.add(right, BorderLayout.EAST);
        }
        return bar;
    }

    /** 卡片: 白底圆角 + 标题 + 内容 */
    public static JPanel card(String title, JComponent body) {
        RoundedPanel card = new RoundedPanel(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 16, 16));

        if (title != null && !title.isEmpty()) {
            JLabel t = new JLabel(title);
            t.setFont(new Font(FONT_NAME, Font.BOLD, 16));
            t.setForeground(TEXT);
            card.add(t, BorderLayout.NORTH);
        }
        if (body != null) card.add(body, BorderLayout.CENTER);
        return card;
    }

    /** 区块标题 (Tab 内容区顶部) */
    public static JLabel sectionTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_TITLE);
        l.setForeground(TEXT);
        return l;
    }

    /** 统一表格样式: 行高 / 斑马纹 / 表头 / 选中色 */
    public static void styleTable(JTable table) {
        table.setRowHeight(38);
        table.setFont(new Font(FONT_NAME, Font.PLAIN, 16));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(TABLE_SELECT);
        table.setSelectionForeground(TEXT);
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 40));

        JTableHeader header = table.getTableHeader();
        header.setBackground(TABLE_HEADER_BG);
        header.setForeground(TEXT_MUTED);
        header.setFont(new Font(FONT_NAME, Font.BOLD, 14));
        header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));

        // 斑马纹 + 左侧留白
        table.setDefaultRenderer(Object.class, new StripeRenderer());
    }

    /** 单元格渲染器: 交替行底色 + 内边距 */
    private static class StripeRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
            if (!isSelected) {
                c.setBackground(row % 2 == 0 ? CARD_BG : TABLE_STRIPE);
            }
            setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
            return c;
        }
    }

    /** 表格放进白底圆角卡片里 */
    public static JPanel tableCard(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(CARD_BG);
        RoundedPanel card = new RoundedPanel(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    // ==================== 英文码 -> 中文显示 ====================

    /**
     * 把数据库里的英文枚举值翻译成中文 (只用于界面显示, 库里仍然存英文码).
     * 覆盖: 工单状态 / 优先级 / 情绪.
     */
    public static String label(String code) {
        if (code == null || code.isEmpty()) return "-";
        switch (code) {
            // 工单状态
            case "open":             return "待处理";
            case "processing":       return "处理中";
            case "pending_customer": return "待用户";
            case "closed":           return "已关闭";
            // 优先级
            case "low":              return "低";
            case "medium":           return "中";
            case "high":             return "高";
            case "urgent":           return "紧急";
            // 情绪
            case "positive":         return "正面";
            case "neutral":          return "中性";
            case "negative":         return "负面";
            // 账号状态
            case "active":           return "启用";
            case "disabled":         return "禁用";
            default:                 return code;
        }
    }

    /** 中文显示值反查英文码 (给状态列上色用) */
    private static String codeOf(String text) {
        if (text == null) return null;
        switch (text) {
            case "待处理": return "open";
            case "处理中": return "processing";
            case "待用户": return "pending_customer";
            case "已关闭": return "closed";
            case "低":     return "low";
            case "中":     return "medium";
            case "高":     return "high";
            case "紧急":   return "urgent";
            case "正面":   return "positive";
            case "中性":   return "neutral";
            case "负面":   return "negative";
            case "启用":   return "active";
            case "禁用":   return "disabled";
            default:       return text;
        }
    }

    /** 给某一列按状态值上色 (状态 / 优先级 / 情绪列, 中文/英文都支持) */
    public static void colorColumn(JTable table, int column) {
        if (column < 0 || column >= table.getColumnCount()) return;
        table.getColumnModel().getColumn(column).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean isSelected, boolean hasFocus,
                                                           int row, int col) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, false, row, col);
                if (!isSelected) {
                    c.setBackground(row % 2 == 0 ? CARD_BG : TABLE_STRIPE);
                    c.setForeground(statusColor(value == null ? null : value.toString()));
                }
                setFont(new Font(FONT_NAME, Font.BOLD, 16));
                setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                return c;
            }
        });
    }

    /** 状态文字 -> 颜色 (接受英文码或中文显示值) */
    public static Color statusColor(String text) {
        String status = codeOf(text);
        if (status == null) return TEXT_MUTED;
        switch (status) {
            case "open":             return new Color(0x2563EB);
            case "processing":       return new Color(0xD97706);
            case "pending_customer": return new Color(0x7C3AED);
            case "closed":           return new Color(0x6B7280);
            case "urgent":           return DANGER;
            case "high":             return new Color(0xEA580C);
            case "medium":           return new Color(0xCA8A04);
            case "low":              return new Color(0x0891B2);
            case "negative":         return DANGER;
            case "positive":         return SUCCESS;
            case "active":           return SUCCESS;
            case "disabled":         return new Color(0x9CA3AF);
            default:                 return TEXT_MUTED;
        }
    }

    /** 通用边框 (卡片内部用) */
    public static Border lineBorder() {
        return BorderFactory.createLineBorder(BORDER);
    }
}
