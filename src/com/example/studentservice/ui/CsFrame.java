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
 * 客服主窗口 —— 三个 Tab:
 *   1. 分配给我的工单 (JTable + 状态流转按钮 + 详情区)
 *   2. 全部未关闭工单 (可以认领处理)
 *   3. AI 辅助问答 (客服自己用)
 */
public class CsFrame extends JFrame {

    private JTabbedPane tabs;

    // Tab 1
    private JTable assignedTable;
    private DefaultTableModel assignedModel;
    private JTextArea detailArea;

    // Tab 2
    private JTable allOpenTable;
    private DefaultTableModel allOpenModel;

    public CsFrame() {
        super("客服工作台 — " + Global.currentUser.getRealName());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        int w = Math.max(1020, (int)(screen.width * 0.80));
        int h = Math.max(720, (int)(screen.height * 0.80));
        setSize(w, h);
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(840, 620));
        getContentPane().setBackground(UITheme.BG);

        tabs = new JTabbedPane();
        tabs.setFont(UITheme.FONT_BASE);
        tabs.addTab("分配给我的工单", buildAssignedPanel());
        tabs.addTab("全部未关闭工单", buildAllOpenPanel());
        tabs.addTab("AI 辅助", buildAIPanel());
        add(tabs, BorderLayout.CENTER);

        add(UITheme.header("客服工作台　" + Global.currentUser.getRealName(), () -> {
            Global.currentUser = null;
            new LoginFrame().setVisible(true);
            this.dispose();
        }), BorderLayout.NORTH);
    }

    // ==================== Tab 1: 分配给我的工单 ====================

    private JPanel buildAssignedPanel() {
        JPanel panel = UITheme.page(new BorderLayout(0, 12), 18);
        panel.add(UITheme.sectionTitle("分配给我的工单"), BorderLayout.NORTH);

        String[] cols = {"#", "状态", "优先级", "类别", "标题", "情绪", "创建时间"};
        assignedModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        assignedTable = new JTable(assignedModel);
        UITheme.styleTable(assignedTable);
        UITheme.colorColumn(assignedTable, 1);
        UITheme.colorColumn(assignedTable, 2);
        UITheme.colorColumn(assignedTable, 5);

        assignedTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        assignedTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        assignedTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        assignedTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        assignedTable.getColumnModel().getColumn(4).setPreferredWidth(300);
        assignedTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        assignedTable.getColumnModel().getColumn(6).setPreferredWidth(190);

        panel.add(UITheme.tableCard(assignedTable), BorderLayout.CENTER);

        // 底部: 左详情 + 右按钮
        JPanel bottom = new JPanel(new BorderLayout(12, 0));
        bottom.setOpaque(false);

        detailArea = new JTextArea(6, 40);
        detailArea.setEditable(false);
        detailArea.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        detailArea.setBackground(UITheme.CARD_BG);
        detailArea.setForeground(UITheme.TEXT);
        detailArea.setBorder(new EmptyBorder(8, 10, 8, 10));
        detailArea.setText("选中一行工单, 点「查看详情」看完整内容.");
        JScrollPane detailScroll = new JScrollPane(detailArea);
        detailScroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        detailScroll.setPreferredSize(new Dimension(0, 190));

        RoundedPanel detailCard = new RoundedPanel(new BorderLayout());
        detailCard.setBorder(new EmptyBorder(6, 6, 6, 6));
        detailCard.add(detailScroll, BorderLayout.CENTER);
        bottom.add(detailCard, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new GridLayout(3, 2, 8, 8));
        btnPanel.setOpaque(false);
        btnPanel.setPreferredSize(new Dimension(420, 190));

        RoundedButton detailBtn = UITheme.ghost("查看详情");
        detailBtn.addActionListener(e -> showTicketDetail());
        btnPanel.add(detailBtn);

        RoundedButton refreshBtn = UITheme.ghost("刷新列表");
        refreshBtn.addActionListener(e -> refreshAssigned());
        btnPanel.add(refreshBtn);

        RoundedButton startBtn = UITheme.primary("开始处理");
        startBtn.addActionListener(e -> doStartProcessing());
        btnPanel.add(startBtn);

        RoundedButton replyBtn = UITheme.primary("回复用户");
        replyBtn.addActionListener(e -> doReply());
        btnPanel.add(replyBtn);

        RoundedButton priorityBtn = UITheme.ghost("改优先级");
        priorityBtn.addActionListener(e -> doChangePriority());
        btnPanel.add(priorityBtn);

        RoundedButton closeBtn = UITheme.danger("关闭工单");
        closeBtn.addActionListener(e -> doClose());
        btnPanel.add(closeBtn);

        bottom.add(btnPanel, BorderLayout.EAST);
        panel.add(bottom, BorderLayout.SOUTH);

        refreshAssigned();
        return panel;
    }

    private void refreshAssigned() {
        assignedModel.setRowCount(0);
        List<Ticket> list = Global.ticketService.listMyAssigned(Global.currentUser.getId());
        for (Ticket t : list) {
            assignedModel.addRow(new Object[]{
                    t.getId(), UITheme.label(t.getStatus()), UITheme.label(t.getPriority()), t.getCategory(),
                    t.getTitle(), UITheme.label(t.getEmotion()),
                    t.getCreateTime() != null ? t.getCreateTime().toString().replace("T", " ") : ""
            });
        }
    }

    private Ticket getSelected(JTable table, DefaultTableModel model) {
        int row = table.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请先选中一行"); return null; }
        Long tid = (Long) model.getValueAt(row, 0);
        return Global.ticketService.getById(tid);
    }

    private void showTicketDetail() {
        Ticket t = getSelected(assignedTable, assignedModel);
        if (t == null) return;
        StringBuilder sb = new StringBuilder();
        sb.append("工单 #").append(t.getId()).append("　").append(t.getTitle()).append("\n");
        sb.append("类别: ").append(t.getCategory())
          .append("　优先级: ").append(UITheme.label(t.getPriority()))
          .append("　状态: ").append(UITheme.label(t.getStatus()))
          .append("　情绪: ").append(UITheme.label(t.getEmotion())).append("\n");
        sb.append("创建人 ID: ").append(t.getCreatorId())
          .append("　处理人 ID: ").append(t.getAssigneeId() == null ? "-" : t.getAssigneeId()).append("\n");
        sb.append("创建时间: ").append(t.getCreateTime())
          .append("　关闭时间: ").append(t.getCloseTime() == null ? "-" : t.getCloseTime()).append("\n");
        sb.append("\n问题描述:\n").append(t.getContent());
        detailArea.setText(sb.toString());
        detailArea.setCaretPosition(0);
    }

    private void doStartProcessing() {
        Ticket t = getSelected(assignedTable, assignedModel);
        if (t == null) return;
        boolean ok = Global.ticketService.startProcessing(t.getId(), Global.currentUser.getId());
        JOptionPane.showMessageDialog(this, ok ? "已开始处理" : "操作失败 (可能已关闭或已在处理)");
        refreshAssigned();
    }

    private void doReply() {
        Ticket t = getSelected(assignedTable, assignedModel);
        if (t == null) return;
        String msg = JOptionPane.showInputDialog(this, "输入回复内容:", "回复工单 #" + t.getId());
        if (msg == null || msg.trim().isEmpty()) return;
        boolean ok = Global.ticketService.replyToCustomer(t.getId(), Global.currentUser.getId(), msg.trim());
        JOptionPane.showMessageDialog(this, ok ? "已回复, 状态变为待用户" : "操作失败");
        refreshAssigned();
    }

    private void doClose() {
        Ticket t = getSelected(assignedTable, assignedModel);
        if (t == null) return;
        int confirm = JOptionPane.showConfirmDialog(this, "确定关闭工单 #" + t.getId() + "?", "确认", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;
        boolean ok = Global.ticketService.closeTicket(t.getId(), Global.currentUser.getId());
        JOptionPane.showMessageDialog(this, ok ? "工单已关闭" : "操作失败");
        refreshAssigned();
    }

    private void doChangePriority() {
        Ticket t = getSelected(assignedTable, assignedModel);
        if (t == null) return;
        final String[] codes  = {"low", "medium", "high", "urgent"};
        final String[] labels = {"低", "中", "高", "紧急"};
        String picked = (String) JOptionPane.showInputDialog(this, "选择新优先级:", "改优先级",
                JOptionPane.QUESTION_MESSAGE, null, labels, UITheme.label(t.getPriority()));
        if (picked == null) return;
        String p = null;
        for (int i = 0; i < labels.length; i++) {
            if (labels[i].equals(picked)) { p = codes[i]; break; }
        }
        if (p == null) return;
        Global.ticketService.updatePriority(t.getId(), p, Global.currentUser.getId());
        JOptionPane.showMessageDialog(this, "优先级已更新为 " + UITheme.label(p));
        refreshAssigned();
    }

    // ==================== Tab 2: 全部未关闭工单 ====================

    private JPanel buildAllOpenPanel() {
        JPanel panel = UITheme.page(new BorderLayout(0, 12), 18);
        panel.add(UITheme.sectionTitle("全部未关闭工单"), BorderLayout.NORTH);

        String[] cols = {"#", "状态", "优先级", "类别", "标题", "情绪", "创建时间"};
        allOpenModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        allOpenTable = new JTable(allOpenModel);
        UITheme.styleTable(allOpenTable);
        UITheme.colorColumn(allOpenTable, 1);
        UITheme.colorColumn(allOpenTable, 2);
        UITheme.colorColumn(allOpenTable, 5);

        allOpenTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        allOpenTable.getColumnModel().getColumn(1).setPreferredWidth(130);
        allOpenTable.getColumnModel().getColumn(2).setPreferredWidth(100);
        allOpenTable.getColumnModel().getColumn(3).setPreferredWidth(90);
        allOpenTable.getColumnModel().getColumn(4).setPreferredWidth(300);
        allOpenTable.getColumnModel().getColumn(5).setPreferredWidth(100);
        allOpenTable.getColumnModel().getColumn(6).setPreferredWidth(190);

        panel.add(UITheme.tableCard(allOpenTable), BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setOpaque(false);

        RoundedButton refreshBtn = UITheme.ghost("刷新");
        refreshBtn.addActionListener(e -> refreshAllOpen());
        btnPanel.add(refreshBtn);

        RoundedButton claimBtn = UITheme.primary("认领此工单");
        claimBtn.addActionListener(e -> doClaim());
        btnPanel.add(claimBtn);

        panel.add(btnPanel, BorderLayout.SOUTH);
        refreshAllOpen();
        return panel;
    }

    private void refreshAllOpen() {
        allOpenModel.setRowCount(0);
        // 用 searchTickets 列出全部 (空关键词查全部, 然后 UI 层过滤掉 closed)
        List<Ticket> list = Global.ticketService.searchTickets(null, null, null, null, 1, 100);
        for (Ticket t : list) {
            if ("closed".equals(t.getStatus())) continue;
            allOpenModel.addRow(new Object[]{
                    t.getId(), UITheme.label(t.getStatus()), UITheme.label(t.getPriority()), t.getCategory(),
                    t.getTitle(), UITheme.label(t.getEmotion()),
                    t.getCreateTime() != null ? t.getCreateTime().toString().replace("T", " ") : ""
            });
        }
    }

    private void doClaim() {
        int row = allOpenTable.getSelectedRow();
        if (row < 0) { JOptionPane.showMessageDialog(this, "请选中一行"); return; }
        Long tid = (Long) allOpenModel.getValueAt(row, 0);
        Global.ticketService.assignTicket(tid, Global.currentUser.getId(), Global.currentUser.getId());
        JOptionPane.showMessageDialog(this, "已认领工单 #" + tid);
        refreshAllOpen();
        refreshAssigned();
    }

    // ==================== Tab 3: AI 辅助 ====================

    private ChatPanel chatPanel;
    private JTextField aiInput;

    private JPanel buildAIPanel() {
        JPanel panel = UITheme.page(new BorderLayout(0, 12), 18);
        panel.add(UITheme.sectionTitle("AI 辅助 (客服内部使用)"), BorderLayout.NORTH);

        chatPanel = new ChatPanel();
        chatPanel.addSystem("客服 AI 辅助已就绪, 输入学生问题, AI 帮你快速定位答案.");

        JPanel chatCard = new RoundedPanel(new BorderLayout());
        chatCard.setBorder(new EmptyBorder(6, 6, 6, 6));
        chatCard.add(chatPanel, BorderLayout.CENTER);
        panel.add(chatCard, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(10, 0));
        bottom.setOpaque(false);

        aiInput = new JTextField();
        aiInput.setFont(UITheme.FONT_BASE);
        aiInput.setPreferredSize(new Dimension(0, 46));
        aiInput.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.BORDER),
                new EmptyBorder(6, 12, 6, 12)));
        aiInput.addActionListener(e -> sendAI());
        bottom.add(aiInput, BorderLayout.CENTER);

        RoundedButton sendBtn = UITheme.primary("发送");
        sendBtn.addActionListener(e -> sendAI());
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        right.setOpaque(false);
        right.add(sendBtn);
        bottom.add(right, BorderLayout.EAST);

        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private void sendAI() {
        String q = aiInput.getText().trim();
        if (q.isEmpty()) return;
        chatPanel.addUser(q);
        aiInput.setText("");
        chatPanel.setTyping(true);

        new SwingWorker<AIUtil.AnswerResult, Void>() {
            @Override
            protected AIUtil.AnswerResult doInBackground() {
                return Global.aiService.chat(q, Global.currentUser.getId());
            }
            @Override
            protected void done() {
                chatPanel.setTyping(false);
                try {
                    AIUtil.AnswerResult r = get();
                    String source = "faq".equals(r.source) ? "本地 FAQ 命中" : "LLM 生成";
                    chatPanel.addAI(r.answer, "来源: " + source
                            + "　置信度: " + String.format("%.2f", r.confidence));
                } catch (Exception ex) {
                    chatPanel.addAI("调用失败: " + ex.getMessage(), null);
                }
            }
        }.execute();
    }
}
