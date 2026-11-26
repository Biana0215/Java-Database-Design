package hospital.ui.module;

import hospital.entity.Dept;
import hospital.entity.Doctor;
import hospital.entity.Patient;
import hospital.entity.Registration;
import hospital.entity.Schedule;
import hospital.entity.Slot;
import hospital.entity.SysUser;
import hospital.service.DeptService;
import hospital.service.DoctorService;
import hospital.service.PatientService;
import hospital.service.RegistrationService;
import hospital.service.ScheduleService;
import hospital.service.SlotService;
import hospital.ui.Session;
import hospital.ui.common.DateSpinner;
import hospital.ui.common.TableUtil;
import hospital.ui.common.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 挂号面板：上半部查患者、选科室医生日期时段号源后确认挂号，下半部查询挂号单。
 */
public class RegisterPanel extends JPanel {

    private static final String[] PATIENT_COLUMNS = {"编号", "姓名", "性别", "出生日期", "证件号", "电话"};
    private static final String[] REG_COLUMNS = {"挂号单号", "患者", "医生", "就诊日期", "时段",
            "挂号费", "状态", "挂号时间"};
    private static final String[] PERIODS = {"上午", "下午"};
    private static final String[] REG_STATUS = {"全部", "已挂号", "已就诊", "已退号"};
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String ALL = "全部";
    private static final String STATUS_STOPPED = "停诊";

    private static final int COMBO_WIDTH = 150;
    private static final int COMBO_HEIGHT = 28;

    private final PatientService patientService = new PatientService();
    private final DeptService deptService = new DeptService();
    private final DoctorService doctorService = new DoctorService();
    private final ScheduleService scheduleService = new ScheduleService();
    private final SlotService slotService = new SlotService();
    private final RegistrationService registrationService = new RegistrationService();

    private final JTextField patientKeywordField = new JTextField(12);
    private final JTable patientTable = TableUtil.createReadOnlyTable(PATIENT_COLUMNS);
    private final JLabel patientLabel = new JLabel("当前患者：未选择");

    private final JComboBox<DeptItem> deptCombo = new JComboBox<>();
    private final JComboBox<DoctorItem> doctorCombo = new JComboBox<>();
    private final DateSpinner dateSpinner = new DateSpinner(LocalDate.now());
    private final JComboBox<String> periodCombo = new JComboBox<>(PERIODS);
    private final JComboBox<SlotItem> slotCombo = new JComboBox<>();
    private final JLabel remainLabel = new JLabel("剩余 —");

    private final JTextField feeField = new JTextField(8);
    private final JButton registerButton = new JButton("确认挂号");

    private final DateSpinner queryDateSpinner = new DateSpinner(LocalDate.now());
    private final JComboBox<DoctorItem> queryDoctorCombo = new JComboBox<>();
    private final JComboBox<String> queryStatusCombo = new JComboBox<>(REG_STATUS);
    private final JTable regTable = TableUtil.createReadOnlyTable(REG_COLUMNS);

    private final List<Patient> patients = new ArrayList<>();
    private final List<Registration> regRows = new ArrayList<>();

    private Patient currentPatient;
    private BigDecimal currentFee;

    private boolean fillingDept;
    private boolean fillingDoctor;

    public RegisterPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, buildTopArea(), buildBottomArea());
        splitPane.setResizeWeight(0.6);
        splitPane.setDividerLocation(0.6);
        splitPane.setDividerSize(6);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        add(splitPane, BorderLayout.CENTER);

        installCenterRenderer();
        bindEvents();
        loadDepts();
        loadDoctors();
        loadQueryDoctors();
        searchPatients();
        reloadSlots();
        reloadRegs();
    }

    /** 上半部：患者查询行 + 选号行 + 挂号费行 + 患者结果表。 */
    private JComponent buildTopArea() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));

        JPanel conditions = new JPanel();
        conditions.setLayout(new BoxLayout(conditions, BoxLayout.Y_AXIS));
        conditions.add(buildPatientRow());
        conditions.add(buildSlotRow());
        conditions.add(buildFeeRow());
        panel.add(conditions, BorderLayout.NORTH);

        JScrollPane patientScroll = new JScrollPane(patientTable);
        patientScroll.setBorder(BorderFactory.createTitledBorder("患者查询结果（选中一行即确定当前患者）"));
        patientScroll.setPreferredSize(new Dimension(0, 120));
        panel.add(patientScroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildPatientRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        row.add(new JLabel("患者："));
        row.add(patientKeywordField);
        JButton searchButton = new JButton("查询");
        searchButton.addActionListener(e -> searchPatients());
        patientKeywordField.addActionListener(e -> searchPatients());
        row.add(searchButton);
        row.add(patientLabel);
        return row;
    }

    private JPanel buildSlotRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        deptCombo.setPreferredSize(new Dimension(COMBO_WIDTH, COMBO_HEIGHT));
        doctorCombo.setPreferredSize(new Dimension(COMBO_WIDTH, COMBO_HEIGHT));
        periodCombo.setPreferredSize(new Dimension(80, COMBO_HEIGHT));
        slotCombo.setPreferredSize(new Dimension(110, COMBO_HEIGHT));
        row.add(new JLabel("科室："));
        row.add(deptCombo);
        row.add(new JLabel("医生："));
        row.add(doctorCombo);
        row.add(new JLabel("日期："));
        row.add(dateSpinner);
        row.add(new JLabel("时段："));
        row.add(periodCombo);
        row.add(new JLabel("号源："));
        row.add(slotCombo);
        row.add(remainLabel);
        return row;
    }

    private JPanel buildFeeRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        feeField.setEditable(false);
        feeField.setFocusable(false);
        feeField.setPreferredSize(new Dimension(90, COMBO_HEIGHT));
        row.add(new JLabel("挂号费（元）："));
        row.add(feeField);
        row.add(registerButton);
        return row;
    }

    /** 下半部：挂号单查询条件 + 结果表。 */
    private JComponent buildBottomArea() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        queryDoctorCombo.setPreferredSize(new Dimension(COMBO_WIDTH, COMBO_HEIGHT));
        queryStatusCombo.setPreferredSize(new Dimension(100, COMBO_HEIGHT));
        bar.add(new JLabel("日期："));
        bar.add(queryDateSpinner);
        bar.add(new JLabel("医生："));
        bar.add(queryDoctorCombo);
        bar.add(new JLabel("状态："));
        bar.add(queryStatusCombo);
        JButton queryButton = new JButton("查询");
        queryButton.addActionListener(e -> reloadRegs());
        bar.add(queryButton);
        panel.add(bar, BorderLayout.NORTH);

        JScrollPane scrollPane = new JScrollPane(regTable);
        scrollPane.setBorder(BorderFactory.createTitledBorder("挂号单列表"));
        panel.add(scrollPane, BorderLayout.CENTER);
        return panel;
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
        for (int i = 0; i < regTable.getColumnCount(); i++) {
            regTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        for (int i = 0; i < patientTable.getColumnCount(); i++) {
            patientTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    private void bindEvents() {
        patientTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                applySelectedPatient();
            }
        });

        deptCombo.addActionListener(e -> {
            if (fillingDept) {
                return;
            }
            loadDoctors();
            reloadSlots();
        });
        doctorCombo.addActionListener(e -> {
            if (fillingDoctor) {
                return;
            }
            applySelectedDoctor();
            reloadSlots();
        });
        dateSpinner.addChangeListener(e -> reloadSlots());
        periodCombo.addActionListener(e -> reloadSlots());
        registerButton.addActionListener(e -> doRegister());
    }

    private void loadDepts() {
        fillingDept = true;
        try {
            deptCombo.removeAllItems();
            for (Dept dept : deptService.listEnabled()) {
                deptCombo.addItem(new DeptItem(dept.getDeptId(), dept.getDeptName()));
            }
            if (deptCombo.getItemCount() > 0) {
                deptCombo.setSelectedIndex(0);
            }
        } catch (Exception e) {
            UiUtil.showException(this, e);
        } finally {
            fillingDept = false;
        }
    }

    private void loadDoctors() {
        int deptId = selectedDeptId();
        fillingDoctor = true;
        try {
            doctorCombo.removeAllItems();
            if (deptId > 0) {
                for (Doctor doctor : doctorService.listByDept(deptId)) {
                    doctorCombo.addItem(new DoctorItem(doctor.getDoctorId(),
                            value(doctor.getDoctorName()) + "（" + value(doctor.getTitle()) + "）",
                            doctor.getRegFee()));
                }
            }
            if (doctorCombo.getItemCount() > 0) {
                doctorCombo.setSelectedIndex(0);
            }
        } catch (Exception e) {
            UiUtil.showException(this, e);
        } finally {
            fillingDoctor = false;
        }
        applySelectedDoctor();
    }

    /** 查询区医生下拉，含“全部”，取全部医生（离职的也可回查历史单据）。 */
    private void loadQueryDoctors() {
        try {
            queryDoctorCombo.removeAllItems();
            queryDoctorCombo.addItem(new DoctorItem(-1, ALL, null));
            for (Doctor doctor : doctorService.listAll()) {
                String label = value(doctor.getDoctorName()) + "（" + value(doctor.getTitle()) + "）";
                queryDoctorCombo.addItem(new DoctorItem(doctor.getDoctorId(), label, doctor.getRegFee()));
            }
            queryDoctorCombo.setSelectedIndex(0);
        } catch (Exception e) {
            UiUtil.showException(this, e);
        }
    }

    private void searchPatients() {
        try {
            List<Patient> list = patientService.search(patientKeywordField.getText());
            patients.clear();
            patients.addAll(list);
            List<Object[]> data = new ArrayList<>();
            for (Patient patient : list) {
                data.add(new Object[]{
                        patient.getPatientId(),
                        value(patient.getPatientName()),
                        value(patient.getGender()),
                        format(patient.getBirthDate()),
                        value(patient.getIdCard()),
                        value(patient.getPhone())
                });
            }
            TableUtil.fillRows(patientTable, data);
            if (list.isEmpty()) {
                currentPatient = null;
                patientLabel.setText("当前患者：未选择（未找到匹配患者）");
            } else {
                patientTable.setRowSelectionInterval(0, 0);
            }
        } catch (Exception e) {
            UiUtil.showException(this, e);
        }
    }

    private void applySelectedPatient() {
        int index = TableUtil.getSelectedRowModelIndex(patientTable);
        if (index < 0 || index >= patients.size()) {
            currentPatient = null;
            patientLabel.setText("当前患者：未选择");
            return;
        }
        currentPatient = patients.get(index);
        patientLabel.setText("当前患者：" + value(currentPatient.getPatientName())
                + "（" + value(currentPatient.getIdCard()) + "）");
    }

    private void applySelectedDoctor() {
        DoctorItem item = (DoctorItem) doctorCombo.getSelectedItem();
        currentFee = item == null ? null : item.regFee;
        feeField.setText(currentFee == null ? "—" : money(currentFee));
    }

    /** 按科室 + 医生 + 日期 + 时段定位排班，再取该排班的可挂号源。 */
    private void reloadSlots() {
        slotCombo.removeAllItems();
        remainLabel.setText("剩余 —");
        int deptId = selectedDeptId();
        int doctorId = selectedDoctorId();
        if (deptId <= 0 || doctorId <= 0) {
            return;
        }
        LocalDate date = dateSpinner.getLocalDate();
        String period = (String) periodCombo.getSelectedItem();
        try {
            Schedule target = null;
            for (Schedule schedule : scheduleService.listByDeptAndDate(deptId, date)) {
                if (schedule.getDoctorId() == doctorId && period.equals(schedule.getPeriod())) {
                    target = schedule;
                    break;
                }
            }
            if (target == null) {
                remainLabel.setText("该医生当日该时段无排班");
                return;
            }
            if (STATUS_STOPPED.equals(target.getStatus())) {
                remainLabel.setText("该排班已停诊");
                return;
            }
            List<Slot> slots = slotService.listAvailable(target.getScheduleId());
            for (Slot slot : slots) {
                slotCombo.addItem(new SlotItem(slot.getSlotId(), "序号 " + slot.getSeqNo()));
            }
            if (slots.isEmpty()) {
                slotCombo.setSelectedIndex(-1);
            }
            int remain = slotService.countAvailable(target.getScheduleId());
            remainLabel.setText("剩余 " + remain + " 个");
        } catch (Exception e) {
            UiUtil.showException(this, e);
        }
    }

    private void doRegister() {
        if (currentPatient == null) {
            UiUtil.warn(this, "请先查询并选中患者");
            return;
        }
        SlotItem slot = (SlotItem) slotCombo.getSelectedItem();
        if (slot == null) {
            UiUtil.warn(this, "请选择号源");
            return;
        }
        DoctorItem doctor = (DoctorItem) doctorCombo.getSelectedItem();
        int operatorId = currentUserId();
        if (operatorId <= 0) {
            UiUtil.error(this, "登录状态已失效，请重新登录");
            return;
        }

        String message = "确认为以下患者挂号？\n"
                + "患者：" + value(currentPatient.getPatientName())
                + "（" + value(currentPatient.getIdCard()) + "）\n"
                + "医生：" + (doctor == null ? "" : doctor.label) + "\n"
                + "就诊：" + format(dateSpinner.getLocalDate()) + " "
                + periodCombo.getSelectedItem() + " " + slot.label + "\n"
                + "挂号费：" + (currentFee == null ? "—" : money(currentFee)) + " 元";
        if (!UiUtil.confirm(this, message)) {
            return;
        }

        try {
            int regId = registrationService.register(currentPatient.getPatientId(), slot.slotId, operatorId);
            UiUtil.info(this, "挂号成功，挂号单号 " + regId);
            slotCombo.setSelectedIndex(-1);
            reloadSlots();
            reloadRegs();
        } catch (Exception e) {
            UiUtil.error(this, e.getMessage() == null || e.getMessage().trim().isEmpty()
                    ? "挂号失败，请稍后重试" : e.getMessage());
        }
    }

    private void reloadRegs() {
        DoctorItem doctor = (DoctorItem) queryDoctorCombo.getSelectedItem();
        Integer doctorId = doctor == null || doctor.doctorId <= 0 ? null : doctor.doctorId;
        String status = (String) queryStatusCombo.getSelectedItem();
        try {
            List<Registration> list = registrationService.search(null,
                    queryDateSpinner.getLocalDate(), doctorId, ALL.equals(status) ? null : status);
            regRows.clear();
            List<Object[]> data = new ArrayList<>();
            for (Registration registration : list) {
                regRows.add(registration);
                data.add(new Object[]{
                        registration.getRegId(),
                        value(registration.getPatientName()),
                        value(registration.getDoctorName()),
                        format(registration.getWorkDate()),
                        value(registration.getPeriod()),
                        registration.getRegFee() == null ? null
                                : registration.getRegFee().setScale(2, RoundingMode.HALF_UP),
                        value(registration.getStatus()),
                        format(registration.getRegTime())
                });
            }
            TableUtil.fillRows(regTable, data);
        } catch (Exception e) {
            UiUtil.showException(this, e);
        }
    }

    private int selectedDeptId() {
        DeptItem item = (DeptItem) deptCombo.getSelectedItem();
        return item == null ? -1 : item.deptId;
    }

    private int selectedDoctorId() {
        DoctorItem item = (DoctorItem) doctorCombo.getSelectedItem();
        return item == null ? -1 : item.doctorId;
    }

    private static int currentUserId() {
        SysUser user = Session.getCurrentUser();
        return user == null ? -1 : user.getUserId();
    }

    private static String format(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMAT);
    }

    private static String format(LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FORMAT);
    }

    private static String money(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }

    /** 科室下拉项。 */
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

    /** 医生下拉项，随项携带挂号费，避免每次选择再查库。 */
    private static final class DoctorItem {
        private final int doctorId;
        private final String label;
        private final BigDecimal regFee;

        private DoctorItem(int doctorId, String label, BigDecimal regFee) {
            this.doctorId = doctorId;
            this.label = label == null ? "" : label;
            this.regFee = regFee;
        }

        @Override
        public String toString() {
            return label;
        }
    }

    /** 号源下拉项，显示“序号 N”。 */
    private static final class SlotItem {
        private final int slotId;
        private final String label;

        private SlotItem(int slotId, String label) {
            this.slotId = slotId;
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}
