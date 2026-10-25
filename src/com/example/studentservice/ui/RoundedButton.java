package com.example.studentservice.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * 圆角按钮 (纯自绘, 支持 hover / 按下 / 禁用三态).
 * <p>
 * 默认 Swing 按钮在 Windows LAF 下改 setBackground 经常无效, 所以这里重写 paintComponent.
 */
public class RoundedButton extends JButton {

    private final Color normal;
    private final Color hover;
    private final Color press;
    private final Color fgNormal;
    private final Color fgHover;
    private final int arc = 10;

    public RoundedButton(String text, Color normal, Color hover, Color fg) {
        this(text, normal, hover, fg, Color.WHITE);
    }

    /**
     * @param fgHover 悬停/按下时的文字色. 深色底用白色, 浅灰底仍用深色, 否则会看不见.
     */
    public RoundedButton(String text, Color normal, Color hover, Color fgNormal, Color fgHover) {
        super(text);
        this.normal = normal;
        this.hover = hover;
        this.press = hover.darker();
        this.fgNormal = fgNormal;
        this.fgHover = fgHover;
        setForeground(fgNormal);
        setFont(UITheme.FONT_BOLD);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setBorder(new EmptyBorder(9, 20, 9, 20));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        Color fill;
        Color text;
        if (!isEnabled()) {
            fill = new Color(0xD8DEE7);
            text = new Color(0x9AA3AE);
        } else if (getModel().isPressed()) {
            fill = press;
            text = fgHover;
        } else if (getModel().isRollover()) {
            fill = hover;
            text = fgHover;
        } else {
            fill = normal;
            text = fgNormal;
        }

        g2.setColor(fill);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
        g2.dispose();

        setForeground(text);
        super.paintComponent(g);   // 只画文字 (contentAreaFilled=false)
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        setCursor(enabled ? Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                          : Cursor.getDefaultCursor());
    }
}
