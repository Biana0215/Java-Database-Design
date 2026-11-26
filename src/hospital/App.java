package hospital;

import com.formdev.flatlaf.FlatLightLaf;
import hospital.ui.LoginFrame;
import hospital.ui.common.UiTheme;

import javax.swing.SwingUtilities;

/**
 * 程序入口：安装 FlatLaf 浅色主题与全局视觉参数后打开登录窗口。
 */
public class App {

    private App() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::start);
    }

    /**
     * 初始化界面环境并显示登录窗。
     * 顺序不可颠倒：先装 FlatLaf，再由 UiTheme 覆盖具体视觉参数。
     */
    private static void start() {
        FlatLightLaf.setup();
        UiTheme.apply();
        new LoginFrame().setVisible(true);
    }
}
