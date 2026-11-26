package hospital.ui.module;

import hospital.entity.Dept;
import hospital.entity.Doctor;
import hospital.entity.Schedule;
import hospital.service.DeptService;
import hospital.service.DoctorService;
import hospital.service.ScheduleService;
import hospital.service.SlotService;
import hospital.ui.common.DateSpinner;
import hospital.ui.common.TableUtil;
import hospital.ui.common.UiUtil;
import hospital.ui.module.dialog.SlotDetailDialog;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 排班查询：按科室、医生、状态组合查询某日排班，只读展示并可查看号源明细。
 */
public class ScheduleQueryPanel extends JPanel {

    private static final String[] COLUMNS = {"排班号", "医生", "日期", "时段", "号源总数", "剩余", "状态"};
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String ALL = "全部";
    private static final int COMBO_WIDTH = 150;
    private static final int COMBO_HEIGHT = 28;

    private final DeptService deptService = new DeptService();
    private final DoctorService doctorService = new DoctorService();
    private final ScheduleService scheduleService = new ScheduleService();
    private final SlotService slotService = new SlotService();

    private final JComboBox<DeptItem> deptCombo = new JComboBox<>();
    private final DateSpinner dateSpinner = new DateSpinner(LocalDate.now());
    private final JComboBox<DoctorItem> doctorCombo = new JComboBox<>();
    private final JComboBox<String> statusCombo = new JComboBox<>(new String[]{ALL, "正常", "停诊"});
    private final JTable table = TableUtil.createReadOnlyTable(COLUMNS);
    private final JButton slotButton = new JButton("查看号源");

    private final List<Schedule> rows = new ArrayList<>();

    private boolean fillingDept;
    private boolean fillingDoctor;

    public ScheduleQueryPanel() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(buildQueryBar(), BorderLayout.NORTH);
        add(buildTableArea(), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        installRowRenderer();
        bindEvents();
        loadDepts();
        loadDoctors();
        reload();
    }

    private JPanel buildQueryBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        deptCombo.setPreferredSize(new Dimension(COMBO_WIDTH, COMBO_HEIGHT));
        doctorCombo.setPreferredSize(new Dimension(COMBO_WIDTH, COMBO_HEIGHT));
        statusCombo.setPreferredSize(new Dimension(90, COMBO_HEIGHT));

        bar.add(new JLabel("科室："));
        bar.add(deptCombo);
        bar.add(new JLabel("日期："));
        bar.add(dateSpinner);
        bar.add(new JLabel("医生："));
        bar.add(doctorCombo);
        bar.add(new JLabel("状态："));
        bar.add(statusCombo);

        JButton queryButton = new JButton("查询");
        queryButton.addActionListener(e -> reload());
        bar.add(queryButton);
        return bar;
    }

    private JScrollPane buildTableArea() {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("排班查询结果"));
        return scrollPane;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        bar.add(slotButton);
        return bar;
    }

    private void bindEvents() {
        deptCombo.addActionListener(e -> {
            if (fillingDept) {
                return;
            }
            loadDoctors();
            reload();
        });
        doctorCombo.addActionListener(e -> {
            if (!fillingDoctor) {
                reload();
            }
        });
        statusCombo.addActionListener(e -> reload());
        dateSpinner.addChangeListener(e -> reload());

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                slotButton.setEnabled(selectedSchedule() != null);
            }
        });
        slotButton.addActionListener(e -> {
            Schedule schedule = selectedSchedule();
            if (schedule != null) {
                new SlotDetailDialog(this, schedule).setVisible(true);
            }
        });
    }

    private void installRowRenderer() {
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

    private void loadDepts() {
        fillingDept = true;
        try {
            deptCombo.removeAllItems();
            deptCombo.addItem(new DeptItem(-1, ALL));
            for (Dept dept : deptService.listEnabled()) {
                deptCombo.addItem(new DeptItem(dept.getDeptId(), dept.getDeptName()));
            }
            deptCombo.setSelectedIndex(0);
        } catch (Exception e) {
            UiUtil.showException(this, e);
        } finally {
            fillingDept = false;
        }
    }

    /** 医生下拉随科室联动，“全部”科室时列出所有医生。 */
    private void loadDoctors() {
        int deptId = selectedDeptId();
        fillingDoctor = true;
        try {
            doctorCombo.removeAllItems();
            doctorCombo.addItem(new DoctorItem(-1, ALL));
            List<Doctor> doctors = deptId > 0 ? doctorService.listByDept(deptId) : doctorService.listAll();
            for (Doctor doctor : doctors) {
                String label = value(doctor.getDoctorName()) + "（" + value(doctor.getTitle()) + "）";
                doctorCombo.addItem(new DoctorItem(doctor.getDoctorId(), label));
            }
            doctorCombo.setSelectedIndex(0);
        } catch (Exception e) {
            UiUtil.showException(this, e);
        } finally {
            fillingDoctor = false;
        }
    }

    /**
     * 先用 listByDeptAndDate 取数（“全部”科室时按启用科室逐个取），再按医生与状态在内存过滤。
     */
    private void reload() {
        LocalDate date = dateSpinner.getLocalDate();
        int deptId = selectedDeptId();
        int doctorId = selectedDoctorId();
        String status = (String) statusCombo.getSelectedItem();

        try {
            List<Schedule> list = new ArrayList<>();
            if (deptId > 0) {
                list.addAll(scheduleService.listByDeptAndDate(deptId, date));
            } else {
                for (Dept dept : deptService.listEnabled()) {
                    list.addAll(scheduleService.listByDeptAndDate(dept.getDeptId(), date));
                }
            }

            rows.clear();
            List<Object[]> data = new ArrayList<>();
            for (Schedule schedule : list) {
                if (doctorId > 0 && schedule.getDoctorId() != doctorId) {
                    continue;
                }
                if (status != null && !ALL.equals(status) && !status.equals(schedule.getStatus())) {
                    continue;
                }
                int remain = slotService.countAvailable(schedule.getScheduleId());
                rows.add(schedule);
                data.add(new Object[]{
                        schedule.getScheduleId(),
                        value(schedule.getDoctorName()),
                        format(schedule.getWorkDate()),
                        value(schedule.getPeriod()),
                        schedule.getSlotTotal(),
                        remain,
                        value(schedule.getStatus())
                });
            }
            TableUtil.fillRows(table, data);
        } catch (Exception e) {
            UiUtil.showException(this, e);
        }
        slotButton.setEnabled(selectedSchedule() != null);
    }

    private int selectedDeptId() {
        DeptItem item = (DeptItem) deptCombo.getSelectedItem();
        return item == null ? -1 : item.deptId;
    }

    private int selectedDoctorId() {
        DoctorItem item = (DoctorItem) doctorCombo.getSelectedItem();
        return item == null ? -1 : item.doctorId;
    }

    private Schedule selectedSchedule() {
        int index = TableUtil.getSelectedRowModelIndex(table);
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return rows.get(index);
    }

    private static String format(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMAT);
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
