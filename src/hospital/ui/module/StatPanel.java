package hospital.ui.module;

import hospital.entity.stat.DeptCountStat;
import hospital.entity.stat.IncomeStat;
import hospital.entity.stat.RateStat;
import hospital.service.StatsService;
import hospital.ui.common.DateSpinner;
import hospital.ui.common.TableUtil;
import hospital.ui.common.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 统计报表：四个页签分别展示科室挂号量、号源使用率、退号率与科室收入。
 */
public class StatPanel extends JPanel {

    private static final String[] COUNT_COLUMNS = {"科室", "挂号人次"};
    private static final String[] USAGE_COLUMNS = {"科室", "号源总数", "已挂数", "使用率(%)"};
    private static final String[] REFUND_COLUMNS = {"科室", "挂号数", "退号数", "退号率(%)"};
    private static final String[] INCOME_COLUMNS = {"科室", "收费笔数", "金额（元）"};

    private static final String TAB_COUNT = "挂号量统计";
    private static final String TAB_USAGE = "号源使用率";
    private static final String TAB_REFUND = "退号率";
    private static final String TAB_INCOME = "收入统计";

    private final StatsService statsService = new StatsService();

    private final DateSpinner fromSpinner = new DateSpinner(LocalDate.now().withDayOfMonth(1));
    private final DateSpinner toSpinner = new DateSpinner(LocalDate.now());
    private final JTabbedPane tabbedPane = new JTabbedPane();

    private final JTable countTable = TableUtil.createReadOnlyTable(COUNT_COLUMNS);
    private final JTable usageTable = TableUtil.createReadOnlyTable(USAGE_COLUMNS);
    private final JTable refundTable = TableUtil.createReadOnlyTable(REFUND_COLUMNS);
    private final JTable incomeTable = TableUtil.createReadOnlyTable(INCOME_COLUMNS);

    public StatPanel() {
        this(0);
    }

    /**
     * @param tabIndex 默认选中的页签：0 挂号量、1 号源使用率、2 退号率、3 收入统计
     */
    public StatPanel(int tabIndex) {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(buildQueryBar(), BorderLayout.NORTH);
        add(buildTabbedPane(), BorderLayout.CENTER);

        installCenterRenderer(countTable);
        installCenterRenderer(usageTable);
        installCenterRenderer(refundTable);
        installCenterRenderer(incomeTable);

        tabbedPane.setSelectedIndex(tabIndex >= 0 && tabIndex < tabbedPane.getTabCount() ? tabIndex : 0);
        reloadAll();
    }

    private JPanel buildQueryBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.add(new JLabel("起始日期："));
        bar.add(fromSpinner);
        bar.add(new JLabel("结束日期："));
        bar.add(toSpinner);
        JButton statButton = new JButton("统计");
        statButton.addActionListener(e -> reloadAll());
        bar.add(statButton);
        return bar;
    }

    private JTabbedPane buildTabbedPane() {
        tabbedPane.addTab(TAB_COUNT, wrap(countTable, "按科室统计挂号人次（含已退号）"));
        tabbedPane.addTab(TAB_USAGE, wrap(usageTable, "号源使用率（已挂数 ÷ 号源总数，停诊号源计入分母）"));
        tabbedPane.addTab(TAB_REFUND, wrap(refundTable, "退号数 ÷ 挂号数"));
        tabbedPane.addTab(TAB_INCOME, wrap(incomeTable, "仅统计已收费单，按金额倒序"));
        return tabbedPane;
    }

    private JScrollPane wrap(JTable table, String title) {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder(title));
        return scrollPane;
    }

    private void installCenterRenderer(JTable table) {
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

    /** 四个页签各自独立查询，任一失败只提示一次，不影响其它页签。 */
    private void reloadAll() {
        LocalDate from = fromSpinner.getLocalDate();
        LocalDate to = toSpinner.getLocalDate();
        if (from.isAfter(to)) {
            UiUtil.warn(this, "起始日期不能晚于结束日期");
            return;
        }
        boolean[] errorShown = {false};
        reloadCount(from, to, errorShown);
        reloadUsage(from, to, errorShown);
        reloadRefund(from, to, errorShown);
        reloadIncome(from, to, errorShown);
    }

    private void reloadCount(LocalDate from, LocalDate to, boolean[] errorShown) {
        try {
            List<Object[]> data = new ArrayList<>();
            for (DeptCountStat stat : statsService.countRegByDept(from, to)) {
                data.add(new Object[]{value(stat.getDeptName()), stat.getRegCount()});
            }
            TableUtil.fillRows(countTable, data);
        } catch (Exception e) {
            reportOnce(errorShown, e);
        }
    }

    private void reloadUsage(LocalDate from, LocalDate to, boolean[] errorShown) {
        try {
            List<Object[]> data = new ArrayList<>();
            for (RateStat stat : statsService.slotUsageByDept(from, to)) {
                data.add(new Object[]{value(stat.getDeptName()), stat.getTotal(), stat.getHit(), stat.getRate()});
            }
            TableUtil.fillRows(usageTable, data);
        } catch (Exception e) {
            reportOnce(errorShown, e);
        }
    }

    private void reloadRefund(LocalDate from, LocalDate to, boolean[] errorShown) {
        try {
            List<Object[]> data = new ArrayList<>();
            for (RateStat stat : statsService.refundRateByDept(from, to)) {
                data.add(new Object[]{value(stat.getDeptName()), stat.getTotal(), stat.getHit(), stat.getRate()});
            }
            TableUtil.fillRows(refundTable, data);
        } catch (Exception e) {
            reportOnce(errorShown, e);
        }
    }

    private void reloadIncome(LocalDate from, LocalDate to, boolean[] errorShown) {
        try {
            List<Object[]> data = new ArrayList<>();
            for (IncomeStat stat : statsService.incomeByDept(from, to)) {
                BigDecimal amount = stat.getAmount() == null
                        ? BigDecimal.ZERO : stat.getAmount().setScale(2, RoundingMode.HALF_UP);
                data.add(new Object[]{value(stat.getLabel()), stat.getCount(), amount});
            }
            TableUtil.fillRows(incomeTable, data);
        } catch (Exception e) {
            reportOnce(errorShown, e);
        }
    }

    private void reportOnce(boolean[] errorShown, Exception e) {
        if (!errorShown[0]) {
            errorShown[0] = true;
            UiUtil.showException(this, e);
        }
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }
}
