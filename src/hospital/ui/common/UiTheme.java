package hospital.ui.common;

import javax.swing.BorderFactory;
import javax.swing.UIManager;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 全局视觉参数中心：颜色常量、字体工具与 UIManager 主题注入。
 * <p>
 * 在 FlatLightLaf.setup() 之后调用 {@link #apply()}，13 个业务面板无需改代码即可统一换肤。
 */
public final class UiTheme {

    // ===== 主色与语义色 =====
    /** 医疗蓝，主色。 */
    public static final Color PRIMARY = new Color(0x1E6FD9);
    /** 主色深色，用于顶部栏与渐变起点。 */
    public static final Color PRIMARY_DARK = new Color(0x1552A8);
    /** 主色浅底，用于菜单选中项。 */
    public static final Color PRIMARY_SOFT = new Color(0xE8F1FD);
    /** 成功。 */
    public static final Color SUCCESS = new Color(0x15803D);
    /** 警告。 */
    public static final Color WARNING = new Color(0xB45309);
    /** 危险。 */
    public static final Color DANGER = new Color(0xB91C1C);
    /** 次要文字灰。 */
    public static final Color TEXT_MUTED = new Color(0x6B7280);
    /** 主要文字色。 */
    public static final Color TEXT_PRIMARY = new Color(0x1F2733);

    // ===== 背景与分隔 =====
    /** 窗口背景。 */
    public static final Color BACKGROUND = new Color(0xF5F7FA);
    /** 卡片/表格白底。 */
    public static final Color SURFACE = Color.WHITE;
    /** 分隔线。 */
    public static final Color DIVIDER = new Color(0xE5E9F0);
    /** 表格斑马纹。 */
    public static final Color ZEBRA = new Color(0xF7F9FC);
    /** 表头浅灰底。 */
    public static final Color TABLE_HEADER_BG = new Color(0xEEF2F7);

    private static final String FONT_FAMILY = "微软雅黑";
    private static final String FONT_FAMILY_ALT = "Microsoft YaHei";
    private static final int FONT_SIZE = 14;
    private static final int ARC = 8;
    private static final int ROW_HEIGHT = 30;
    private static final int HEADER_HEIGHT = 34;

    /** 字体族只探测一次。 */
    private static String fontFamily;

    private UiTheme() {
    }

    /**
     * 安装现代浅色主题参数，必须紧跟 FlatLightLaf.setup() 之后调用。
     */
    public static void apply() {
        applyGlobalFont();

        // ---- 圆角与焦点 ----
        UIManager.put("Component.arc", ARC);
        UIManager.put("Button.arc", ARC);
        UIManager.put("TextComponent.arc", ARC);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);

        // ---- 边框与强调色 ----
        UIManager.put("Component.accentColor", color(PRIMARY));
        UIManager.put("Component.borderColor", color(DIVIDER));
        UIManager.put("Component.disabledBorderColor", color(new Color(0xE9EDF3)));
        UIManager.put("Component.focusedBorderColor", color(PRIMARY));
        UIManager.put("Component.focusColor", color(PRIMARY_SOFT));

        // ---- 按钮：默认高约 34px ----
        UIManager.put("Button.margin", new Insets(7, 16, 7, 16));
        UIManager.put("Button.default.background", color(PRIMARY));
        UIManager.put("Button.default.foreground", color(Color.WHITE));
        UIManager.put("Button.default.focusColor", color(PRIMARY_DARK));
        UIManager.put("Button.default.borderColor", color(PRIMARY_DARK));
        UIManager.put("Button.default.hoverBackground", color(new Color(0x1A64C4)));

        // ---- 输入框：默认高约 32px ----
        UIManager.put("TextField.margin", new Insets(6, 10, 6, 10));
        UIManager.put("PasswordField.margin", new Insets(6, 10, 6, 10));
        UIManager.put("TextField.placeholderForeground", color(TEXT_MUTED));
        UIManager.put("Component.placeholderForeground", color(TEXT_MUTED));

        // ---- 表格 ----
        UIManager.put("Table.rowHeight", ROW_HEIGHT);
        UIManager.put("Table.showHorizontalLines", Boolean.TRUE);
        UIManager.put("Table.showVerticalLines", Boolean.FALSE);
        UIManager.put("Table.gridColor", color(DIVIDER));
        UIManager.put("Table.background", color(SURFACE));
        UIManager.put("Table.foreground", color(TEXT_PRIMARY));
        // 主色 15% 叠加白底的实色近似，避免个别 LAF 不支持 alpha 时变成实心深色
        UIManager.put("Table.selectionBackground", color(blend(PRIMARY, SURFACE, 0.15)));
        UIManager.put("Table.selectionForeground", color(TEXT_PRIMARY));
        UIManager.put("Table.focusCellHighlightBorder", BorderFactory.createEmptyBorder());

        // ---- 表头 ----
        UIManager.put("TableHeader.background", color(TABLE_HEADER_BG));
        UIManager.put("TableHeader.foreground", color(TEXT_PRIMARY));
        UIManager.put("TableHeader.font", fontResource(Font.BOLD, FONT_SIZE));
        UIManager.put("TableHeader.separatorColor", color(DIVIDER));
        UIManager.put("TableHeader.height", HEADER_HEIGHT);
        UIManager.put("TableHeader.cellMargins", new Insets(0, 8, 0, 8));

        // ---- 分组边框标题（各面板的 TitledBorder 统一走这里）----
        UIManager.put("TitledBorder.font", fontResource(Font.BOLD, FONT_SIZE));
        UIManager.put("TitledBorder.titleColor", color(TEXT_PRIMARY));
        UIManager.put("TitledBorder.border",
                BorderFactory.createLineBorder(DIVIDER, 1, true));

        // ---- 面板与弹窗 ----
        UIManager.put("Panel.background", color(BACKGROUND));
        UIManager.put("OptionPane.background", color(SURFACE));
        UIManager.put("OptionPane.messageFont", font(Font.PLAIN, FONT_SIZE));
        UIManager.put("OptionPane.buttonFont", font(Font.PLAIN, FONT_SIZE));

        // ---- 滚动条与分隔条 ----
        UIManager.put("ScrollBar.width", 10);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.thumbInsets", new Insets(2, 2, 2, 2));
        UIManager.put("ScrollBar.showButtons", Boolean.FALSE);
        UIManager.put("ScrollPane.border", BorderFactory.createEmptyBorder());
        UIManager.put("SplitPane.dividerSize", 1);
        UIManager.put("SplitPane.background", color(DIVIDER));
    }

    /**
     * 字体工具：指定字号与是否加粗，字体族固定为微软雅黑。
     */
    public static Font titleFont(int size, boolean bold) {
        return font(bold ? Font.BOLD : Font.PLAIN, size);
    }

    /**
     * 字体工具：指定字重样式与字号。
     */
    public static Font font(int style, int size) {
        return new Font(family(), style, size);
    }

    /**
     * 按比例混合两色，ratio 为 color 占比（0~1）。
     */
    public static Color blend(Color color, Color base, double ratio) {
        double r = Math.max(0, Math.min(1, ratio));
        int red = (int) Math.round(color.getRed() * r + base.getRed() * (1 - r));
        int green = (int) Math.round(color.getGreen() * r + base.getGreen() * (1 - r));
        int blue = (int) Math.round(color.getBlue() * r + base.getBlue() * (1 - r));
        return new Color(red, green, blue);
    }

    /**
     * 把 UIManager 中所有字体类默认值统一换成 14 号微软雅黑；
     * 判断依据是值类型为 Font，这样 OptionPane.messageFont 这类不以 .font 结尾的键也能覆盖。
     */
    private static void applyGlobalFont() {
        FontUIResource base = new FontUIResource(family(), Font.PLAIN, FONT_SIZE);

        // 直接边遍历边 put 会抛并发修改异常，先收集键再统一替换
        List<Object> fontKeys = new ArrayList<>();
        Enumeration<Object> keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key instanceof String && UIManager.get(key) instanceof Font) {
                fontKeys.add(key);
            }
        }
        for (Object key : fontKeys) {
            UIManager.put(key, base);
        }
    }

    /**
     * 挑选可用字体族：优先微软雅黑，其次 Microsoft YaHei，都没有则沿用系统默认界面字体。
     */
    private static String family() {
        if (fontFamily != null) {
            return fontFamily;
        }
        Set<String> families = new HashSet<>();
        try {
            for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
                families.add(name);
            }
        } catch (Exception ignored) {
            // 无字体环境时退回默认，不影响启动
        }
        if (families.contains(FONT_FAMILY)) {
            fontFamily = FONT_FAMILY;
        } else if (families.contains(FONT_FAMILY_ALT)) {
            fontFamily = FONT_FAMILY_ALT;
        } else {
            Font base = UIManager.getFont("Label.font");
            fontFamily = base != null ? base.getFamily() : Font.SANS_SERIF;
        }
        return fontFamily;
    }

    private static ColorUIResource color(Color color) {
        return new ColorUIResource(color);
    }

    private static FontUIResource fontResource(int style, int size) {
        return new FontUIResource(family(), style, size);
    }
}
