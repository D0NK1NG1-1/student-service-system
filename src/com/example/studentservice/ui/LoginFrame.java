package com.example.studentservice.ui;

import com.example.studentservice.Global;
import com.example.studentservice.entity.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * 登录 / 注册窗口 —— 程序入口.
 * <p>
 * 版式参考主流登录页: 不再用 Tab 切换, 而是"登录"和"注册"两张卡片用 CardLayout 切换.
 * 登录按钮下方放一行「还没有账号? 立即注册」, 点一下切到注册卡片; 注册页底部有「返回登录」.
 * 登录成功后根据 user.role 打开对应角色的主窗口.
 */
public class LoginFrame extends JFrame {

    private static final String CARD_LOGIN    = "login";
    private static final String CARD_REGISTER = "register";

    private CardLayout cardLayout;
    private JPanel cardPanel;

    // ==== 登录卡片组件 ====
    private JTextField loginUserField;
    private JPasswordField loginPassField;
    private JLabel loginStatusLabel;

    // ==== 注册卡片组件 ====
    private JTextField regUserField;
    private JPasswordField regPassField;
    private JTextField regNameField;
    private JComboBox<String> regRoleCombo;
    private JLabel regStatusLabel;

    public LoginFrame() {
        super("智能学生客服系统 — 登录");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.max(620, (int)(screen.width * 0.42));
        int h = Math.max(600, (int)(screen.height * 0.60));
        setSize(w, h);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(560, 580));
        getContentPane().setBackground(UITheme.BG);

        // 顶部品牌横幅
        add(buildBanner(), BorderLayout.NORTH);

        // 中间: 登录卡片 / 注册卡片 二选一
        cardLayout = new CardLayout();
        cardPanel = new JPanel(cardLayout);
        cardPanel.setBackground(UITheme.BG);
        cardPanel.add(buildLoginPanel(), CARD_LOGIN);
        cardPanel.add(buildRegisterPanel(), CARD_REGISTER);
        add(cardPanel, BorderLayout.CENTER);

        // 窗口显示后把焦点给用户名输入框
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowOpened(java.awt.event.WindowEvent e) {
                loginUserField.requestFocusInWindow();
            }
        });
    }

    /** 顶部蓝色横幅: 标题 + 副标题居中 */
    private JPanel buildBanner() {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(UITheme.PRIMARY);
        banner.setBorder(new EmptyBorder(22, 24, 22, 24));

        JLabel title = new JLabel("智能学生客服系统", SwingConstants.CENTER);
        title.setFont(UITheme.FONT_TITLE);
        title.setForeground(Color.WHITE);

        JLabel sub = new JLabel("校园问题 · AI 先答 · 人工兜底", SwingConstants.CENTER);
        sub.setFont(UITheme.FONT_SMALL);
        sub.setForeground(new Color(0xD6E4FF));

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 6));
        center.setOpaque(false);
        center.add(title);
        center.add(sub);

        banner.add(center, BorderLayout.CENTER);
        return banner;
    }

    // ==================== 登录卡片 ====================

    private JPanel buildLoginPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(UITheme.BG);
        outer.setBorder(new EmptyBorder(24, 36, 24, 36));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // 用户名
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(fieldLabel("用户名"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.insets = new Insets(10, 0, 10, 0);
        loginUserField = new JTextField();
        styleField(loginUserField);
        form.add(loginUserField, gbc);

        // 密码
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.insets = new Insets(10, 0, 10, 12);
        form.add(fieldLabel("密码"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.insets = new Insets(10, 0, 10, 0);
        loginPassField = new JPasswordField();
        styleField(loginPassField);
        form.add(loginPassField, gbc);

        // 登录按钮
        gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2; gbc.weightx = 1;
        gbc.insets = new Insets(20, 0, 8, 0);
        RoundedButton loginBtn = UITheme.primary("登 录");
        loginBtn.addActionListener(e -> doLogin());
        form.add(loginBtn, gbc);

        // 状态提示
        gbc.gridy = 3; gbc.insets = new Insets(2, 0, 2, 0);
        loginStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        loginStatusLabel.setFont(UITheme.FONT_SMALL);
        loginStatusLabel.setForeground(UITheme.DANGER);
        form.add(loginStatusLabel, gbc);

        // 「还没有账号? 立即注册」 —— 主流登录页的常见位置
        gbc.gridy = 4; gbc.insets = new Insets(4, 0, 0, 0);
        JPanel regRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        regRow.setOpaque(false);
        JLabel tip = new JLabel("还没有账号?");
        tip.setFont(UITheme.FONT_SMALL);
        tip.setForeground(UITheme.TEXT_MUTED);
        regRow.add(tip);
        regRow.add(linkButton("立即注册", () -> showCard(CARD_REGISTER, regUserField)));
        form.add(regRow, gbc);

        // 测试账号提示
        gbc.gridy = 5; gbc.insets = new Insets(10, 0, 0, 0);
        JLabel hint = new JLabel("测试账号 admin01 / cs001 / student01   密码均为 123456",
                SwingConstants.CENTER);
        hint.setFont(UITheme.FONT_SMALL);
        hint.setForeground(UITheme.TEXT_MUTED);
        form.add(hint, gbc);

        JPanel card = UITheme.card(null, form);
        card.setPreferredSize(new Dimension(540, 380));
        outer.add(card);

        // 回车登录
        loginPassField.addActionListener(e -> doLogin());
        return outer;
    }

    private void doLogin() {
        String username = loginUserField.getText().trim();
        String password = new String(loginPassField.getPassword()).trim();
        if (username.isEmpty() || password.isEmpty()) {
            loginStatusLabel.setForeground(UITheme.DANGER);
            loginStatusLabel.setText("请输入用户名和密码");
            return;
        }
        User u = Global.userService.login(username, password);
        if (u == null) {
            loginStatusLabel.setForeground(UITheme.DANGER);
            loginStatusLabel.setText("用户名或密码错误");
            return;
        }
        // ⚠️ 原来这里写的是 "disabled".equals(u.getStatus()), 但 status 是 Integer(1/0),
        //    字符串和 Integer 比较永远 false —— 判断形同虚设. 改成按 0 判断.
        if (u.getStatus() != null && u.getStatus() == 0) {
            loginStatusLabel.setForeground(UITheme.DANGER);
            loginStatusLabel.setText("账号已被禁用, 请联系管理员");
            return;
        }
        Global.currentUser = u;
        loginStatusLabel.setForeground(UITheme.SUCCESS);
        loginStatusLabel.setText("登录成功, 欢迎 " + u.getRealName());

        // 打开对应角色主窗口
        SwingUtilities.invokeLater(() -> openRoleFrame(u));
    }

    private void openRoleFrame(User u) {
        String role = u.getRole();
        switch (role) {
            case "student": new StudentFrame().setVisible(true); break;
            case "cs":      new CsFrame().setVisible(true);      break;
            case "admin":   new AdminFrame().setVisible(true);   break;
            default: JOptionPane.showMessageDialog(this, "未知角色: " + role);
        }
        this.dispose();  // 关闭登录窗口
    }

    // ==================== 注册卡片 ====================

    private JPanel buildRegisterPanel() {
        JPanel outer = new JPanel(new GridBagLayout());
        outer.setBackground(UITheme.BG);
        outer.setBorder(new EmptyBorder(20, 36, 20, 36));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 0, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(fieldLabel("用户名"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.insets = new Insets(8, 0, 8, 0);
        regUserField = new JTextField();
        styleField(regUserField);
        form.add(regUserField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.insets = new Insets(8, 0, 8, 12);
        form.add(fieldLabel("密码"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.insets = new Insets(8, 0, 8, 0);
        regPassField = new JPasswordField();
        styleField(regPassField);
        form.add(regPassField, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.insets = new Insets(8, 0, 8, 12);
        form.add(fieldLabel("真实姓名"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.insets = new Insets(8, 0, 8, 0);
        regNameField = new JTextField();
        styleField(regNameField);
        form.add(regNameField, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0; gbc.insets = new Insets(8, 0, 8, 12);
        form.add(fieldLabel("角色"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.insets = new Insets(8, 0, 8, 0);
        regRoleCombo = new JComboBox<>(new String[]{"student 学生", "cs 客服"});
        regRoleCombo.setFont(UITheme.FONT_BASE);
        regRoleCombo.setPreferredSize(new Dimension(0, 40));
        form.add(regRoleCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 2; gbc.weightx = 1;
        gbc.insets = new Insets(18, 0, 8, 0);
        RoundedButton regBtn = UITheme.success("注 册");
        regBtn.addActionListener(e -> doRegister());
        form.add(regBtn, gbc);

        gbc.gridy = 5; gbc.insets = new Insets(2, 0, 2, 0);
        regStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        regStatusLabel.setFont(UITheme.FONT_SMALL);
        regStatusLabel.setForeground(UITheme.DANGER);
        form.add(regStatusLabel, gbc);

        // 「已有账号? 返回登录」
        gbc.gridy = 6; gbc.insets = new Insets(4, 0, 0, 0);
        JPanel backRow = new JPanel(new FlowLayout(FlowLayout.CENTER, 2, 0));
        backRow.setOpaque(false);
        JLabel tip = new JLabel("已有账号?");
        tip.setFont(UITheme.FONT_SMALL);
        tip.setForeground(UITheme.TEXT_MUTED);
        backRow.add(tip);
        backRow.add(linkButton("返回登录", () -> showCard(CARD_LOGIN, loginUserField)));
        form.add(backRow, gbc);

        JPanel card = UITheme.card(null, form);
        card.setPreferredSize(new Dimension(540, 430));
        outer.add(card);

        // 回车提交注册
        regPassField.addActionListener(e -> doRegister());
        return outer;
    }

    private void doRegister() {
        String username = regUserField.getText().trim();
        String password = new String(regPassField.getPassword()).trim();
        String realName = regNameField.getText().trim();
        String roleFull = (String) regRoleCombo.getSelectedItem();
        String role = roleFull.startsWith("cs") ? "cs" : "student";

        if (username.isEmpty() || password.isEmpty() || realName.isEmpty()) {
            regStatusLabel.setForeground(UITheme.DANGER);
            regStatusLabel.setText("请填写完整信息");
            return;
        }
        if (password.length() < 6) {
            regStatusLabel.setForeground(UITheme.DANGER);
            regStatusLabel.setText("密码至少 6 位");
            return;
        }

        try {
            User u = Global.userService.register(username, password, role, realName);
            if (u != null && u.getId() != null) {
                // 注册成功: 清空注册表单, 自动切回登录卡片并回填用户名
                regUserField.setText("");
                regPassField.setText("");
                regNameField.setText("");
                regStatusLabel.setText(" ");

                loginUserField.setText(username);
                loginPassField.setText("");
                loginStatusLabel.setForeground(UITheme.SUCCESS);
                loginStatusLabel.setText("注册成功, 请登录");
                showCard(CARD_LOGIN, loginPassField);
            } else {
                regStatusLabel.setForeground(UITheme.DANGER);
                regStatusLabel.setText("注册失败, 用户名可能已存在");
            }
        } catch (Exception ex) {
            regStatusLabel.setForeground(UITheme.DANGER);
            regStatusLabel.setText("异常: " + ex.getMessage());
        }
    }

    // ==================== 小组件 ====================

    /** 切换登录/注册卡片, 并把焦点交给指定输入框 */
    private void showCard(String cardName, JTextField focusField) {
        cardLayout.show(cardPanel, cardName);
        if (focusField != null) {
            SwingUtilities.invokeLater(focusField::requestFocusInWindow);
        }
    }

    /** 文字链样式按钮 (无边框、蓝色、带下划线、手型光标) */
    private JButton linkButton(String text, Runnable action) {
        JButton b = new JButton("<html><u>" + text + "</u></html>");
        b.setFont(UITheme.FONT_SMALL);
        b.setForeground(UITheme.PRIMARY);
        b.setBackground(UITheme.BG);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setOpaque(false);
        b.setBorder(new EmptyBorder(2, 4, 2, 4));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        // 悬停变深色
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setForeground(UITheme.PRIMARY_DARK); }
            @Override public void mouseExited(MouseEvent e)  { b.setForeground(UITheme.PRIMARY); }
        });
        return b;
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_BOLD);
        l.setForeground(UITheme.TEXT);
        return l;
    }

    /** 输入框统一: 字体 + 高度 + 内边距 */
    private void styleField(JTextField f) {
        f.setFont(UITheme.FONT_BASE);
        f.setPreferredSize(new Dimension(0, 40));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(4, 10, 4, 10)));
    }
}
