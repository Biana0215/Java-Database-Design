package hospital.ui.module.dialog;

import hospital.entity.Schedule;
import hospital.entity.Slot;
import hospital.service.SlotService;
import hospital.ui.common.TableUtil;
import hospital.ui.common.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 号源明细对话框：展示某条排班下全部号源的序号与状态，并按状态着色。
 */
public class SlotDetailDialog extends JDialog {

    private static final String[] COLUMNS = {"序号", "状态"};
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String SLOT_AVAILABLE = "可挂";
    private static final String SLOT_TAKEN = "已挂";
    private static final String SLOT_DISABLED = "停用";
    private static final Color COLOR_AVAILABLE = new Color(0, 140, 60);
    private static final Color COLOR_TAKEN = Color.BLACK;
    private static final Color COLOR_DISABLED = Color.GRAY;
    private static final int DIALOG_WIDTH = 520;
    private static final int DIALOG_HEIGHT = 420;

    private final SlotService slotService = new SlotService();
    private final JTable table = TableUtil.createReadOnlyTable(COLUMNS);
    private final JLabel summaryLabel = new JLabel(" ");

    private final Schedule schedule;

    public SlotDetailDialog(Component parent, Schedule schedule) {
        super(SwingUtilities.getWindowAncestor(parent), "号源明细", ModalityType.APPLICATION_MODAL);
        this.schedule = schedule;
        initUi(parent);
        reload();
    }

    private void initUi(Component parent) {
        setLayout(new BorderLayout(0, 8));
        setTitle(buildTitle(schedule));

        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        header.add(summaryLabel);
        add(header, BorderLayout.NORTH);

        installStatusRenderer();
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setBorder(BorderFactory.createTitledBorder("号源状态（可挂绿色、已挂黑色、停用灰色）"));
        add(scrollPane, BorderLayout.CENTER);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton closeButton = new JButton("关闭");
        closeButton.addActionListener(e -> dispose());
        buttonBar.add(closeButton);
        add(buttonBar, BorderLayout.SOUTH);

        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setLocationRelativeTo(parent);
        setResizable(false);
        getRootPane().setDefaultButton(closeButton);
    }

    /** 按号源状态着色：可挂绿色、已挂黑色、停用灰色。 */
    private void installStatusRenderer() {
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                setHorizontalAlignment(SwingConstants.CENTER);
                if (!isSelected) {
                    c.setForeground(colorOf(value == null ? null : value.toString()));
                }
                return c;
            }
        };
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    private void reload() {
        if (schedule == null) {
            TableUtil.clearRows(table);
            summaryLabel.setText("未获取到排班信息");
            return;
        }
        try {
            List<Slot> slots = new ArrayList<>(slotService.listBySchedule(schedule.getScheduleId()));
            slots.sort(Comparator.comparingInt(Slot::getSeqNo));

            List<Object[]> data = new ArrayList<>();
            int available = 0;
            for (Slot slot : slots) {
                if (SLOT_AVAILABLE.equals(slot.getStatus())) {
                    available++;
                }
                data.add(new Object[]{slot.getSeqNo(), slot.getStatus() == null ? "" : slot.getStatus()});
            }
            TableUtil.fillRows(table, data);
            summaryLabel.setText("共 " + slots.size() + " 个号源，可挂 " + available + " 个");
        } catch (Exception e) {
            UiUtil.showException(this, e);
            summaryLabel.setText("号源加载失败");
        }
    }

    private static Color colorOf(String status) {
        if (SLOT_AVAILABLE.equals(status)) {
            return COLOR_AVAILABLE;
        }
        if (SLOT_DISABLED.equals(status)) {
            return COLOR_DISABLED;
        }
        if (SLOT_TAKEN.equals(status)) {
            return COLOR_TAKEN;
        }
        return COLOR_TAKEN;
    }

    private static String buildTitle(Schedule schedule) {
        if (schedule == null) {
            return "号源明细";
        }
        String date = schedule.getWorkDate() == null ? "" : schedule.getWorkDate().format(DATE_FORMAT);
        return "号源明细 - " + value(schedule.getDoctorName()) + " " + date + " " + value(schedule.getPeriod());
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }
}
