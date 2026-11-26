package hospital.ui;

import hospital.entity.SysUser;
import hospital.service.SysUserService;
import hospital.ui.common.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.concurrent.ExecutionException;

/**
 * 登录窗口：校验账号密码，成功后进入主窗体。
 */
public class LoginFrame extends JFrame {

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

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.insets = new Insets(8, 0, 8, 8);
        gbc.anchor = GridBagConstraints.LINE_END;

        JLabel userLabel = new JLabel("用户名：");
        userLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        formPanel.add(userLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        formPanel.add(usernameField, gbc);

        JLabel pwdLabel = new JLabel("密　码：");
        pwdLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets = new Insets(8, 0, 8, 8);
        formPanel.add(pwdLabel, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        formPanel.add(passwordField, gbc);

        loginButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        exitButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        loginButton.setPreferredSize(new Dimension(96, 32));
        exitButton.setPreferredSize(new Dimension(96, 32));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 24, 8));
        buttonPanel.add(loginButton);
        buttonPanel.add(exitButton);

        JPanel root = new JPanel(new BorderLayout(0, 12));
        root.setBorder(BorderFactory.createEmptyBorder(24, 32, 16, 32));
        root.add(formPanel, BorderLayout.CENTER);
        root.add(buttonPanel, BorderLayout.SOUTH);
        setContentPane(root);

        // 回车即登录：默认按钮 + 两个输入框各自绑定
        getRootPane().setDefaultButton(loginButton);
        loginButton.addActionListener(e -> doLogin());
        exitButton.addActionListener(e -> System.exit(0));
        usernameField.addActionListener(e -> doLogin());
        passwordField.addActionListener(e -> doLogin());

        setSize(420, 260);
        setResizable(false);
        setLocationRelativeTo(null);
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
}
