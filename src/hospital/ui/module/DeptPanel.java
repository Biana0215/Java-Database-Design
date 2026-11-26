package hospital.ui.module;

import hospital.entity.Dept;
import hospital.service.DeptService;
import hospital.ui.common.TableUtil;
import hospital.ui.common.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * 科室管理：维护科室名称、电话、位置与启用状态，停用只影响后续排班选择。
 */
public class DeptPanel extends JPanel {

    private static final String[] COLUMNS = {"编号", "科室名", "电话", "位置", "状态"};
    private static final String STATUS_ENABLED = "启用";
    private static final String STATUS_DISABLED = "停用";

    private final DeptService deptService = new DeptService();

    private final JTextField keywordField = new JTextField(12);
    private final JTable table = TableUtil.createReadOnlyTable(COLUMNS);
    private final JButton addButton = new JButton("新增");
    private final JButton editButton = new JButton("修改");
    private final JButton toggleButton = new JButton("停用");
    private final JButton refreshButton = new JButton("刷新");

    private final List<Dept> rows = new ArrayList<>();

    public DeptPanel() {
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
        bar.add(new JLabel("科室名："));
        bar.add(keywordField);
        JButton queryButton = new JButton("查询");
        queryButton.addActionListener(e -> reload());
        bar.add(queryButton);
        return bar;
    }

    private JScrollPane buildTableArea() {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("科室列表"));
        return scrollPane;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        bar.add(addButton);
        bar.add(editButton);
        bar.add(toggleButton);
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
        addButton.addActionListener(e -> openEditor(null));
        editButton.addActionListener(e -> openEditor(selectedDept()));
        toggleButton.addActionListener(e -> toggleStatus());
        refreshButton.addActionListener(e -> reload());
    }

    private void reload() {
        String keyword = keywordField.getText() == null ? "" : keywordField.getText().trim();
        try {
            rows.clear();
            List<Object[]> data = new ArrayList<>();
            for (Dept dept : deptService.listAll()) {
                if (!keyword.isEmpty() && !value(dept.getDeptName()).contains(keyword)) {
                    continue;
                }
                rows.add(dept);
                data.add(new Object[]{
                        dept.getDeptId(),
                        value(dept.getDeptName()),
                        value(dept.getDeptPhone()),
                        value(dept.getLocation()),
                        value(dept.getStatus())
                });
            }
            TableUtil.fillRows(table, data);
        } catch (Exception e) {
            UiUtil.showException(this, e);
        }
        updateButtons();
    }

    /** 新增/修改共用一个小表单对话框，dept 为 null 表示新增。 */
    private void openEditor(Dept dept) {
        JTextField nameField = new JTextField(dept == null ? "" : value(dept.getDeptName()), 16);
        JTextField phoneField = new JTextField(dept == null ? "" : value(dept.getDeptPhone()), 16);
        JTextField locationField = new JTextField(dept == null ? "" : value(dept.getLocation()), 16);

        JPanel form = new JPanel(new GridLayout(3, 2, 8, 8));
        form.add(new JLabel("科室名："));
        form.add(nameField);
        form.add(new JLabel("电话："));
        form.add(phoneField);
        form.add(new JLabel("位置："));
        form.add(locationField);

        int result = JOptionPane.showConfirmDialog(this, form,
                dept == null ? "新增科室" : "修改科室",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) {
            UiUtil.warn(this, "科室名称不能为空");
            return;
        }
        String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
        String location = locationField.getText() == null ? "" : locationField.getText().trim();

        Dept target = dept == null ? new Dept() : dept;
        target.setDeptName(name);
        target.setDeptPhone(phone.isEmpty() ? null : phone);
        target.setLocation(location.isEmpty() ? null : location);

        try {
            if (dept == null) {
                deptService.create(target);
                UiUtil.info(this, "科室新增成功");
            } else {
                deptService.update(target);
                UiUtil.info(this, "科室信息已保存");
            }
            reload();
        } catch (Exception e) {
            UiUtil.error(this, e.getMessage() == null || e.getMessage().trim().isEmpty()
                    ? "保存科室失败，请稍后重试" : e.getMessage());
        }
    }

    private void toggleStatus() {
        Dept dept = selectedDept();
        if (dept == null) {
            return;
        }
        boolean enabled = STATUS_ENABLED.equals(dept.getStatus());
        String targetStatus = enabled ? STATUS_DISABLED : STATUS_ENABLED;
        String message = enabled
                ? "确认停用科室“" + value(dept.getDeptName()) + "”？\n停用后该科室不再出现在新排班里，已有数据不受影响。"
                : "确认启用科室“" + value(dept.getDeptName()) + "”？\n启用后该科室将重新出现在排班科室列表中。";
        if (!UiUtil.confirm(this, message)) {
            return;
        }
        try {
            deptService.changeStatus(dept.getDeptId(), targetStatus);
            UiUtil.info(this, "科室已" + targetStatus);
            reload();
        } catch (Exception e) {
            UiUtil.error(this, e.getMessage() == null || e.getMessage().trim().isEmpty()
                    ? "修改科室状态失败，请稍后重试" : e.getMessage());
        }
    }

    /** 选中行决定按钮文案与可用状态。 */
    private void updateButtons() {
        Dept dept = selectedDept();
        boolean selected = dept != null;
        editButton.setEnabled(selected);
        toggleButton.setEnabled(selected);
        if (selected) {
            toggleButton.setText(STATUS_ENABLED.equals(dept.getStatus()) ? "停用" : "启用");
        } else {
            toggleButton.setText("启用/停用");
        }
    }

    private Dept selectedDept() {
        int index = TableUtil.getSelectedRowModelIndex(table);
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return rows.get(index);
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }
}
