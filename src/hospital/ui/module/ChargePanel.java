package hospital.ui.module;

import hospital.entity.Charge;
import hospital.entity.Registration;
import hospital.entity.SysUser;
import hospital.service.BusinessException;
import hospital.service.ChargeService;
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
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 收费面板：按日期与挂号状态查询挂号单，逐行显示收费状态，对选中单据按支付方式收费。
 */
public class ChargePanel extends JPanel {

    private static final String[] COLUMNS = {"挂号单号", "患者", "医生", "就诊日期",
            "挂号费", "挂号状态", "收费状态"};
    private static final String[] STATUS_OPTIONS = {"已挂号", "已就诊", "全部"};
    private static final String[] PAY_METHODS = {"现金", "微信", "支付宝", "银行卡"};
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String ALL = "全部";
    private static final String NOT_CHARGED = "未收费";
    private static final String UNCHARGED_HINT = "尚未收费";

    private final RegistrationService registrationService = new RegistrationService();
    private final ChargeService chargeService = new ChargeService();

    private final DateSpinner dateSpinner = new DateSpinner(LocalDate.now());
    private final JComboBox<String> statusCombo = new JComboBox<>(STATUS_OPTIONS);
    private final JTable table = TableUtil.createReadOnlyTable(COLUMNS);
    private final JButton chargeButton = new JButton("收费");

    private final List<Registration> rows = new ArrayList<>();
    private final List<String> chargeStatuses = new ArrayList<>();

    public ChargePanel() {
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
        scrollPane.setBorder(BorderFactory.createTitledBorder("挂号单与收费状态"));
        return scrollPane;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        bar.add(chargeButton);
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
                chargeButton.setEnabled(selectedRegistration() != null);
            }
        });
        chargeButton.addActionListener(e -> doCharge());
    }

    private void reload() {
        String status = (String) statusCombo.getSelectedItem();
        try {
            List<Registration> list = registrationService.search(null, dateSpinner.getLocalDate(),
                    null, ALL.equals(status) ? null : status);
            rows.clear();
            chargeStatuses.clear();
            List<Object[]> data = new ArrayList<>();
            for (Registration registration : list) {
                String chargeStatus = chargeStatusOf(registration.getRegId());
                rows.add(registration);
                chargeStatuses.add(chargeStatus);
                data.add(new Object[]{
                        registration.getRegId(),
                        value(registration.getPatientName()),
                        value(registration.getDoctorName()),
                        format(registration.getWorkDate()),
                        registration.getRegFee() == null ? null
                                : registration.getRegFee().setScale(2, RoundingMode.HALF_UP),
                        value(registration.getStatus()),
                        chargeStatus
                });
            }
            TableUtil.fillRows(table, data);
        } catch (Exception e) {
            UiUtil.showException(this, e);
        }
        chargeButton.setEnabled(selectedRegistration() != null);
    }

    /**
     * 逐行取收费状态：服务在“尚未收费”时抛业务异常，据此显示“未收费”；
     * 其它业务异常（如查库失败）继续上抛，由 reload 统一提示，不吞异常。
     */
    private String chargeStatusOf(int regId) {
        try {
            Charge charge = chargeService.getByRegId(regId);
            return value(charge.getStatus());
        } catch (BusinessException e) {
            if (e.getMessage() != null && e.getMessage().contains(UNCHARGED_HINT)) {
                return NOT_CHARGED;
            }
            throw e;
        }
    }

    private void doCharge() {
        Registration registration = selectedRegistration();
        if (registration == null) {
            return;
        }
        String chargeStatus = selectedChargeStatus();
        if (!NOT_CHARGED.equals(chargeStatus)) {
            UiUtil.warn(this, "该挂号单收费状态为“" + chargeStatus + "”，不能重复收费");
            return;
        }
        int operatorId = currentUserId();
        if (operatorId <= 0) {
            UiUtil.error(this, "登录状态已失效，请重新登录");
            return;
        }

        JComboBox<String> methodCombo = new JComboBox<>(PAY_METHODS);
        String tip = "挂号单号：" + registration.getRegId() + "\n"
                + "患者：" + value(registration.getPatientName()) + "\n"
                + "金额：" + money(registration.getRegFee()) + " 元\n"
                + "请选择支付方式：";
        Object[] content = {tip, methodCombo};
        int result = JOptionPane.showConfirmDialog(this, content, "收费",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return;
        }
        String payMethod = (String) methodCombo.getSelectedItem();

        try {
            chargeService.charge(registration.getRegId(), payMethod, operatorId);
            UiUtil.info(this, "收费成功，金额 " + money(registration.getRegFee()) + " 元");
            reload();
        } catch (Exception e) {
            UiUtil.error(this, e.getMessage() == null || e.getMessage().trim().isEmpty()
                    ? "收费失败，请稍后重试" : e.getMessage());
        }
    }

    private Registration selectedRegistration() {
        int index = TableUtil.getSelectedRowModelIndex(table);
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return rows.get(index);
    }

    private String selectedChargeStatus() {
        int index = TableUtil.getSelectedRowModelIndex(table);
        if (index < 0 || index >= chargeStatuses.size()) {
            return "";
        }
        return chargeStatuses.get(index);
    }

    private static int currentUserId() {
        SysUser user = Session.getCurrentUser();
        return user == null ? -1 : user.getUserId();
    }

    private static String format(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMAT);
    }

    private static String money(BigDecimal amount) {
        return amount == null ? "0.00" : amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }
}
