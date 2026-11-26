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
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 退号面板：按患者姓名、日期、状态查询挂号单，对“已挂号”单据办理退号并释放号源。
 */
public class RefundPanel extends JPanel {

    private static final String[] COLUMNS = {"挂号单号", "患者", "医生", "就诊日期", "时段",
            "挂号费", "状态", "挂号时间"};
    private static final String[] STATUS_OPTIONS = {"已挂号", "已就诊", "已退号", "全部"};
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String ALL = "全部";
    private static final String STATUS_REGISTERED = "已挂号";

    private final RegistrationService registrationService = new RegistrationService();

    private final JTextField keywordField = new JTextField(10);
    private final DateSpinner dateSpinner = new DateSpinner(LocalDate.now());
    private final JComboBox<String> statusCombo = new JComboBox<>(STATUS_OPTIONS);
    private final JTable table = TableUtil.createReadOnlyTable(COLUMNS);
    private final JButton refundButton = new JButton("退号");

    private final List<Registration> rows = new ArrayList<>();

    public RefundPanel() {
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
        bar.add(new JLabel("患者姓名："));
        bar.add(keywordField);
        bar.add(new JLabel("日期："));
        bar.add(dateSpinner);
        statusCombo.setPreferredSize(new Dimension(100, 28));
        bar.add(new JLabel("状态："));
        bar.add(statusCombo);
        JButton queryButton = new JButton("查询");
        queryButton.addActionListener(e -> reload());
        bar.add(queryButton);
        return bar;
    }

    private JScrollPane buildTableArea() {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("挂号单列表（仅“已挂号”可退号）"));
        return scrollPane;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        bar.add(refundButton);
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
        refundButton.addActionListener(e -> doRefund());
    }

    /** 服务只支持按患者编号过滤，姓名关键词在内存里过滤（结果已按日期与状态缩小）。 */
    private void reload() {
        String status = (String) statusCombo.getSelectedItem();
        String keyword = keywordField.getText() == null ? "" : keywordField.getText().trim();
        try {
            List<Registration> list = registrationService.search(null, dateSpinner.getLocalDate(),
                    null, ALL.equals(status) ? null : status);
            rows.clear();
            List<Object[]> data = new ArrayList<>();
            for (Registration registration : list) {
                if (!keyword.isEmpty() && !value(registration.getPatientName()).contains(keyword)) {
                    continue;
                }
                rows.add(registration);
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
            TableUtil.fillRows(table, data);
        } catch (Exception e) {
            UiUtil.showException(this, e);
        }
        updateButtons();
    }

    private void doRefund() {
        Registration registration = selectedRegistration();
        if (registration == null) {
            return;
        }
        if (!STATUS_REGISTERED.equals(registration.getStatus())) {
            UiUtil.warn(this, "只有“已挂号”的单据可以退号");
            return;
        }
        int operatorId = currentUserId();
        if (operatorId <= 0) {
            UiUtil.error(this, "登录状态已失效，请重新登录");
            return;
        }

        String message = "确认退号？\n"
                + "患者：" + value(registration.getPatientName()) + "\n"
                + "医生：" + value(registration.getDoctorName()) + "\n"
                + "就诊：" + format(registration.getWorkDate()) + " " + value(registration.getPeriod()) + "\n"
                + "挂号费：" + money(registration.getRegFee()) + " 元\n"
                + "退号后号源将释放，已收费的将同步退费。";
        if (!UiUtil.confirm(this, message)) {
            return;
        }

        String reason = JOptionPane.showInputDialog(this, "请输入退号原因（可留空）：",
                "退号原因", JOptionPane.PLAIN_MESSAGE);
        if (reason == null) {
            return;
        }
        String reasonText = reason.trim();

        try {
            registrationService.refund(registration.getRegId(), operatorId,
                    reasonText.isEmpty() ? null : reasonText);
            UiUtil.info(this, "退号成功，挂号单号 " + registration.getRegId());
            reload();
        } catch (Exception e) {
            UiUtil.error(this, e.getMessage() == null || e.getMessage().trim().isEmpty()
                    ? "退号失败，请稍后重试" : e.getMessage());
        }
    }

    private void updateButtons() {
        refundButton.setEnabled(selectedRegistration() != null);
    }

    private Registration selectedRegistration() {
        int index = TableUtil.getSelectedRowModelIndex(table);
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return rows.get(index);
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
}
