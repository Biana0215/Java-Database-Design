package hospital.ui.common;

import javax.swing.JOptionPane;
import javax.swing.JTextField;
import java.awt.Component;

/**
 * 弹窗与输入取值的统一入口，界面层不再直接使用 JOptionPane。
 */
public final class UiUtil {

    private static final String TITLE_INFO = "提示";
    private static final String TITLE_ERROR = "操作失败";
    private static final String TITLE_WARN = "警告";
    private static final String TITLE_CONFIRM = "确认";

    private UiUtil() {
    }

    public static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, text(message, ""), TITLE_INFO,
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, text(message, "操作失败，请稍后重试"), TITLE_ERROR,
                JOptionPane.ERROR_MESSAGE);
    }

    public static void warn(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, text(message, ""), TITLE_WARN,
                JOptionPane.WARNING_MESSAGE);
    }

    /**
     * 是/否确认框，选择“是”返回 true。
     */
    public static boolean confirm(Component parent, String message) {
        int result = JOptionPane.showConfirmDialog(parent, text(message, ""), TITLE_CONFIRM,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return result == JOptionPane.YES_OPTION;
    }

    /**
     * 取输入框去掉首尾空白的文本，为空时返回 null，由调用方决定提示语。
     */
    public static String requireText(JTextField field, String fieldName) {
        if (field == null) {
            return null;
        }
        String value = field.getText();
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim();
    }

    /**
     * 展示异常：优先显示 message，没有则显示异常类名。
     */
    public static void showException(Component parent, Exception e) {
        if (e == null) {
            error(parent, "发生未知错误");
            return;
        }
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) {
            message = e.getClass().getSimpleName();
        }
        error(parent, message);
    }

    private static String text(String message, String fallback) {
        if (message == null || message.trim().isEmpty()) {
            return fallback;
        }
        return message;
    }
}
