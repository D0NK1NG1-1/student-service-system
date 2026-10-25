import com.example.studentservice.Global;
import com.example.studentservice.ui.AdminFrame;
import com.example.studentservice.ui.CsFrame;
import com.example.studentservice.ui.LoginFrame;
import com.example.studentservice.ui.StudentFrame;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Enumeration;

/**
 * UI 外观截图工具（临时文件）
 * 用法: java -Dui.font=32 -cp ... UIShot
 * 直接复刻 Main 的全局字体逻辑, 然后把四个窗口离屏渲染成 PNG, 不弹窗不抢焦点.
 */
public class UIShot {

    public static void main(String[] args) throws Exception {
        // 和 Main 完全一致的初始化: 安装统一主题
        com.example.studentservice.ui.UITheme.install();
        new File("dselftest/shots").mkdirs();

        // 登录窗（不依赖登录态）
        LoginFrame lf = new LoginFrame();
        shot("1_login", lf, 0);

        // 登录窗 - 注册卡片 (反射切卡片, 检查"返回登录"链接)
        LoginFrame lf2 = new LoginFrame();
        java.lang.reflect.Field clF = LoginFrame.class.getDeclaredField("cardLayout");
        clF.setAccessible(true);
        java.lang.reflect.Field cpF = LoginFrame.class.getDeclaredField("cardPanel");
        cpF.setAccessible(true);
        ((CardLayout) clF.get(lf2)).show((JPanel) cpF.get(lf2), "register");
        shot("1b_register", lf2, 0);

        // 学生端
        Global.currentUser = Global.userService.login("student01", "123456");
        StudentFrame sf = new StudentFrame();
        demoChat(sf, "chatPanel");
        shot("2_student", sf, 0);

        // 学生端 - 我的工单 Tab (检查中文状态列)
        StudentFrame sf2 = new StudentFrame();
        selectTab(sf2, 2);
        shot("5_student_tickets", sf2, 0);

        // 客服端
        Global.currentUser = Global.userService.login("cs001", "123456");
        CsFrame cf = new CsFrame();
        demoChat(cf, "chatPanel");
        shot("3_cs", cf, 0);

        // 管理端
        Global.currentUser = Global.userService.login("admin01", "123456");
        AdminFrame af = new AdminFrame();
        shot("4_admin", af, 0);

        // 管理端 - 用户管理 Tab (检查禁用账号是否仍可见)
        AdminFrame af2 = new AdminFrame();
        selectTab(af2, 3);
        shot("6_admin_users", af2, 0);

        System.out.println("done.");
    }

    /** 完全复刻 Main.main 里的字体覆盖逻辑 */
    private static void applyGlobalFont(int fontSize) {
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof FontUIResource) {
                FontUIResource oldFont = (FontUIResource) value;
                int newStyle = oldFont.getStyle();
                if (newStyle == 0) newStyle = Font.PLAIN;
                UIManager.put(key, new FontUIResource("微软雅黑", newStyle, fontSize));
            }
        }
        UIManager.put("TabbedPane.font",        new FontUIResource("微软雅黑", Font.PLAIN, fontSize + 2));
        UIManager.put("OptionPane.messageFont", new FontUIResource("微软雅黑", Font.PLAIN, fontSize));
        UIManager.put("OptionPane.buttonFont",  new FontUIResource("微软雅黑", Font.PLAIN, fontSize));
    }

    /** 反射切到指定 Tab */
    private static void selectTab(Object frame, int index) throws Exception {
        java.lang.reflect.Field f = frame.getClass().getDeclaredField("tabs");
        f.setAccessible(true);
        ((JTabbedPane) f.get(frame)).setSelectedIndex(index);
    }

    /** 反射拿到 chatPanel, 塞几条示例消息, 用于检查气泡渲染效果 */
    private static void demoChat(Object frame, String field) throws Exception {
        java.lang.reflect.Field f = frame.getClass().getDeclaredField(field);
        f.setAccessible(true);
        com.example.studentservice.ui.ChatPanel cp = (com.example.studentservice.ui.ChatPanel) f.get(frame);
        cp.addUser("图书馆几点开门?");
        cp.addAI("图书馆周一至周日 8:00 - 22:00，法定节假日开放时间请关注图书馆公告。",
                "来源: 本地 FAQ 命中　置信度: 0.95");
        cp.addUser("我手机丢在操场了，很着急，催了好几次都没人管");
        cp.addAI("建议先到操场失物招领处登记，并联系辅导员发布寻物启事。",
                "来源: LLM 生成　置信度: 0.82");
        cp.addSystem("这个问题建议转人工, 请到「创建工单」Tab 提交.");
    }

    private static void shot(String name, JFrame f, int fontSize) throws Exception {
        // 移到屏幕外再显示: Swing 需要真实 peer 才能正确绘制, 但用户看不到窗口
        f.setLocation(-6000, -6000);
        f.setVisible(true);
        Thread.sleep(500);
        f.validate();

        int w = f.getWidth(), h = f.getHeight();
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        f.getRootPane().paintAll(g2);
        g2.dispose();
        ImageIO.write(img, "png", new File("dselftest/shots/" + name + ".png"));
        System.out.println(name + "  " + w + "x" + h);
        f.dispose();
    }
}
