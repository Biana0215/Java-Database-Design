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
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 今日挂号：当前医生按日期查看自己的挂号单，并统计挂号人数与已就诊人数。
 */
public class DoctorWorkPanel extends JPanel {

    private static final String[] COLUMNS = {"挂号单号", "患者", "时段", "挂号费", "状态", "挂号时间"};
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String STATUS_VISITED = "已就诊";
    private static final String STATUS_REFUNDED = "已退号";
    private static final String NO_DOCTOR_HINT = "当前账号未关联医生，无法查看门诊列表";

    private final RegistrationService registrationService = new RegistrationService();

    private final DateSpinner dateSpinner = new DateSpinner(LocalDate.now());
    private final JButton refreshButton = new JButton("刷新");
    private final JLabel statLabel = new JLabel(" ");
    private final JTable table = TableUtil.createReadOnlyTable(COLUMNS);

    private final List<Registration> rows = new ArrayList<>();

    /** 当前登录医生，未关联时为 null。 */
    private final Integer doctorId;

    public DoctorWorkPanel() {
        SysUser user = Session.getCurrentUser();
        this.doctorId = user == null ? null : user.getDoctorId();

        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(buildQueryBar(), BorderLayout.NORTH);
        add(buildTableArea(), BorderLayout.CENTER);

        installCenterRenderer();
        refreshButton.addActionListener(e -> reload());
        dateSpinner.addChangeListener(e -> reload());
        reload();
    }

    private JPanel buildQueryBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.add(new JLabel("日期："));
        bar.add(dateSpinner);
        bar.add(refreshButton);
        bar.add(statLabel);
        return bar;
    }

    private JScrollPane buildTableArea() {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("我的门诊挂号单"));
        return scrollPane;
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

    private void reload() {
        if (doctorId == null) {
            rows.clear();
            TableUtil.clearRows(table);
            statLabel.setText(NO_DOCTOR_HINT);
            return;
        }
        try {
            // 退号单不属于当日待诊范围，故在展示层排除；先复制一份再过滤，避免影响到 Service 返回的集合
            List<Registration> list = new ArrayList<>(
                    registrationService.findByDoctorAndDate(doctorId, dateSpinner.getLocalDate()));
            list.removeIf(registration -> STATUS_REFUNDED.equals(registration.getStatus()));
            rows.clear();
            List<Object[]> data = new ArrayList<>();
            int visited = 0;
            for (Registration registration : list) {
                if (STATUS_VISITED.equals(registration.getStatus())) {
                    visited++;
                }
                rows.add(registration);
                data.add(new Object[]{
                        registration.getRegId(),
                        value(registration.getPatientName()),
                        value(registration.getPeriod()),
                        registration.getRegFee() == null ? null
                                : registration.getRegFee().setScale(2, RoundingMode.HALF_UP),
                        value(registration.getStatus()),
                        format(registration.getRegTime())
                });
            }
            TableUtil.fillRows(table, data);
            statLabel.setText("今日挂号 " + list.size() + " 人，已就诊 " + visited + " 人");
        } catch (Exception e) {
            UiUtil.showException(this, e);
            statLabel.setText("加载失败");
        }
    }

    private static String format(LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FORMAT);
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }
}
