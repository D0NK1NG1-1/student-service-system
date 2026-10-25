import com.example.studentservice.ui.LoginFrame;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 登录/注册卡片切换测试 (临时文件)
 * 不弹窗, 直接在组件树里找到「立即注册」「返回登录」两个链接按钮并 doClick(),
 * 然后检查当前显示的是哪张卡片.
 */
public class LoginFlowTest {

    public static void main(String[] args) throws Exception {
        LoginFrame f = new LoginFrame();
        JPanel cardPanel = (JPanel) get(f, "cardPanel");

        System.out.println("初始可见卡片: " + visibleCard(cardPanel));

        JButton regLink = findButton(f, "立即注册");
        System.out.println("找到「立即注册」按钮: " + (regLink != null));
        if (regLink != null) regLink.doClick();
        System.out.println("点击后可见卡片: " + visibleCard(cardPanel));

        JButton backLink = findButton(f, "返回登录");
        System.out.println("找到「返回登录」按钮: " + (backLink != null));
        if (backLink != null) backLink.doClick();
        System.out.println("点击后可见卡片: " + visibleCard(cardPanel));

        boolean ok = regLink != null && backLink != null;
        System.out.println(ok ? "== 卡片切换链路正常 ==" : "== 有按钮没找到 ==");
        f.dispose();
    }

    /** 当前可见的卡片名 (CardLayout 下只有一张可见) */
    private static String visibleCard(JPanel cardPanel) {
        for (Component c : cardPanel.getComponents()) {
            if (c.isVisible()) {
                return c == cardPanel.getComponent(0) ? "登录卡片" : "注册卡片";
            }
        }
        return "无";
    }

    private static Object get(Object o, String field) throws Exception {
        java.lang.reflect.Field fl = o.getClass().getDeclaredField(field);
        fl.setAccessible(true);
        return fl.get(o);
    }

    /** 递归找文字包含 keyword 的按钮 (链接按钮用了 HTML, 文字里带 <u> 标签) */
    private static JButton findButton(Component root, String keyword) {
        List<Component> stack = new ArrayList<>();
        stack.add(root);
        while (!stack.isEmpty()) {
            Component c = stack.remove(stack.size() - 1);
            if (c instanceof JButton) {
                String t = ((JButton) c).getText();
                if (t != null && t.contains(keyword)) return (JButton) c;
            }
            if (c instanceof Container) {
                for (Component child : ((Container) c).getComponents()) stack.add(child);
            }
        }
        return null;
    }
}
