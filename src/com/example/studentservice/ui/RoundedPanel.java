package com.example.studentservice.ui;

import javax.swing.*;
import java.awt.*;

/**
 * 圆角卡片面板 (白底 + 细描边 + 圆角), 替代默认的直角 JPanel / TitledBorder.
 */
public class RoundedPanel extends JPanel {

    private final int arc = 14;
    private Color fill = UITheme.CARD_BG;
    private Color stroke = UITheme.BORDER;

    public RoundedPanel(LayoutManager layout) {
        super(layout);
        setOpaque(false);   // 背景自己画
    }

    /** 自定义填充色 (聊天气泡用) */
    public void setFill(Color fill) {
        this.fill = fill;
        repaint();
    }

    public void setStroke(Color stroke) {
        this.stroke = stroke;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth() - 1;
        int h = getHeight() - 1;

        // 淡淡的投影 (两圈浅色描边模拟, 比真阴影省性能)
        g2.setColor(new Color(0, 0, 0, 8));
        g2.fillRoundRect(1, 2, w, h, arc, arc);

        g2.setColor(fill);
        g2.fillRoundRect(0, 0, w, h, arc, arc);

        if (stroke != null) {
            g2.setColor(stroke);
            g2.drawRoundRect(0, 0, w, h, arc, arc);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
