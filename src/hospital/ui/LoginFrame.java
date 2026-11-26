package hospital.ui;

import hospital.entity.SysUser;
import hospital.service.SysUserService;
import hospital.ui.common.UiTheme;
import hospital.ui.common.UiUtil;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.concurrent.ExecutionException;

/**
 * 登录窗口：左侧品牌区 + 右侧表单区的卡片式登录页。
 */
public class LoginFrame extends JFrame {

    private static final int BRAND_WIDTH = 300;
    private static final int FIELD_WIDTH = 300;
    private static final int FIELD_HEIGHT = 32;
    private static final int BUTTON_HEIGHT = 40;

    private final SysUserService userService = new SysUserService();

    private final JTextField usernameField = new JTextField(16);
    private final JPasswordField passwordField = new JPasswordField(16);
    private final JButton loginButton = new JButton("登录");
    private final JButton exitButton = new JButton("退出");

    // 防止登录过程中重复点击
    private boolean loggingIn;

    public LoginFrame() {
        super("医院挂号系统 - 登录");
        initUi();
    }

    private void initUi() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(UiTheme.SURFACE);
        root.add(buildBrandPanel(), BorderLayout.WEST);
        root.add(buildFormArea(), BorderLayout.CENTER);
        setContentPane(root);

        // 回车即登录：默认按钮 + 两个输入框各自绑定
        getRootPane().setDefaultButton(loginButton);
        loginButton.addActionListener(e -> doLogin());
        exitButton.addActionListener(e -> System.exit(0));
        usernameField.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> doLogin());

        setSize(720, 420);
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /**
     * 左侧品牌区：主色渐变底 + 系统名 + 一行业务说明。
     */
    private JPanel buildBrandPanel() {
        JPanel brand = new BrandPanel();
        brand.setLayout(new GridBagLayout());
        brand.setPreferredSize(new Dimension(BRAND_WIDTH, 0));
        brand.setBorder(BorderFactory.createEmptyBorder(0, 40, 0, 24));

        JPanel stack = new JPanel();
        stack.setOpaque(false);
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));

        LogoMark logo = new LogoMark();
        logo.setAlignmentX(Component.LEFT_ALIGNMENT);
        stack.add(logo);
        stack.add(Box.createVerticalStrut(22));

        JLabel title = new JLabel("医院挂号系统");
        title.setFont(UiTheme.titleFont(24, true));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        stack.add(title);
        stack.add(Box.createVerticalStrut(10));

        JLabel subtitle = new JLabel("门诊挂号 · 收费 · 统计");
        subtitle.setFont(UiTheme.font(Font.PLAIN, 13));
        subtitle.setForeground(new Color(255, 255, 255, 205));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        stack.add(subtitle);

        // 单组件 + 零权重，GridBagLayout 会把它整体居中
        brand.add(stack);
        return brand;
    }

    /**
     * 右侧表单区：标题、账号、密码、登录按钮与初始账号提示。
     */
    private JPanel buildFormArea() {
        exitButton.setFont(UiTheme.font(Font.PLAIN, 13));
        exitButton.setForeground(UiTheme.TEXT_MUTED);
        exitButton.setContentAreaFilled(false);
        exitButton.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
        exitButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel topBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        topBar.setBackground(UiTheme.SURFACE);
        topBar.setBorder(BorderFactory.createEmptyBorder(14, 0, 0, 12));
        topBar.add(exitButton);

        JPanel area = new JPanel(new BorderLayout());
        area.setBackground(UiTheme.SURFACE);
        area.add(topBar, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.LINE_START;

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 6, 0);
        JLabel title = new JLabel("账号登录");
        title.setFont(UiTheme.titleFont(20, true));
        title.setForeground(UiTheme.TEXT_PRIMARY);
        form.add(title, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 24, 0);
        JLabel subtitle = new JLabel("请使用院内分配的账号进入系统");
        subtitle.setFont(UiTheme.font(Font.PLAIN, 12));
        subtitle.setForeground(UiTheme.TEXT_MUTED);
        form.add(subtitle, gbc);

        usernameField.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        passwordField.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        usernameField.setFont(UiTheme.font(Font.PLAIN, 14));
        passwordField.setFont(UiTheme.font(Font.PLAIN, 14));

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 6, 0);
        form.add(fieldLabel("用户名"), gbc);

        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 14, 0);
        form.add(usernameField, gbc);

        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 6, 0);
        form.add(fieldLabel("密　码"), gbc);

        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 22, 0);
        form.add(passwordField, gbc);

        // 登录按钮同时是默认按钮，FlatLaf 会用 UiTheme 里的主色绘制
        loginButton.setFont(UiTheme.titleFont(15, true));
        loginButton.setBackground(UiTheme.PRIMARY);
        loginButton.setForeground(Color.WHITE);
        loginButton.setPreferredSize(new Dimension(FIELD_WIDTH, BUTTON_HEIGHT));
        loginButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 14, 0);
        form.add(loginButton, gbc);

        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 0, 0);
        JLabel hint = new JLabel("初始账号：admin / 123456");
        hint.setFont(UiTheme.font(Font.PLAIN, 12));
        hint.setForeground(UiTheme.TEXT_MUTED);
        form.add(hint, gbc);

        JPanel center = new JPanel(new GridBagLayout());
        center.setBackground(UiTheme.SURFACE);
        center.add(form);
        area.add(center, BorderLayout.CENTER);
        return area;
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(UiTheme.font(Font.PLAIN, 13));
        label.setForeground(UiTheme.TEXT_MUTED);
        label.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0));
        return label;
    }

    /**
     * 校验本地输入后异步调用业务层登录，避免界面卡死。
     */
    private void doLogin() {
        if (loggingIn) {
            return;
        }

        String username = UiUtil.requireText(usernameField, "用户名");
        String password = new String(passwordField.getPassword());
        if (username == null || password.isEmpty()) {
            UiUtil.warn(this, "请输入用户名和密码");
            if (username == null) {
                usernameField.requestFocusInWindow();
            } else {
                passwordField.requestFocusInWindow();
            }
            return;
        }

        setBusy(true);
        new SwingWorker<SysUser, Void>() {
            @Override
            protected SysUser doInBackground() {
                return userService.login(username, password);
            }

            @Override
            protected void done() {
                setBusy(false);
                try {
                    SysUser user = get();
                    Session.setCurrentUser(user);
                    new MainFrame(user).setVisible(true);
                    dispose();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    UiUtil.error(LoginFrame.this, "登录被中断，请重试");
                } catch (ExecutionException e) {
                    UiUtil.error(LoginFrame.this, resolveMessage(e.getCause()));
                    passwordField.setText("");
                    passwordField.requestFocusInWindow();
                }
            }
        }.execute();
    }

    /**
     * 沿着 cause 链找第一条非空提示，避免 ExceptionInInitializerError 这类无 message 的异常把原因吞掉。
     */
    private static String resolveMessage(Throwable error) {
        Throwable current = error;
        while (current != null) {
            String message = current.getMessage();
            if (message != null && !message.trim().isEmpty()) {
                return message;
            }
            current = current.getCause();
        }
        return "登录失败，请稍后重试";
    }

    /**
     * 切换登录中状态，同时锁住两个按钮。
     */
    private void setBusy(boolean busy) {
        loggingIn = busy;
        loginButton.setText(busy ? "登录中…" : "登录");
        loginButton.setEnabled(!busy);
        exitButton.setEnabled(!busy);
        usernameField.setEnabled(!busy);
        passwordField.setEnabled(!busy);
    }

    /**
     * 左侧品牌底：主色渐变 + 两枚淡白装饰圆。
     */
    private static final class BrandPanel extends JPanel {

        private BrandPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            g2.setPaint(new GradientPaint(0, 0, UiTheme.PRIMARY_DARK, w, h, UiTheme.PRIMARY));
            g2.fillRect(0, 0, w, h);
            g2.setColor(new Color(255, 255, 255, 26));
            g2.fillOval(w - 130, -80, 240, 240);
            g2.setColor(new Color(255, 255, 255, 16));
            g2.fillOval(-100, h - 130, 250, 250);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * 品牌标识：白色圆角方块 + 主色医疗十字，纯绘制，不引入图片资源。
     */
    private static final class LogoMark extends JComponent {

        private static final int SIZE = 46;

        private LogoMark() {
            setOpaque(false);
            setPreferredSize(new Dimension(SIZE, SIZE));
            setMinimumSize(new Dimension(SIZE, SIZE));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(0, 0, SIZE, SIZE, 14, 14);
            g2.setColor(UiTheme.PRIMARY);
            g2.fillRoundRect(SIZE / 2 - 3, 11, 6, SIZE - 22, 4, 4);
            g2.fillRoundRect(11, SIZE / 2 - 3, SIZE - 22, 6, 4, 4);
            g2.dispose();
        }
    }
}
