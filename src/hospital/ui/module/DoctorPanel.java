package hospital.ui.module;

import hospital.entity.Dept;
import hospital.entity.Doctor;
import hospital.service.DeptService;
import hospital.service.DoctorService;
import hospital.ui.common.TableUtil;
import hospital.ui.common.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 医生管理：按科室查看医生，维护姓名、职称、挂号费与在职状态，离职只影响后续排班。
 */
public class DoctorPanel extends JPanel {

    private static final String[] COLUMNS = {"编号", "姓名", "科室", "职称", "挂号费", "电话", "状态"};
    private static final String[] TITLES = {"主任医师", "副主任医师", "主治医师", "住院医师"};
    private static final String ALL = "全部";
    private static final String STATUS_ACTIVE = "在职";
    private static final String STATUS_LEFT = "离职";
    private static final int COMBO_WIDTH = 160;
    private static final int COMBO_HEIGHT = 28;

    private final DeptService deptService = new DeptService();
    private final DoctorService doctorService = new DoctorService();

    private final JComboBox<DeptItem> deptCombo = new JComboBox<>();
    private final JTable table = TableUtil.createReadOnlyTable(COLUMNS);
    private final JButton addButton = new JButton("新增");
    private final JButton editButton = new JButton("修改");
    private final JButton toggleButton = new JButton("在职/离职切换");
    private final JButton refreshButton = new JButton("刷新");

    private final List<Doctor> rows = new ArrayList<>();

    private boolean fillingDept;

    public DoctorPanel() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(buildQueryBar(), BorderLayout.NORTH);
        add(buildTableArea(), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        installCenterRenderer();
        bindEvents();
        loadDepts();
        reload();
    }

    private JPanel buildQueryBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        deptCombo.setPreferredSize(new Dimension(COMBO_WIDTH, COMBO_HEIGHT));
        bar.add(new JLabel("科室："));
        bar.add(deptCombo);
        JButton queryButton = new JButton("查询");
        queryButton.addActionListener(e -> reload());
        bar.add(queryButton);
        return bar;
    }

    private JScrollPane buildTableArea() {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("医生列表"));
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
        deptCombo.addActionListener(e -> {
            if (!fillingDept) {
                reload();
            }
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
            }
        });
        addButton.addActionListener(e -> openEditor(null));
        editButton.addActionListener(e -> openEditor(selectedDoctor()));
        toggleButton.addActionListener(e -> toggleStatus());
        refreshButton.addActionListener(e -> reload());
    }

    private void loadDepts() {
        fillingDept = true;
        try {
            deptCombo.removeAllItems();
            deptCombo.addItem(new DeptItem(-1, ALL));
            for (Dept dept : deptService.listAll()) {
                deptCombo.addItem(new DeptItem(dept.getDeptId(), dept.getDeptName()));
            }
            deptCombo.setSelectedIndex(0);
        } catch (Exception e) {
            UiUtil.showException(this, e);
        } finally {
            fillingDept = false;
        }
    }

    /** 用 listAll 再按科室过滤，保证离职医生也能查到。 */
    private void reload() {
        int deptId = selectedDeptId();
        try {
            rows.clear();
            List<Object[]> data = new ArrayList<>();
            for (Doctor doctor : doctorService.listAll()) {
                if (deptId > 0 && doctor.getDeptId() != deptId) {
                    continue;
                }
                rows.add(doctor);
                data.add(new Object[]{
                        doctor.getDoctorId(),
                        value(doctor.getDoctorName()),
                        value(doctor.getDeptName()),
                        value(doctor.getTitle()),
                        doctor.getRegFee() == null ? null
                                : doctor.getRegFee().setScale(2, RoundingMode.HALF_UP),
                        value(doctor.getPhone()),
                        value(doctor.getStatus())
                });
            }
            TableUtil.fillRows(table, data);
        } catch (Exception e) {
            UiUtil.showException(this, e);
        }
        updateButtons();
    }

    /** 新增/修改共用一个小表单对话框，doctor 为 null 表示新增。 */
    private void openEditor(Doctor doctor) {
        JTextField nameField = new JTextField(doctor == null ? "" : value(doctor.getDoctorName()), 14);
        JComboBox<DeptItem> deptBox = new JComboBox<>();
        JComboBox<String> titleBox = new JComboBox<>(TITLES);
        JTextField feeField = new JTextField(
                doctor == null || doctor.getRegFee() == null ? "" : doctor.getRegFee().toPlainString(), 14);
        JTextField phoneField = new JTextField(doctor == null ? "" : value(doctor.getPhone()), 14);

        int preselectDeptId = doctor == null ? selectedDeptId() : doctor.getDeptId();
        try {
            for (Dept dept : deptService.listAll()) {
                DeptItem item = new DeptItem(dept.getDeptId(), dept.getDeptName());
                deptBox.addItem(item);
                if (item.deptId == preselectDeptId) {
                    deptBox.setSelectedItem(item);
                }
            }
        } catch (Exception e) {
            UiUtil.showException(this, e);
            return;
        }
        if (doctor != null && doctor.getTitle() != null) {
            titleBox.setSelectedItem(doctor.getTitle());
        }

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.add(new JLabel("姓名："));
        form.add(nameField);
        form.add(new JLabel("科室："));
        form.add(deptBox);
        form.add(new JLabel("职称："));
        form.add(titleBox);
        form.add(new JLabel("挂号费（元）："));
        form.add(feeField);
        form.add(new JLabel("电话："));
        form.add(phoneField);

        int result = JOptionPane.showConfirmDialog(this, form,
                doctor == null ? "新增医生" : "修改医生",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }

        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) {
            UiUtil.warn(this, "医生姓名不能为空");
            return;
        }
        DeptItem deptItem = (DeptItem) deptBox.getSelectedItem();
        if (deptItem == null) {
            UiUtil.warn(this, "请选择所属科室");
            return;
        }
        String feeText = feeField.getText() == null ? "" : feeField.getText().trim();
        BigDecimal fee;
        try {
            fee = new BigDecimal(feeText);
        } catch (NumberFormatException e) {
            UiUtil.warn(this, "挂号费必须填写数字");
            return;
        }
        if (fee.compareTo(BigDecimal.ZERO) <= 0) {
            UiUtil.warn(this, "挂号费必须大于 0");
            return;
        }
        String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();

        Doctor target = doctor == null ? new Doctor() : doctor;
        target.setDoctorName(name);
        target.setDeptId(deptItem.deptId);
        target.setTitle((String) titleBox.getSelectedItem());
        target.setRegFee(fee.setScale(2, RoundingMode.HALF_UP));
        target.setPhone(phone.isEmpty() ? null : phone);

        try {
            if (doctor == null) {
                doctorService.create(target);
                UiUtil.info(this, "医生新增成功");
            } else {
                doctorService.update(target);
                UiUtil.info(this, "医生信息已保存");
            }
            reload();
        } catch (Exception e) {
            UiUtil.error(this, e.getMessage() == null || e.getMessage().trim().isEmpty()
                    ? "保存医生失败，请稍后重试" : e.getMessage());
        }
    }

    private void toggleStatus() {
        Doctor doctor = selectedDoctor();
        if (doctor == null) {
            return;
        }
        boolean active = STATUS_ACTIVE.equals(doctor.getStatus());
        String targetStatus = active ? STATUS_LEFT : STATUS_ACTIVE;
        String message = active
                ? "确认将医生“" + value(doctor.getDoctorName()) + "”置为离职？\n离职后该医生不再出现在新排班里。"
                : "确认将医生“" + value(doctor.getDoctorName()) + "”恢复为在职？\n恢复后可按科室重新排班。";
        if (!UiUtil.confirm(this, message)) {
            return;
        }
        try {
            doctorService.changeStatus(doctor.getDoctorId(), targetStatus);
            UiUtil.info(this, "医生状态已改为" + targetStatus);
            reload();
        } catch (Exception e) {
            UiUtil.error(this, e.getMessage() == null || e.getMessage().trim().isEmpty()
                    ? "修改医生状态失败，请稍后重试" : e.getMessage());
        }
    }

    private void updateButtons() {
        Doctor doctor = selectedDoctor();
        boolean selected = doctor != null;
        editButton.setEnabled(selected);
        toggleButton.setEnabled(selected);
        if (selected) {
            toggleButton.setText(STATUS_ACTIVE.equals(doctor.getStatus()) ? "设为离职" : "恢复在职");
        } else {
            toggleButton.setText("在职/离职切换");
        }
    }

    private int selectedDeptId() {
        DeptItem item = (DeptItem) deptCombo.getSelectedItem();
        return item == null ? -1 : item.deptId;
    }

    private Doctor selectedDoctor() {
        int index = TableUtil.getSelectedRowModelIndex(table);
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return rows.get(index);
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }

    private static final class DeptItem {
        private final int deptId;
        private final String deptName;

        private DeptItem(int deptId, String deptName) {
            this.deptId = deptId;
            this.deptName = deptName == null ? "" : deptName;
        }

        @Override
        public String toString() {
            return deptName;
        }
    }
}
