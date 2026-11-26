package hospital.ui;

import hospital.entity.SysUser;

/**
 * 保存当前登录用户，供各界面读取身份与角色。
 */
public class Session {

    private static SysUser currentUser;

    private Session() {
    }

    public static void setCurrentUser(SysUser user) {
        currentUser = user;
    }

    public static SysUser getCurrentUser() {
        return currentUser;
    }

    /**
     * 清除登录信息，退出登录时调用。
     */
    public static void clear() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    /**
     * 当前用户角色，未登录返回空串。
     */
    public static String getRole() {
        if (currentUser == null || currentUser.getRole() == null) {
            return "";
        }
        return currentUser.getRole();
    }

    /**
     * 顶部信息栏展示用：用户名（角色）。
     */
    public static String getDisplayName() {
        if (currentUser == null) {
            return "未登录";
        }
        String username = currentUser.getUsername() == null ? "" : currentUser.getUsername();
        String role = currentUser.getRole() == null ? "" : currentUser.getRole();
        return username + "（" + role + "）";
    }
}
