import com.example.studentservice.Global;
import com.example.studentservice.dao.UserDao;
import com.example.studentservice.dao.impl.UserDaoImpl;
import com.example.studentservice.entity.User;
import com.example.studentservice.service.UserService;
import com.example.studentservice.service.impl.UserServiceImpl;
import com.example.studentservice.ui.LoginFrame;

import javax.swing.*;
import java.util.List;

/**
 * 「禁用账号」链路回归测试 (临时文件)
 * <p>
 * 覆盖 4 个曾经出问题/容易出问题的点:
 *   1. 禁用后 selectByUsername 仍能查到 (否则登录只会提示"密码错误", 用户不知道是被禁用)
 *   2. 禁用后管理端列表仍能看到 (否则没有入口恢复 —— 原 bug 的根因)
 *   3. 禁用后登录被拦下, 且提示"账号已被禁用"
 *   4. 恢复后能正常登录
 */
public class DisableFlowTest {

    private static int pass = 0, fail = 0;

    private static void check(String name, boolean ok, String detail) {
        if (ok) { pass++; System.out.println("  [PASS] " + name + (detail.isEmpty() ? "" : "  (" + detail + ")")); }
        else    { fail++; System.out.println("  [FAIL] " + name + "  -> " + detail); }
    }

    public static void main(String[] args) throws Exception {
        UserService userService = new UserServiceImpl();
        UserDao userDao = new UserDaoImpl();

        String uname = "tmp_disable_test";
        // 清理上次残留
        User old = userDao.selectByUsername(uname);
        if (old != null) userDao.deleteById(old.getId());

        User u = userService.register(uname, "123456", "student", "临时测试账号");
        System.out.println("测试账号已创建: id=" + u.getId() + ", username=" + uname);

        // ---------- 禁用 ----------
        boolean ok = userService.updateStatus(u.getId(), 0);
        check("禁用账号", ok, "");

        User found = userDao.selectByUsername(uname);
        check("禁用后 selectByUsername 仍能查到 (登录可给明确提示)", found != null,
                found == null ? "查不到 —— 登录只会提示密码错误" : "status=" + found.getStatus());

        boolean inAdminList = containsId(userService.listByRoleIncludeDisabled("student"), u.getId());
        check("禁用后仍出现在管理端用户列表 (可恢复)", inAdminList,
                inAdminList ? "" : "列表里看不到 —— 禁用后无法恢复, 就是原 bug");

        boolean inNormalList = containsId(userService.listByRole("student"), u.getId());
        check("禁用后不出现在普通角色列表 (分配客服等场景)", !inNormalList, "");

        // ---------- 登录拦截 ----------
        Global.currentUser = null;
        LoginFrame lf = new LoginFrame();
        set(lf, "loginUserField", JTextField.class).setText(uname);
        set(lf, "loginPassField", JPasswordField.class).setText("123456");
        call(lf, "doLogin");

        JLabel status = (JLabel) get(lf, "loginStatusLabel");
        check("禁用账号登录被拦截", Global.currentUser == null, "currentUser=" + Global.currentUser);
        check("提示文案明确 (不是笼统的密码错误)", status.getText().contains("禁用"), status.getText());
        lf.dispose();

        // ---------- 恢复 ----------
        userService.updateStatus(u.getId(), 1);
        check("恢复后能正常登录", userService.login(uname, "123456") != null, "");

        // ---------- 清理 ----------
        userDao.deleteById(u.getId());
        System.out.println("测试账号已删除");

        System.out.println("\n==== 结果: " + pass + " 通过 / " + fail + " 失败 ====");
    }

    private static boolean containsId(List<User> list, Long id) {
        for (User x : list) if (x.getId().equals(id)) return true;
        return false;
    }

    private static Object get(Object o, String field) throws Exception {
        java.lang.reflect.Field f = o.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return f.get(o);
    }

    @SuppressWarnings("unchecked")
    private static <T> T set(Object o, String field, Class<T> type) throws Exception {
        java.lang.reflect.Field f = o.getClass().getDeclaredField(field);
        f.setAccessible(true);
        return (T) f.get(o);
    }

    private static void call(Object o, String method) throws Exception {
        java.lang.reflect.Method m = o.getClass().getDeclaredMethod(method);
        m.setAccessible(true);
        m.invoke(o);
    }
}
