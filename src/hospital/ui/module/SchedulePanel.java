package hospital.ui.module;

import hospital.entity.Dept;
import hospital.entity.Schedule;
import hospital.service.DeptService;
import hospital.service.ScheduleService;
import hospital.service.SlotService;
import hospital.ui.common.DateSpinner;
import hospital.ui.common.TableUtil;
import hospital.ui.common.UiUtil;
import hospital.ui.module.dialog.ScheduleCreateDialog;
import hospital.ui.module.dialog.SlotDetailDialog;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 排班管理：按科室与日期查看排班及实时剩余号源，支持生成排班、停诊与查看号源明细。
 */
public class SchedulePanel extends JPanel {

    private static final String[] COLUMNS = {"排班号", "医生", "日期", "时段", "号源总数", "剩余", "状态"};
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String STATUS_STOPPED = "停诊";
    private static final int COMBO_WIDTH = 160;
    private static final int COMBO_HEIGHT = 28;

    private final DeptService deptService = new DeptService();
    private final ScheduleService scheduleService = new ScheduleService();
    private final SlotService slotService = new SlotService();

    private final JComboBox<DeptItem> deptCombo = new JComboBox<>();
    private final DateSpinner dateSpinner = new DateSpinner(LocalDate.now());
    private final JTable table = TableUtil.createReadOnlyTable(COLUMNS);
    private final JButton createButton = new JButton("生成排班");
    private final JButton stopButton = new JButton("停诊");
    private final JButton slotButton = new JButton("查看号源");
    private final JButton refreshButton = new JButton("刷新");

    /** 与表格模型行一一对应的数据，用于按选中行取排班对象。 */
    private final List<Schedule> rows = new ArrayList<>();

    /** 填充科室下拉时不触发查询。 */
    private boolean fillingDept;

    public SchedulePanel() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(buildQueryBar(), BorderLayout.NORTH);
        add(buildTableArea(), BorderLayout.CENTER);
        add(buildButtonBar(), BorderLayout.SOUTH);

        installRowRenderer();
        bindEvents();
        loadDepts();
        reload();
    }

    /** 条件条：科室 + 日期 + 查询。 */
    private JPanel buildQueryBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        deptCombo.setPreferredSize(new Dimension(COMBO_WIDTH, COMBO_HEIGHT));
        bar.add(new JLabel("科室："));
        bar.add(deptCombo);
        bar.add(new JLabel("日期："));
        bar.add(dateSpinner);
        JButton queryButton = new JButton("查询");
        queryButton.addActionListener(e -> reload());
        bar.add(queryButton);
        return bar;
    }

    private JScrollPane buildTableArea() {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("排班列表（剩余号源实时统计）"));
        return scrollPane;
    }

    /** 按钮条：生成排班、停诊、查看号源、刷新。 */
    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        bar.add(createButton);
        bar.add(stopButton);
        bar.add(slotButton);
        bar.add(refreshButton);
        return bar;
    }

    private void bindEvents() {
        deptCombo.addActionListener(e -> {
            if (!fillingDept) {
                reload();
            }
        });
        dateSpinner.addChangeListener(e -> reload());

        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                updateButtons();
            }
        });

        createButton.addActionListener(e -> openCreateDialog());
        stopButton.addActionListener(e -> stopSchedule());
        slotButton.addActionListener(e -> openSlotDialog());
        refreshButton.addActionListener(e -> reload());
    }

    /** 停诊状态整行置灰。 */
    private void installRowRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                Schedule schedule = scheduleAt(t.convertRowIndexToModel(row));
                if (!isSelected) {
                    boolean stopped = schedule != null && STATUS_STOPPED.equals(schedule.getStatus());
                    c.setForeground(stopped ? Color.GRAY : Color.BLACK);
                }
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    /** 载入启用科室，默认选中第一个。 */
    private void loadDepts() {
        fillingDept = true;
        try {
            deptCombo.removeAllItems();
            List<Dept> depts = deptService.listEnabled();
            for (Dept dept : depts) {
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

    private void reload() {
        int deptId = selectedDeptId();
        if (deptId <= 0) {
            rows.clear();
            TableUtil.clearRows(table);
            updateButtons();
            return;
        }
        LocalDate date = dateSpinner.getLocalDate();
        try {
            List<Schedule> list = scheduleService.listByDeptAndDate(deptId, date);
            List<Object[]> data = new ArrayList<>();
            rows.clear();
            for (Schedule schedule : list) {
                int remain = slotService.countAvailable(schedule.getScheduleId());
                rows.add(schedule);
                data.add(new Object[]{
                        schedule.getScheduleId(),
                        text(schedule.getDoctorName()),
                        format(schedule.getWorkDate()),
                        text(schedule.getPeriod()),
                        schedule.getSlotTotal(),
                        remain,
                        text(schedule.getStatus())
                });
            }
            TableUtil.fillRows(table, data);
        } catch (Exception e) {
            UiUtil.showException(this, e);
        }
        updateButtons();
    }

    private void openCreateDialog() {
        int deptId = selectedDeptId();
        if (deptId <= 0) {
            UiUtil.warn(this, "请先选择科室");
            return;
        }
        ScheduleCreateDialog dialog = new ScheduleCreateDialog(this, deptId);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            reload();
            UiUtil.info(this, "排班生成成功，号源已同步创建");
        }
    }

    private void stopSchedule() {
        Schedule schedule = selectedSchedule();
        if (schedule == null) {
            return;
        }
        if (STATUS_STOPPED.equals(schedule.getStatus())) {
            UiUtil.warn(this, "该排班已是停诊状态");
            return;
        }
        String message = "确认停诊该排班？\n"
                + "医生：" + text(schedule.getDoctorName()) + "\n"
                + "日期：" + format(schedule.getWorkDate()) + " " + text(schedule.getPeriod()) + "\n"
                + "停诊后未挂号的号源将置为停用，已挂号的不受影响。";
        if (!UiUtil.confirm(this, message)) {
            return;
        }
        try {
            int disabled = scheduleService.stopSchedule(schedule.getScheduleId());
            UiUtil.info(this, "已停诊，停用号源 " + disabled + " 个");
            reload();
        } catch (Exception e) {
            UiUtil.showException(this, e);
        }
    }

    private void openSlotDialog() {
        Schedule schedule = selectedSchedule();
        if (schedule == null) {
            return;
        }
        new SlotDetailDialog(this, schedule).setVisible(true);
    }

    /** 依赖选中行的按钮：未选中禁用，已停诊的排班不能再次停诊。 */
    private void updateButtons() {
        Schedule schedule = selectedSchedule();
        slotButton.setEnabled(schedule != null);
        stopButton.setEnabled(schedule != null && !STATUS_STOPPED.equals(schedule.getStatus()));
    }

    private int selectedDeptId() {
        DeptItem item = (DeptItem) deptCombo.getSelectedItem();
        return item == null ? -1 : item.deptId;
    }

    private Schedule selectedSchedule() {
        return scheduleAt(TableUtil.getSelectedRowModelIndex(table));
    }

    private Schedule scheduleAt(int modelRow) {
        if (modelRow < 0 || modelRow >= rows.size()) {
            return null;
        }
        return rows.get(modelRow);
    }

    private static String format(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMAT);
    }

    private static String text(String value) {
        return value == null ? "" : value;
    }

    /** 科室下拉项，带科室编号。 */
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
