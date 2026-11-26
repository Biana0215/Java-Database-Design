package hospital.ui.module;

import hospital.entity.Doctor;
import hospital.entity.SysUser;
import hospital.service.DoctorService;
import hospital.service.SysUserService;
import hospital.ui.common.TableUtil;
import hospital.ui.common.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * 用户管理：维护登录账号的角色、关联医生与启停状态，并支持重置密码。
 */
public class UserPanel extends JPanel {

    private static final String[] COLUMNS = {"编号", "用户名", "角色", "关联医生", "状态"};
    private static final String[] ROLES = {"挂号员", "医生", "管理员"};
    private static final String ROLE_DOCTOR = "医生";
    private static final String STATUS_ENABLED = "启用";
    private static final String STATUS_DISABLED = "停用";
    private static final String NONE = "—";

    private final SysUserService userService = new SysUserService();
    private final DoctorService doctorService = new DoctorService();

    private final JTextField keywordField = new JTextField(12);
    private final JTable table = TableUtil.createReadOnlyTable(COLUMNS);
    private final JButton addButton = new JButton("新增用户");
    private final JButton toggleButton = new JButton("启用/停用");
    private final JButton resetButton = new JButton("重置密码");
    private final JButton refreshButton = new JButton("刷新");

    private final List<SysUser> rows = new ArrayList<>();

    public UserPanel() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(buildQueryBar(), BorderLayout.NORTH);
        add(buildTableArea(), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        installCenterRenderer();
        bindEvents();
        reload();
    }

    private JPanel buildQueryBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.add(new JLabel("用户名："));
        bar.add(keywordField);
        JButton queryButton = new JButton("查询");
        queryButton.addActionListener(e -> reload());
        bar.add(queryButton);
        return bar;
    }

    private JScrollPane buildTableArea() {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("账号列表（密码不显示）"));
        return scrollPane;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        bar.add(addButton);
        bar.add(toggleButton);
        bar.add(resetButton);
        bar.add(refreshButton);
        return bar;
    }

    private void installCenterRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    private void bindEvents() {
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
            }
        });
        addButton.addActionListener(e -> openCreateDialog());
        toggleButton.addActionListener(e -> toggleStatus());
        resetButton.addActionListener(e -> resetPassword());
        refreshButton.addActionListener(e -> reload());
    }

    private void reload() {
        String keyword = keywordField.getText() == null ? "" : keywordField.getText().trim();
        try {
            rows.clear();
            List<Object[]> data = new ArrayList<>();
            for (SysUser user : userService.listAll()) {
                if (!keyword.isEmpty() && !value(user.getUsername()).contains(keyword)) {
                    continue;
                }
                rows.add(user);
                data.add(new Object[]{
                        user.getUserId(),
                        value(user.getUsername()),
                        value(user.getRole()),
                        value(user.getDoctorName()).isEmpty() ? NONE : user.getDoctorName(),
                        value(user.getStatus())
                });
            }
            TableUtil.fillRows(table, data);
        } catch (Exception e) {
            UiUtil.showException(this, e);
        }
        updateButtons();
    }

    private void openCreateDialog() {
        UserCreateDialog dialog = new UserCreateDialog(this);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            reload();
        }
    }

    private void toggleStatus() {
        SysUser user = selectedUser();
        if (user == null) {
            return;
        }
        boolean enabled = STATUS_ENABLED.equals(user.getStatus());
        String targetStatus = enabled ? STATUS_DISABLED : STATUS_ENABLED;
        String message = enabled
                ? "确认停用账号“" + value(user.getUsername()) + "”？\n停用后该账号无法登录系统。"
                : "确认启用账号“" + value(user.getUsername()) + "”？\n启用后该账号可正常登录。";
        if (!UiUtil.confirm(this, message)) {
            return;
        }
        try {
            userService.changeStatus(user.getUserId(), targetStatus);
            UiUtil.info(this, "账号已" + targetStatus);
            reload();
        } catch (Exception e) {
            UiUtil.error(this, e.getMessage() == null || e.getMessage().trim().isEmpty()
                    ? "修改账号状态失败，请稍后重试" : e.getMessage());
        }
    }

    /** 两次输入一致才提交，不一致时重新弹出输入框。 */
    private void resetPassword() {
        SysUser user = selectedUser();
        if (user == null) {
            return;
        }
        while (true) {
            JPasswordField firstField = new JPasswordField(14);
            JPasswordField secondField = new JPasswordField(14);
            JPanel form = new JPanel(new GridLayout(2, 2, 8, 8));
            form.add(new JLabel("新密码："));
            form.add(firstField);
            form.add(new JLabel("确认新密码："));
            form.add(secondField);

            int result = JOptionPane.showConfirmDialog(this, form,
                    "重置密码 - " + value(user.getUsername()),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (result != JOptionPane.OK_OPTION) {
                return;
            }
            String first = new String(firstField.getPassword());
            String second = new String(secondField.getPassword());
            if (first.isEmpty()) {
                UiUtil.warn(this, "新密码不能为空");
                continue;
            }
            if (!first.equals(second)) {
                UiUtil.warn(this, "两次输入的密码不一致，请重新输入");
                continue;
            }
            try {
                userService.resetPassword(user.getUserId(), first);
                UiUtil.info(this, "密码已重置");
                return;
            } catch (Exception e) {
                UiUtil.error(this, e.getMessage() == null || e.getMessage().trim().isEmpty()
                        ? "重置密码失败，请稍后重试" : e.getMessage());
                return;
            }
        }
    }

    private void updateButtons() {
        SysUser user = selectedUser();
        boolean selected = user != null;
        toggleButton.setEnabled(selected);
        resetButton.setEnabled(selected);
        if (selected) {
            toggleButton.setText(STATUS_ENABLED.equals(user.getStatus()) ? "停用" : "启用");
        } else {
            toggleButton.setText("启用/停用");
        }
    }

    private SysUser selectedUser() {
        int index = TableUtil.getSelectedRowModelIndex(table);
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return rows.get(index);
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }

    /** 医生下拉项，带医生编号。 */
    private static final class DoctorItem {
        private final int doctorId;
        private final String label;

        private DoctorItem(int doctorId, String label) {
            this.doctorId = doctorId;
            this.label = label == null ? "" : label;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** 新增用户对话框：角色选“医生”时必须绑定一个医生账号。 */
    private final class UserCreateDialog extends JDialog {

        private final JTextField usernameField = new JTextField(14);
        private final JPasswordField passwordField = new JPasswordField(14);
        private final JComboBox<String> roleCombo = new JComboBox<>(ROLES);
        private final JComboBox<DoctorItem> doctorCombo = new JComboBox<>();
        private boolean confirmed;

        private UserCreateDialog(Component parent) {
            super(SwingUtilities.getWindowAncestor(parent), "新增用户", ModalityType.APPLICATION_MODAL);
            initUi(parent);
            loadDoctors();
        }

        private boolean isConfirmed() {
            return confirmed;
        }

        private void initUi(Component parent) {
            setLayout(new BorderLayout());
            setResizable(false);

            JPanel form = new JPanel(new GridLayout(4, 2, 10, 10));
            form.setBorder(BorderFactory.createEmptyBorder(16, 16, 8, 16));
            form.add(new JLabel("用户名："));
            form.add(usernameField);
            form.add(new JLabel("初始密码："));
            form.add(passwordField);
            form.add(new JLabel("角色："));
            form.add(roleCombo);
            form.add(new JLabel("关联医生："));
            form.add(doctorCombo);

            JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
            JButton okButton = new JButton("确定");
            JButton cancelButton = new JButton("取消");
            okButton.addActionListener(e -> onCreate());
            cancelButton.addActionListener(e -> dispose());
            buttonBar.add(okButton);
            buttonBar.add(cancelButton);

            roleCombo.addActionListener(e -> updateDoctorEnabled());
            updateDoctorEnabled();

            add(form, BorderLayout.CENTER);
            add(buttonBar, BorderLayout.SOUTH);
            getRootPane().setDefaultButton(okButton);
            setSize(400, 260);
            setLocationRelativeTo(parent);
        }

        /** 只有“医生”角色才允许且必须选择关联医生。 */
        private void updateDoctorEnabled() {
            boolean isDoctor = ROLE_DOCTOR.equals(roleCombo.getSelectedItem());
            doctorCombo.setEnabled(isDoctor);
        }

        private void loadDoctors() {
            try {
                doctorCombo.removeAllItems();
                List<Doctor> doctors = doctorService.listAll();
                List<Doctor> active = new ArrayList<>();
                for (Doctor doctor : doctors) {
                    if ("在职".equals(doctor.getStatus())) {
                        active.add(doctor);
                        doctorCombo.addItem(new DoctorItem(doctor.getDoctorId(),
                                value(doctor.getDoctorName()) + "（" + value(doctor.getDeptName()) + "·"
                                        + value(doctor.getTitle()) + "）"));
                    }
                }
                if (active.isEmpty()) {
                    doctorCombo.setSelectedIndex(-1);
                }
            } catch (Exception e) {
                UiUtil.showException(this, e);
            }
        }

        private void onCreate() {
            String username = usernameField.getText() == null ? "" : usernameField.getText().trim();
            if (username.isEmpty()) {
                UiUtil.warn(this, "用户名不能为空");
                return;
            }
            String password = new String(passwordField.getPassword());
            if (password.isEmpty()) {
                UiUtil.warn(this, "初始密码不能为空");
                return;
            }
            String role = (String) roleCombo.getSelectedItem();
            DoctorItem doctor = (DoctorItem) doctorCombo.getSelectedItem();
            if (ROLE_DOCTOR.equals(role) && doctor == null) {
                UiUtil.warn(this, "角色为“医生”时必须选择一个关联医生");
                return;
            }

            SysUser user = new SysUser();
            user.setUsername(username);
            user.setRole(role);
            user.setStatus(STATUS_ENABLED);
            user.setDoctorId(ROLE_DOCTOR.equals(role) ? doctor.doctorId : null);

            try {
                userService.create(user, password);
                UiUtil.info(this, "用户新增成功");
                confirmed = true;
                dispose();
            } catch (Exception e) {
                // 用户名重复等提示由后端给出，原样展示，对话框不关闭
                UiUtil.error(this, e.getMessage() == null || e.getMessage().trim().isEmpty()
                        ? "新增用户失败，请稍后重试" : e.getMessage());
            }
        }
    }
}
