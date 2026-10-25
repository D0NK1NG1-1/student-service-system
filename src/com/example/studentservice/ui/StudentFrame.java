package com.example.studentservice.ui;

import com.example.studentservice.Global;
import com.example.studentservice.entity.Ticket;
import com.example.studentservice.util.AIUtil;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * 学生主窗口 —— 三个 Tab:
 *   1. AI 自助问答 (气泡聊天界面, SwingWorker 包装, 不阻塞 UI)
 *   2. 创建工单
 *   3. 我的工单 (JTable 展示 + 状态按钮)
 */
public class StudentFrame extends JFrame {

    private JTabbedPane tabs;

    public StudentFrame() {
        super("学生工作台 — " + Global.currentUser.getRealName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.max(940, (int)(screen.width * 0.75));
        int h = Math.max(680, (int)(screen.height * 0.78));
        setSize(w, h);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(760, 560));
        getContentPane().setBackground(UITheme.BG);

        tabs = new JTabbedPane();
        tabs.setFont(UITheme.FONT_BASE);
        tabs.addTab("AI 自助问答", buildAIPanel());
        tabs.addTab("创建工单", buildCreatePanel());
        tabs.addTab("我的工单", buildMyTicketsPanel());
        add(tabs, BorderLayout.CENTER);

        add(UITheme.header("学生工作台　" + Global.currentUser.getRealName()
                + "　(" + Global.currentUser.getUsername() + ")", () -> {
            Global.currentUser = null;
            new LoginFrame().setVisible(true);
            this.dispose();
        }), BorderLayout.NORTH);
    }

    // ==================== AI 问答 Tab ====================

    private ChatPanel chatPanel;
    private JTextField aiInputField;
    private RoundedButton aiSendBtn;

    private JPanel buildAIPanel() {
        JPanel panel = UITheme.page(new BorderLayout(0, 12), 18);

        // 聊天区 (白底卡片)
        chatPanel = new ChatPanel();
        chatPanel.addSystem("你好! 我是校园智能助手, 可以问我图书馆时间、选课、宿舍、缴费等问题.");
        chatPanel.addSystem("AI 解决不了的问题, 可以直接在「创建工单」里提交给人工客服.");

        JPanel chatCard = new RoundedPanel(new BorderLayout());
        chatCard.setBorder(new EmptyBorder(6, 6, 6, 6));
        chatCard.add(chatPanel, BorderLayout.CENTER);
        panel.add(chatCard, BorderLayout.CENTER);

        // 底部输入区
        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setOpaque(false);

        aiInputField = new JTextField();
        aiInputField.setFont(UITheme.FONT_BASE);
        aiInputField.setPreferredSize(new Dimension(0, 46));
        aiInputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(6, 12, 6, 12)));
        aiInputField.addActionListener(e -> sendAIQuestion());
        bottom.add(aiInputField, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnPanel.setOpaque(false);

        aiSendBtn = UITheme.primary("发送");
        aiSendBtn.addActionListener(e -> sendAIQuestion());
        btnPanel.add(aiSendBtn);

        RoundedButton clearBtn = UITheme.ghost("清空");
        clearBtn.addActionListener(e -> {
            chatPanel.removeAll();
            chatPanel.addSystem("对话已清空, 可以重新提问.");
            chatPanel.revalidate();
            chatPanel.repaint();
        });
        btnPanel.add(clearBtn);

        bottom.add(btnPanel, BorderLayout.EAST);
        panel.add(bottom, BorderLayout.SOUTH);

        return panel;
    }

    private void sendAIQuestion() {
        String question = aiInputField.getText().trim();
        if (question.isEmpty()) return;

        chatPanel.addUser(question);
        aiInputField.setText("");
        aiSendBtn.setEnabled(false);
        chatPanel.setTyping(true);

        // ⚠️ 关键: 必须用 SwingWorker, 否则 AIUtil 的 HTTP 调用 (5-15秒) 会卡死 UI
        new SwingWorker<AIUtil.AnswerResult, Void>() {
            @Override
            protected AIUtil.AnswerResult doInBackground() throws Exception {
                return Global.aiService.chat(question, Global.currentUser.getId());
            }

            @Override
            protected void done() {
                chatPanel.setTyping(false);
                try {
                    AIUtil.AnswerResult result = get();
                    String source = "faq".equals(result.source) ? "本地 FAQ 命中" : "LLM 生成";
                    String meta = "来源: " + source
                            + "　置信度: " + String.format("%.2f", result.confidence)
                            + (result.needHuman ? "　建议转人工" : "");
                    chatPanel.addAI(result.answer, meta);

                    if (result.needHuman) {
                        chatPanel.addSystem("这个问题建议转人工, 请到「创建工单」Tab 提交.");
                    }
                } catch (Exception ex) {
                    chatPanel.addAI("服务暂不可用: " + ex.getMessage(), null);
                } finally {
                    aiSendBtn.setEnabled(true);
                    aiInputField.requestFocus();
                }
            }
        }.execute();
    }

    // ==================== 创建工单 Tab ====================

    private JTextField createTitleField;
    private JTextArea createContentArea;
    private JComboBox<String> createCategoryCombo;
    private JLabel createStatusLabel;

    private JPanel buildCreatePanel() {
        JPanel panel = UITheme.page(new BorderLayout(0, 12), 18);
        panel.add(UITheme.sectionTitle("提交新工单"), BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 14);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
        form.add(fieldLabel("工单标题"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.insets = new Insets(10, 0, 10, 0);
        createTitleField = new JTextField();
        createTitleField.setFont(UITheme.FONT_BASE);
        createTitleField.setPreferredSize(new Dimension(0, 42));
        createTitleField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER), new EmptyBorder(4, 10, 4, 10)));
        form.add(createTitleField, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0; gbc.insets = new Insets(10, 0, 10, 14);
        form.add(fieldLabel("类别"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.insets = new Insets(10, 0, 10, 0);
        createCategoryCombo = new JComboBox<>(new String[]{"教学", "宿舍", "网络", "图书馆", "财务", "其他"});
        createCategoryCombo.setFont(UITheme.FONT_BASE);
        createCategoryCombo.setPreferredSize(new Dimension(0, 42));
        form.add(createCategoryCombo, gbc);

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0; gbc.insets = new Insets(10, 0, 10, 14);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        form.add(fieldLabel("问题描述"), gbc);
        gbc.gridx = 1; gbc.weightx = 1; gbc.weighty = 1;
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.fill = GridBagConstraints.BOTH;
        createContentArea = new JTextArea(6, 30);
        createContentArea.setLineWrap(true);
        createContentArea.setWrapStyleWord(true);
        createContentArea.setFont(UITheme.FONT_BASE);
        createContentArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        JScrollPane contentScroll = new JScrollPane(createContentArea);
        contentScroll.setBorder(BorderFactory.createLineBorder(UITheme.BORDER));
        form.add(contentScroll, gbc);

        gbc.gridx = 0; gbc.gridy = 3; gbc.gridwidth = 2; gbc.weightx = 1; gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(16, 0, 6, 0);
        RoundedButton submitBtn = UITheme.success("提交工单");
        submitBtn.addActionListener(e -> doCreateTicket());
        form.add(submitBtn, gbc);

        gbc.gridy = 4; gbc.insets = new Insets(2, 0, 0, 0);
        createStatusLabel = new JLabel(" ", SwingConstants.CENTER);
        createStatusLabel.setFont(UITheme.FONT_SMALL);
        createStatusLabel.setForeground(UITheme.DANGER);
        form.add(createStatusLabel, gbc);

        JPanel card = UITheme.card(null, form);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(card, BorderLayout.CENTER);
        panel.add(wrap, BorderLayout.CENTER);
        return panel;
    }

    private void doCreateTicket() {
        String title = createTitleField.getText().trim();
        String content = createContentArea.getText().trim();
        String category = (String) createCategoryCombo.getSelectedItem();

        if (title.isEmpty() || content.isEmpty()) {
            createStatusLabel.setForeground(UITheme.DANGER);
            createStatusLabel.setText("请填写标题和描述");
            return;
        }

        try {
            Ticket t = Global.ticketService.createTicket(
                    Global.currentUser.getId(), title, content, category);
            if (t != null && t.getId() != null) {
                createStatusLabel.setForeground(UITheme.SUCCESS);
                createStatusLabel.setText("工单已提交! 编号 #" + t.getId() +
                        "　情绪: " + UITheme.label(t.getEmotion()) +
                        "　优先级: " + UITheme.label(t.getPriority()));
                createTitleField.setText("");
                createContentArea.setText("");
                // 刷新我的工单 Tab
                refreshMyTickets();
            } else {
                createStatusLabel.setForeground(UITheme.DANGER);
                createStatusLabel.setText("提交失败");
            }
        } catch (Exception ex) {
            createStatusLabel.setForeground(UITheme.DANGER);
            createStatusLabel.setText(ex.getMessage());
        }
    }

    // ==================== 我的工单 Tab ====================

    private JTable myTicketsTable;
    private DefaultTableModel myTicketsModel;

    private JPanel buildMyTicketsPanel() {
        JPanel panel = UITheme.page(new BorderLayout(0, 12), 18);
        panel.add(UITheme.sectionTitle("我的工单"), BorderLayout.NORTH);

        // 表格
        String[] cols = {"#", "状态", "优先级", "类别", "标题", "情绪", "创建时间"};
        myTicketsModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        myTicketsTable = new JTable(myTicketsModel);
        UITheme.styleTable(myTicketsTable);
        UITheme.colorColumn(myTicketsTable, 1);
        UITheme.colorColumn(myTicketsTable, 2);
        UITheme.colorColumn(myTicketsTable, 5);

        myTicketsTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        myTicketsTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        myTicketsTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        myTicketsTable.getColumnModel().getColumn(3).setPreferredWidth(100);
        myTicketsTable.getColumnModel().getColumn(4).setPreferredWidth(320);
        myTicketsTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        myTicketsTable.getColumnModel().getColumn(6).setPreferredWidth(190);

        panel.add(UITheme.tableCard(myTicketsTable), BorderLayout.CENTER);

        // 操作按钮
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setOpaque(false);

        RoundedButton refreshBtn = UITheme.ghost("刷新");
        refreshBtn.addActionListener(e -> refreshMyTickets());
        btnPanel.add(refreshBtn);

        RoundedButton reopenBtn = UITheme.primary("重新打开");
        reopenBtn.addActionListener(e -> doReopenMyTicket());
        btnPanel.add(reopenBtn);

        RoundedButton closeBtn = UITheme.danger("关闭选中工单");
        closeBtn.addActionListener(e -> doCloseMyTicket());
        btnPanel.add(closeBtn);

        panel.add(btnPanel, BorderLayout.SOUTH);

        refreshMyTickets();
        return panel;
    }

    private void refreshMyTickets() {
        myTicketsModel.setRowCount(0);
        List<Ticket> list = Global.ticketService.listMyTickets(Global.currentUser.getId());
        for (Ticket t : list) {
            myTicketsModel.addRow(new Object[]{
                    t.getId(),
                    UITheme.label(t.getStatus()),
                    UITheme.label(t.getPriority()),
                    t.getCategory(),
                    t.getTitle(),
                    UITheme.label(t.getEmotion()),
                    t.getCreateTime() != null ? t.getCreateTime().toString().replace("T", " ") : ""
            });
        }
    }

    private Ticket getSelectedTicket() {
        int row = myTicketsTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "请先选中一行工单");
            return null;
        }
        Long tid = (Long) myTicketsModel.getValueAt(row, 0);
        return Global.ticketService.getById(tid);
    }

    private void doCloseMyTicket() {
        Ticket t = getSelectedTicket();
        if (t == null) return;
        if ("closed".equals(t.getStatus())) {
            JOptionPane.showMessageDialog(this, "工单已关闭, 无需重复关闭");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "确定关闭工单 #" + t.getId() + "?", "确认", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            Global.ticketService.closeTicket(t.getId(), Global.currentUser.getId());
            refreshMyTickets();
        }
    }

    private void doReopenMyTicket() {
        Ticket t = getSelectedTicket();
        if (t == null) return;
        if (!"closed".equals(t.getStatus()) && !"pending_customer".equals(t.getStatus())) {
            JOptionPane.showMessageDialog(this, "只有 closed 或 pending_customer 可以重新打开");
            return;
        }
        Global.ticketService.reopenTicket(t.getId(), Global.currentUser.getId());
        refreshMyTickets();
    }

    private JLabel fieldLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UITheme.FONT_BOLD);
        l.setForeground(UITheme.TEXT);
        return l;
    }
}
