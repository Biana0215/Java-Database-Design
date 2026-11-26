package hospital.ui.module.dialog;

import hospital.entity.Doctor;
import hospital.service.DeptService;
import hospital.service.DoctorService;
import hospital.service.ScheduleService;
import hospital.ui.common.DateSpinner;
import hospital.ui.common.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.List;

/**
 * 生成排班对话框：选医生、日期、时段与号源数，本地校验通过后调用 Service 一次生成排班与号源。
 */
public class ScheduleCreateDialog extends JDialog {

    private static final int DIALOG_WIDTH = 420;
    private static final int DIALOG_HEIGHT = 300;
    private static final int MIN_SLOT = 1;
    private static final int MAX_SLOT = 100;
    private static final int DEFAULT_SLOT = 30;
    private static final int FIELD_WIDTH = 180;
    private static final int FIELD_HEIGHT = 26;

    private final DeptService deptService = new DeptService();
    private final DoctorService doctorService = new DoctorService();
    private final ScheduleService scheduleService = new ScheduleService();

    private final JTextField deptField = new JTextField();
    private final JComboBox<DoctorItem> doctorCombo = new JComboBox<>();
    private final DateSpinner dateSpinner = new DateSpinner(LocalDate.now());
    private final JComboBox<String> periodCombo = new JComboBox<>(new String[]{"上午", "下午"});
    private final JSpinner slotSpinner = new JSpinner(
            new SpinnerNumberModel(DEFAULT_SLOT, MIN_SLOT, MAX_SLOT, 1));

    private final int deptId;
    private boolean confirmed;

    public ScheduleCreateDialog(Component parent, int deptId) {
        super(SwingUtilities.getWindowAncestor(parent), "生成排班", ModalityType.APPLICATION_MODAL);
        this.deptId = deptId;
        initUi(parent);
        loadDeptName();
        loadDoctors();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void initUi(Component parent) {
        setLayout(new BorderLayout());
        setResizable(false);

        deptField.setEditable(false);
        deptField.setFocusable(false);

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(18, 18, 8, 18));
        form.add(new JLabel("科室："));
        form.add(deptField);
        form.add(new JLabel("医生："));
        form.add(doctorCombo);
        form.add(new JLabel("出诊日期："));
        form.add(wrapLeft(dateSpinner));
        form.add(new JLabel("时段："));
        form.add(wrapLeft(periodCombo));
        form.add(new JLabel("号源数："));
        form.add(wrapLeft(slotSpinner));

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton okButton = new JButton("确定");
        JButton cancelButton = new JButton("取消");
        okButton.addActionListener(e -> onConfirm());
        cancelButton.addActionListener(e -> dispose());
        buttonBar.add(okButton);
        buttonBar.add(cancelButton);

        add(form, BorderLayout.CENTER);
        add(buttonBar, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(okButton);

        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setLocationRelativeTo(parent);
    }

    /** 微调框与下拉框宽度固定，避免被 GridLayout 拉满整行。 */
    private JPanel wrapLeft(JComponent component) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        component.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        panel.add(component);
        return panel;
    }

    private void loadDeptName() {
        try {
            deptField.setText(deptService.get(deptId).getDeptName());
        } catch (Exception e) {
            deptField.setText("科室 #" + deptId);
            UiUtil.showException(this, e);
        }
    }

    private void loadDoctors() {
        try {
            doctorCombo.removeAllItems();
            List<Doctor> doctors = doctorService.listByDept(deptId);
            for (Doctor doctor : doctors) {
                String label = value(doctor.getDoctorName()) + "（" + value(doctor.getTitle()) + "）";
                doctorCombo.addItem(new DoctorItem(doctor.getDoctorId(), label));
            }
            if (doctorCombo.getItemCount() > 0) {
                doctorCombo.setSelectedIndex(0);
            }
        } catch (Exception e) {
            UiUtil.showException(this, e);
        }
    }

    /** 本地校验通过后才调 Service，失败时对话框保留并提示原因。 */
    private void onConfirm() {
        DoctorItem doctor = (DoctorItem) doctorCombo.getSelectedItem();
        if (doctor == null) {
            UiUtil.warn(this, "请选择出诊医生");
            return;
        }
        LocalDate date = dateSpinner.getLocalDate();
        if (date.isBefore(LocalDate.now())) {
            UiUtil.warn(this, "出诊日期不能早于今天");
            return;
        }
        int slotTotal = ((Number) slotSpinner.getValue()).intValue();
        if (slotTotal < MIN_SLOT || slotTotal > MAX_SLOT) {
            UiUtil.warn(this, "号源数必须在 " + MIN_SLOT + "~" + MAX_SLOT + " 之间");
            return;
        }

        try {
            scheduleService.createSchedule(doctor.doctorId, date,
                    (String) periodCombo.getSelectedItem(), slotTotal);
            confirmed = true;
            dispose();
        } catch (Exception e) {
            UiUtil.showException(this, e);
        }
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }

    /** 医生下拉项，显示“姓名（职称）”。 */
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
}
