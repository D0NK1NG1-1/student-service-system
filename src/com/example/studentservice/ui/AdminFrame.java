package com.example.studentservice.ui;

import com.example.studentservice.Global;
import com.example.studentservice.entity.Faq;
import com.example.studentservice.entity.Ticket;
import com.example.studentservice.entity.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理员主窗口 —— 四个 Tab:
 *   1. 统计面板 (验收硬指标: GROUP BY 聚合 + TIMESTAMPDIFF)
 *   2. 全部工单 (分配客服 / 改优先级 / 关闭)
 *   3. FAQ 管理 (CRUD)
 *   4. 用户管理
 */
public class AdminFrame extends JFrame {

    private JTabbedPane tabs;

    public AdminFrame() {
        super("管理员工作台 — " + Global.currentUser.getRealName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.max(1080, (int)(screen.width * 0.86));
        int h = Math.max(740, (int)(screen.height * 0.84));
        setSize(w, h);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(900, 640));
        getContentPane().setBackground(UITheme.BG);

        tabs = new JTabbedPane();
        tabs.setFont(UITheme.FONT_BASE);
        tabs.addTab("统计面板", buildStatsPanel());
        tabs.addTab("全部工单", buildAllTicketsPanel());
        tabs.addTab("FAQ 管理", buildFaqPanel());
        tabs.addTab("用户管理", buildUserPanel());
        add(tabs, BorderLayout.CENTER);

        add(UITheme.header("管理员工作台　" + Global.currentUser.getRealName(), () -> {
            Global.currentUser = null;
            new LoginFrame().setVisible(true);
            this.dispose();
        }), BorderLayout.NORTH);
    }

    // ==================== Tab 1: 统计面板 ====================

    private JPanel buildStatsPanel() {
        JPanel panel = UITheme.page(new BorderLayout(0, 12), 18);

        // 顶部: 标题 + 刷新
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(UITheme.sectionTitle("数据统计"), BorderLayout.WEST);

        RoundedButton refreshBtn = UITheme.ghost("刷新统计");
        refreshBtn.addActionListener(e -> refreshStats());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(refreshBtn);
        top.add(right, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        // 4 张统计卡片
        JPanel center = new JPanel(new GridLayout(2, 2, 14, 14));
        center.setOpaque(false);
        center.add(buildMapStatCard("按状态分布", Global.statsService.ticketCountByStatus()));
        center.add(buildMapStatCard("按类别分布", Global.statsService.ticketCountByCategory()));
        center.add(buildMapStatCard("按优先级分布", Global.statsService.ticketCountByPriority()));
        center.add(buildMapStatCard("按情绪分布", Global.statsService.ticketCountByEmotion()));
        panel.add(center, BorderLayout.CENTER);

        // 底部: 平均响应时长
        Double avg = Global.statsService.avgResponseSeconds();
        String avgText;
        if (avg == null) {
            avgText = "暂无数据";
        } else if (avg >= 3600) {
            avgText = String.format("%.1f 小时", avg / 3600.0);
        } else {
            avgText = String.format("%.0f 秒", avg);
        }

        JLabel avgLabel = new JLabel(avgText, SwingConstants.CENTER);
        avgLabel.setFont(UITheme.FONT_BIG);
        avgLabel.setForeground(UITheme.PRIMARY);
        avgLabel.setBorder(new EmptyBorder(10, 0, 10, 0));

        JPanel avgCard = UITheme.card("平均响应时长 (TIMESTAMPDIFF 聚合)", avgLabel);
        avgCard.setPreferredSize(new Dimension(0, 120));
        panel.add(avgCard, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel buildMapStatCard(String title, Map<String, Long> data) {
        String[] cols = {"项目", "数量"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = new JTable(model);
        table.setRowHeight(34);
        table.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(UITheme.BORDER);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionBackground(UITheme.TABLE_SELECT);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        table.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, 14));
        table.getTableHeader().setBackground(UITheme.TABLE_HEADER_BG);
        table.getTableHeader().setForeground(UITheme.TEXT_MUTED);

        if (data != null) {
            for (Map.Entry<String, Long> e : data.entrySet()) {
                // 分组 key 是英文枚举 (open/urgent/negative...), 显示时翻译成中文
                model.addRow(new Object[]{UITheme.label(e.getKey()), e.getValue()});
            }
        }

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(new EmptyBorder(0, 0, 0, 0));
        sp.getViewport().setBackground(UITheme.CARD_BG);

        return UITheme.card(title, sp);
    }

    private void refreshStats() {
        tabs.setComponentAt(0, buildStatsPanel());
    }

    // ==================== Tab 2: 全部工单 ====================

    private JTable allTicketsTable;
    private DefaultTableModel allTicketsModel;

    private JPanel buildAllTicketsPanel() {
        JPanel panel = UITheme.page(new BorderLayout(0, 12), 18);
        panel.add(UITheme.sectionTitle("全部工单"), BorderLayout.NORTH);

        String[] cols = {"#", "状态", "优先级", "类别", "标题", "创建人", "处理人", "情绪", "创建时间"};
        allTicketsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        allTicketsTable = new JTable(allTicketsModel);
        UITheme.styleTable(allTicketsTable);
        UITheme.colorColumn(allTicketsTable, 1);
        UITheme.colorColumn(allTicketsTable, 2);
        UITheme.colorColumn(allTicketsTable, 7);

        allTicketsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        allTicketsTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        allTicketsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        allTicketsTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        allTicketsTable.getColumnModel().getColumn(4).setPreferredWidth(280);
        allTicketsTable.getColumnModel().getColumn(5).setPreferredWidth(110);
        allTicketsTable.getColumnModel().getColumn(6).setPreferredWidth(110);
        allTicketsTable.getColumnModel().getColumn(7).setPreferredWidth(100);
        allTicketsTable.getColumnModel().getColumn(8).setPreferredWidth(190);

        panel.add(UITheme.tableCard(allTicketsTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setOpaque(false);

        RoundedButton refreshBtn = UITheme.ghost("刷新");
        refreshBtn.addActionListener(e -> refreshAllTickets());
        btnPanel.add(refreshBtn);

        RoundedButton assignBtn = UITheme.primary("分配客服");
        assignBtn.addActionListener(e -> doAssign());
        btnPanel.add(assignBtn);

        RoundedButton priorityBtn = UITheme.ghost("改优先级");
        priorityBtn.addActionListener(e -> doChangePriority());
        btnPanel.add(priorityBtn);

        RoundedButton closeBtn = UITheme.danger("关闭工单");
        closeBtn.addActionListener(e -> doAdminClose());
        btnPanel.add(closeBtn);

        panel.add(btnPanel, BorderLayout.SOUTH);
        refreshAllTickets();
        return panel;
    }

    private void refreshAllTickets() {
        allTicketsModel.setRowCount(0);
        Map<Long, String> names = loadUserNames();
        List<Ticket> list = Global.ticketService.searchTickets(null, null, null, null, 1, 100);
        for (Ticket t : list) {
            allTicketsModel.addRow(new Object[]{
                    t.getId(), UITheme.label(t.getStatus()), UITheme.label(t.getPriority()), t.getCategory(),
                    t.getTitle(),
                    nameOf(names, t.getCreatorId()),
                    t.getAssigneeId() == null ? "-" : nameOf(names, t.getAssigneeId()),
                    UITheme.label(t.getEmotion()),
                    t.getCreateTime() != null ? t.getCreateTime().toString().replace("T", " ") : ""
            });
        }
    }

    /**
     * id -> 真实姓名 映射.
     * 注: 等 C 组员把 TicketDaoImpl.selectWithUserNamesById 的 JOIN 结果解析补完后,
     * 这里可以改成直接读 t.getCreatorName() / t.getAssigneeName(), 就不用再查一次用户表.
     */
    private Map<Long, String> loadUserNames() {
        Map<Long, String> map = new HashMap<>();
        for (String role : new String[]{"admin", "cs", "student"}) {
            // 含禁用账号, 否则禁用用户创建的工单会显示成 "#4"
            for (User u : Global.userService.listByRoleIncludeDisabled(role)) {
                map.put(u.getId(), u.getRealName() == null ? u.getUsername() : u.getRealName());
            }
        }
        return map;
    }

    private String nameOf(Map<Long, String> map, Long id) {
        if (id == null) return "-";
        String n = map.get(id);
        return n == null ? ("#" + id) : n;
    }

    private Ticket getSelectedTicket() {
        int row = allTicketsTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请选中一行"); return null; }
        Long tid = (Long) allTicketsModel.getValueAt(row, 0);
        return Global.ticketService.getById(tid);
    }

    private void doAssign() {
        Ticket t = getSelectedTicket();
        if (t == null) return;
        // 列出所有客服
        List<User> csList = Global.userService.listByRole("cs");
        if (csList.isEmpty()) { JOptionPane.showMessageDialog(this, "没有可用客服"); return; }
        String[] options = csList.stream().map(u -> u.getId() + " - " + u.getRealName() + "(" + u.getUsername() + ")").toArray(String[]::new);
        String selected = (String) JOptionPane.showInputDialog(this, "选择客服:", "分配工单 #" + t.getId(),
                JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
        if (selected == null) return;
        Long csId = Long.parseLong(selected.split(" - ")[0]);
        Global.ticketService.assignTicket(t.getId(), csId, Global.currentUser.getId());
        JOptionPane.showMessageDialog(this, "已分配给客服");
        refreshAllTickets();
    }

    private void doChangePriority() {
        Ticket t = getSelectedTicket();
        if (t == null) return;
        String p = pickPriority(t.getPriority());
        if (p == null) return;
        Global.ticketService.updatePriority(t.getId(), p, Global.currentUser.getId());
        refreshAllTickets();
    }

    /** 优先级选择框: 显示中文, 返回英文码 (库里存的还是英文) */
    private String pickPriority(String current) {
        final String[] codes  = {"low", "medium", "high", "urgent"};
        final String[] labels = {"低", "中", "高", "紧急"};
        String picked = (String) JOptionPane.showInputDialog(this, "新优先级:", "改优先级",
                JOptionPane.QUESTION_MESSAGE, null, labels, UITheme.label(current));
        if (picked == null) return null;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(picked)) return codes[i];
        }
        return null;
    }

    private void doAdminClose() {
        Ticket t = getSelectedTicket();
        if (t == null) return;
        int confirm = JOptionPane.showConfirmDialog(this, "关闭工单 #" + t.getId() + "?", "确认", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            Global.ticketService.closeTicket(t.getId(), Global.currentUser.getId());
            refreshAllTickets();
        }
    }

    // ==================== Tab 3: FAQ 管理 ====================

    private JTable faqTable;
    private DefaultTableModel faqModel;

    private JPanel buildFaqPanel() {
        JPanel panel = UITheme.page(new BorderLayout(0, 12), 18);
        panel.add(UITheme.sectionTitle("FAQ 知识库"), BorderLayout.NORTH);

        String[] cols = {"#", "问题", "类别", "命中次数", "状态"};
        faqModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        faqTable = new JTable(faqModel);
        UITheme.styleTable(faqTable);
        faqTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        faqTable.getColumnModel().getColumn(1).setPreferredWidth(520);
        faqTable.getColumnModel().getColumn(2).setPreferredWidth(140);
        faqTable.getColumnModel().getColumn(3).setPreferredWidth(120);
        faqTable.getColumnModel().getColumn(4).setPreferredWidth(100);

        panel.add(UITheme.tableCard(faqTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setOpaque(false);

        RoundedButton refreshBtn = UITheme.ghost("刷新");
        refreshBtn.addActionListener(e -> refreshFaq());
        btnPanel.add(refreshBtn);

        RoundedButton addBtn = UITheme.success("新增 FAQ");
        addBtn.addActionListener(e -> doAddFaq());
        btnPanel.add(addBtn);

        RoundedButton editBtn = UITheme.primary("编辑");
        editBtn.addActionListener(e -> doEditFaq());
        btnPanel.add(editBtn);

        RoundedButton delBtn = UITheme.danger("删除");
        delBtn.addActionListener(e -> doDeleteFaq());
        btnPanel.add(delBtn);

        panel.add(btnPanel, BorderLayout.SOUTH);
        refreshFaq();
        return panel;
    }

    private void refreshFaq() {
        faqModel.setRowCount(0);
        List<Faq> list = Global.faqService.listAll();
        for (Faq f : list) {
            faqModel.addRow(new Object[]{
                    f.getId(), f.getQuestion(), f.getCategory(),
                    f.getHitCount(), f.getStatus() != null && f.getStatus() == 1 ? "启用" : "禁用"
            });
        }
    }

    private void doAddFaq() {
        JTextField qField = new JTextField(30);
        JTextArea aArea = new JTextArea(5, 30);
        aArea.setLineWrap(true);
        aArea.setWrapStyleWord(true);
        JTextField catField = new JTextField("通用", 10);
        JTextField kwField = new JTextField(20);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(UITheme.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; inputPanel.add(new JLabel("问题:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; inputPanel.add(qField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; inputPanel.add(new JLabel("答案:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.weighty = 1; gbc.fill = GridBagConstraints.BOTH;
        inputPanel.add(new JScrollPane(aArea), gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.weighty = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        inputPanel.add(new JLabel("类别:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; inputPanel.add(catField, gbc);
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0; inputPanel.add(new JLabel("关键词(逗号分隔):"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; inputPanel.add(kwField, gbc);

        int result = JOptionPane.showConfirmDialog(this, inputPanel, "新增 FAQ",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        Faq f = new Faq();
        f.setQuestion(qField.getText().trim());
        f.setAnswer(aArea.getText().trim());
        f.setCategory(catField.getText().trim());
        f.setKeywords(kwField.getText().trim());
        f.setStatus(1);  // 1 = 启用
        Global.faqService.create(f);
        refreshFaq();
    }

    private void doEditFaq() {
        int row = faqTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请选中一行"); return; }
        Long fid = (Long) faqModel.getValueAt(row, 0);
        Faq f = Global.faqService.getById(fid);
        if (f == null) return;

        JTextField qField = new JTextField(f.getQuestion(), 30);
        JTextArea aArea = new JTextArea(f.getAnswer(), 5, 30);
        aArea.setLineWrap(true);
        aArea.setWrapStyleWord(true);
        JTextField catField = new JTextField(f.getCategory(), 10);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(UITheme.CARD_BG);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0; inputPanel.add(new JLabel("问题:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; inputPanel.add(qField, gbc);
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; inputPanel.add(new JLabel("答案:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.weighty = 1; gbc.fill = GridBagConstraints.BOTH;
        inputPanel.add(new JScrollPane(aArea), gbc);
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.weighty = 0; gbc.fill = GridBagConstraints.HORIZONTAL;
        inputPanel.add(new JLabel("类别:"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; inputPanel.add(catField, gbc);

        int result = JOptionPane.showConfirmDialog(this, inputPanel, "编辑 FAQ #" + fid,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) return;

        f.setQuestion(qField.getText().trim());
        f.setAnswer(aArea.getText().trim());
        f.setCategory(catField.getText().trim());
        Global.faqService.update(f);
        refreshFaq();
    }

    private void doDeleteFaq() {
        int row = faqTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请选中一行"); return; }
        Long fid = (Long) faqModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "删除 FAQ #" + fid + "?", "确认", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            Global.faqService.delete(fid);
            refreshFaq();
        }
    }

    // ==================== Tab 4: 用户管理 ====================

    private JTable userTable;
    private DefaultTableModel userModel;

    private JPanel buildUserPanel() {
        JPanel panel = UITheme.page(new BorderLayout(0, 12), 18);
        panel.add(UITheme.sectionTitle("用户管理"), BorderLayout.NORTH);

        String[] cols = {"#", "用户名", "真实姓名", "角色", "状态"};
        userModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        userTable = new JTable(userModel);
        UITheme.styleTable(userTable);
        UITheme.colorColumn(userTable, 4);   // 状态列: 启用=绿 / 禁用=灰
        userTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        userTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        userTable.getColumnModel().getColumn(2).setPreferredWidth(220);
        userTable.getColumnModel().getColumn(3).setPreferredWidth(140);
        userTable.getColumnModel().getColumn(4).setPreferredWidth(140);

        panel.add(UITheme.tableCard(userTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setOpaque(false);

        RoundedButton refreshBtn = UITheme.ghost("刷新");
        refreshBtn.addActionListener(e -> refreshUsers());
        btnPanel.add(refreshBtn);

        RoundedButton toggleBtn = UITheme.primary("启用 / 禁用");
        toggleBtn.addActionListener(e -> doToggleUser());
        btnPanel.add(toggleBtn);

        panel.add(btnPanel, BorderLayout.SOUTH);
        refreshUsers();
        return panel;
    }

    private void refreshUsers() {
        userModel.setRowCount(0);
        // ⚠️ 用 IncludeDisabled 版本: 禁用的账号也必须列出来, 否则禁用后就再也找不到、无法恢复
        List<User> list = new java.util.ArrayList<>();
        list.addAll(Global.userService.listByRoleIncludeDisabled("admin"));
        list.addAll(Global.userService.listByRoleIncludeDisabled("cs"));
        list.addAll(Global.userService.listByRoleIncludeDisabled("student"));
        for (User u : list) {
            String statusText = u.getStatus() != null && u.getStatus() == 1 ? "active" : "disabled";
            userModel.addRow(new Object[]{
                    u.getId(), u.getUsername(), u.getRealName(), u.getRole(), UITheme.label(statusText)
            });
        }
    }

    private void doToggleUser() {
        int row = userTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请选中一行"); return; }
        Long uid = (Long) userModel.getValueAt(row, 0);
        User u = Global.userService.getById(uid);
        if (u == null) return;

        boolean enabled = u.getStatus() != null && u.getStatus() == 1;

        // 防呆 1: 不能禁用自己 —— 否则退出登录后连自己都进不来
        if (enabled && uid.equals(Global.currentUser.getId())) {
            JOptionPane.showMessageDialog(this,
                    "不能禁用当前登录的账号。\n如确实要停用, 请换另一个管理员账号操作。",
                    "操作被拒绝", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // 防呆 2: 系统必须保留至少一个启用的管理员, 否则整个系统没人能管理
        if (enabled && "admin".equals(u.getRole()) && countEnabledAdmins() <= 1) {
            JOptionPane.showMessageDialog(this,
                    "系统至少要保留一个启用的管理员账号。\n该操作会导致无人可管理系统。",
                    "操作被拒绝", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int newStatus = enabled ? 0 : 1;
        String action = enabled ? "禁用" : "启用";
        int confirm = JOptionPane.showConfirmDialog(this,
                "确定" + action + "账号「" + u.getUsername() + " (" + u.getRealName() + ")」?",
                "确认" + action, JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        boolean ok = Global.userService.updateStatus(uid, newStatus);
        if (!ok) {
            JOptionPane.showMessageDialog(this, "操作失败, 请查看控制台日志");
        }
        refreshUsers();
    }

    /** 当前启用的管理员数量 (listByRole 本身只返回 status=1 的账号) */
    private long countEnabledAdmins() {
        return Global.userService.listByRole("admin").size();
    }
}
