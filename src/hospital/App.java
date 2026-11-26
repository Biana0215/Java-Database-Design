package hospital;

import hospital.ui.LoginFrame;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 程序入口：设置外观与全局字体后打开登录窗口。
 */
public class App {

    // 统一界面字体
    private static final String FONT_NAME = "微软雅黑";
    private static final String FONT_NAME_ALT = "Microsoft YaHei";
    private static final int FONT_SIZE = 14;

    private App() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(App::start);
    }

    /**
     * 初始化界面环境并显示登录窗。
     */
    private static void start() {
        applyLookAndFeel();
        applyGlobalFont();
        new LoginFrame().setVisible(true);
    }

    /**
     * 使用系统外观，失败时保留默认外观继续启动。
     */
    private static void applyLookAndFeel() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // 外观不可用属于非致命问题，不影响程序运行
        }
    }

    /**
     * 把 UIManager 中所有字体类默认值统一换成 14 号微软雅黑。
     */
    private static void applyGlobalFont() {
        FontUIResource font = new FontUIResource(pickFont());

        // 直接遍历 UIManager 的键集合并 put 会抛并发修改异常，先收集键再统一替换
        // 判断依据是当前值是否为 Font，这样 OptionPane.messageFont 这类不以 .font 结尾的键也能覆盖
        List<Object> fontKeys = new ArrayList<>();
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key instanceof String && UIManager.get(key) instanceof Font) {
                fontKeys.add(key);
            }
        }
        for (Object key : fontKeys) {
            UIManager.put(key, font);
        }
    }

    /**
     * 挑选可用字体：优先微软雅黑，系统无此字体时退回默认界面字体。
     */
    private static Font pickFont() {
        Set<String> families = new HashSet<>();
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            families.add(name);
        }

        String family;
        if (families.contains(FONT_NAME)) {
            family = FONT_NAME;
        } else if (families.contains(FONT_NAME_ALT)) {
            family = FONT_NAME_ALT;
        } else {
            Font base = UIManager.getFont("Label.font");
            family = base != null ? base.getFamily() : Font.SANS_SERIF;
        }
        return new Font(family, Font.PLAIN, FONT_SIZE);
    }
}
