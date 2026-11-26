package hospital.ui.module;

import hospital.entity.Registration;
import hospital.entity.SysUser;
import hospital.service.RegistrationService;
import hospital.ui.Session;
import hospital.ui.common.DateSpinner;
import hospital.ui.common.TableUtil;
import hospital.ui.common.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 就诊确认：当前医生按日期查看自己的挂号单，并把“已挂号”的单据标记为已就诊。
 */
public class VisitPanel extends JPanel {

    private static final String[] COLUMNS = {"挂号单号", "患者", "时段", "状态", "挂号时间"};
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String STATUS_REGISTERED = "已挂号";
    private static final String STATUS_REFUNDED = "已退号";
    private static final String NO_DOCTOR_HINT = "当前账号未关联医生，无法进行就诊确认";

    private final RegistrationService registrationService = new RegistrationService();

    private final DateSpinner dateSpinner = new DateSpinner(LocalDate.now());
    private final JButton refreshButton = new JButton("刷新");
    private final JButton visitButton = new JButton("标记已就诊");
    private final JLabel hintLabel = new JLabel(" ");
    private final JTable table = TableUtil.createReadOnlyTable(COLUMNS);

    private final List<Registration> rows = new ArrayList<>();

    private final Integer doctorId;

    public VisitPanel() {
        SysUser user = Session.getCurrentUser();
        this.doctorId = user == null ? null : user.getDoctorId();

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
        bar.add(new JLabel("日期："));
        bar.add(dateSpinner);
        bar.add(refreshButton);
        bar.add(hintLabel);
        return bar;
    }

    private JScrollPane buildTableArea() {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("门诊挂号单（仅“已挂号”可确认就诊）"));
        return scrollPane;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        bar.add(visitButton);
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
        refreshButton.addActionListener(e -> reload());
        dateSpinner.addChangeListener(e -> reload());
        visitButton.addActionListener(e -> markVisited());
    }

    private void reload() {
        if (doctorId == null) {
            rows.clear();
            TableUtil.clearRows(table);
            hintLabel.setText(NO_DOCTOR_HINT);
            updateButtons();
            return;
        }
        try {
            // 退号单不属于当日待诊范围，故在展示层排除；先复制一份再过滤，避免影响到 Service 返回的集合
            List<Registration> list = new ArrayList<>(
                    registrationService.findByDoctorAndDate(doctorId, dateSpinner.getLocalDate()));
            list.removeIf(registration -> STATUS_REFUNDED.equals(registration.getStatus()));
            rows.clear();
            List<Object[]> data = new ArrayList<>();
            int registered = 0;
            for (Registration registration : list) {
                if (STATUS_REGISTERED.equals(registration.getStatus())) {
                    registered++;
                }
                rows.add(registration);
                data.add(new Object[]{
                        registration.getRegId(),
                        value(registration.getPatientName()),
                        value(registration.getPeriod()),
                        value(registration.getStatus()),
                        format(registration.getRegTime())
                });
            }
            TableUtil.fillRows(table, data);
            hintLabel.setText("共 " + list.size() + " 条，待就诊 " + registered + " 人");
        } catch (Exception e) {
            UiUtil.showException(this, e);
            hintLabel.setText("加载失败");
        }
        updateButtons();
    }

    private void markVisited() {
        Registration registration = selectedRegistration();
        if (registration == null) {
            return;
        }
        if (!STATUS_REGISTERED.equals(registration.getStatus())) {
            UiUtil.warn(this, "只有“已挂号”的单据可以确认就诊");
            return;
        }
        String message = "确认将患者“" + value(registration.getPatientName()) + "”标记为已就诊？\n"
                + "挂号单号：" + registration.getRegId() + "\n"
                + "时段：" + value(registration.getPeriod());
        if (!UiUtil.confirm(this, message)) {
            return;
        }
        try {
            registrationService.markVisited(registration.getRegId());
            UiUtil.info(this, "已标记为就诊");
            reload();
        } catch (Exception e) {
            UiUtil.error(this, e.getMessage() == null || e.getMessage().trim().isEmpty()
                    ? "标记就诊失败，请稍后重试" : e.getMessage());
        }
    }

    /** 未选中、或状态已经不是“已挂号”时禁用。 */
    private void updateButtons() {
        Registration registration = selectedRegistration();
        visitButton.setEnabled(registration != null && STATUS_REGISTERED.equals(registration.getStatus()));
    }

    private Registration selectedRegistration() {
        int index = TableUtil.getSelectedRowModelIndex(table);
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return rows.get(index);
    }

    private static String format(LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FORMAT);
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }
}
