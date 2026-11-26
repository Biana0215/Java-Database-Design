package hospital.ui.module;

import hospital.entity.stat.IncomeStat;
import hospital.service.StatsService;
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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 日结汇总面板：按支付方式汇总当日收费金额，退费记负数，并给出合计笔数与净收入。
 */
public class DailySummaryPanel extends JPanel {

    private static final String[] COLUMNS = {"支付方式", "笔数", "金额（元）"};
    private static final Color COLOR_NEGATIVE = new Color(200, 0, 0);
    private static final int AMOUNT_COLUMN = 2;

    private final StatsService statsService = new StatsService();

    private final DateSpinner dateSpinner = new DateSpinner(LocalDate.now());
    private final JTable table = TableUtil.createReadOnlyTable(COLUMNS);
    private final JLabel summaryLabel = new JLabel("合计笔数 0，净收入 0.00 元");

    public DailySummaryPanel() {
        setLayout(new BorderLayout(0, 8));
        setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        add(buildQueryBar(), BorderLayout.NORTH);
        add(buildTableArea(), BorderLayout.CENTER);
        add(buildSummaryBar(), BorderLayout.SOUTH);

        installAmountRenderer();
        reload();
    }

    private JPanel buildQueryBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.add(new JLabel("日期："));
        bar.add(dateSpinner);
        JButton statButton = new JButton("统计");
        statButton.addActionListener(e -> reload());
        bar.add(statButton);
        return bar;
    }

    private JScrollPane buildTableArea() {
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("按支付方式汇总"));
        return scrollPane;
    }

    private JPanel buildSummaryBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        bar.add(summaryLabel);
        return bar;
    }

    /** 金额为负数的行用红色显示。 */
    private void installAmountRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (!isSelected && column == AMOUNT_COLUMN && value instanceof BigDecimal amount
                        && amount.signum() < 0) {
                    c.setForeground(COLOR_NEGATIVE);
                }
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(140);
        table.setPreferredScrollableViewportSize(new Dimension(0, 0));
    }

    private void reload() {
        LocalDate date = dateSpinner.getLocalDate();
        try {
            List<IncomeStat> list = statsService.dailySummary(date);
            List<Object[]> data = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO;
            int count = 0;
            for (IncomeStat stat : list) {
                BigDecimal amount = stat.getAmount() == null
                        ? BigDecimal.ZERO : stat.getAmount().setScale(2, RoundingMode.HALF_UP);
                total = total.add(amount);
                count += stat.getCount();
                data.add(new Object[]{value(stat.getLabel()), stat.getCount(), amount});
            }
            TableUtil.fillRows(table, data);
            summaryLabel.setText("合计笔数 " + count + "，净收入 "
                    + total.setScale(2, RoundingMode.HALF_UP).toPlainString() + " 元");
        } catch (Exception e) {
            UiUtil.showException(this, e);
            summaryLabel.setText("统计失败，请稍后重试");
        }
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }
}
