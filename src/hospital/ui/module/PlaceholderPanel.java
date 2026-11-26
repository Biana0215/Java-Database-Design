package hospital.ui.module;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;

/**
 * 模块占位面板，后续由各成员替换成真实业务面板，构造签名保持不变。
 */
public class PlaceholderPanel extends JPanel {

    private static final String DEFAULT_TITLE = "未命名模块";

    private final String title;

    public PlaceholderPanel() {
        this(DEFAULT_TITLE);
    }

    public PlaceholderPanel(String title) {
        this.title = title == null || title.trim().isEmpty() ? DEFAULT_TITLE : title.trim();
        setLayout(new BorderLayout());

        JLabel label = new JLabel("【" + this.title + "】界面待实现", SwingConstants.CENTER);
        label.setForeground(Color.GRAY);
        label.setFont(new Font("微软雅黑", Font.PLAIN, 16));
        add(label, BorderLayout.CENTER);
    }

    public String getTitle() {
        return title;
    }
}
