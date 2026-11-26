package hospital.ui.common;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.Font;
import java.util.List;

/**
 * 表格创建与数据填充工具，JScrollPane 由调用方负责添加。
 */
public final class TableUtil {

    private static final int ROW_HEIGHT = 24;
    private static final int FONT_SIZE = 14;

    private TableUtil() {
    }

    /**
     * 创建单元格不可编辑、整行单选、表头可排序的表格。
     */
    public static JTable createReadOnlyTable(String[] columnNames) {
        String[] names = columnNames == null ? new String[0] : columnNames;
        DefaultTableModel model = new DefaultTableModel(names, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(ROW_HEIGHT);
        table.setFont(new Font("微软雅黑", Font.PLAIN, FONT_SIZE));
        table.getTableHeader().setFont(new Font("微软雅黑", Font.BOLD, FONT_SIZE));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        return table;
    }

    public static void clearRows(JTable table) {
        model(table).setRowCount(0);
    }

    public static void addRow(JTable table, Object[] values) {
        model(table).addRow(values == null ? new Object[0] : values);
    }

    /**
     * 先清空再批量填充，rows 为空时仅清空。
     */
    public static void fillRows(JTable table, List<Object[]> rows) {
        clearRows(table);
        if (rows == null) {
            return;
        }
        DefaultTableModel model = model(table);
        for (Object[] row : rows) {
            if (row != null) {
                model.addRow(row);
            }
        }
    }

    /**
     * 选中行在模型中的索引（排序后视图行号会变），未选中返回 -1。
     */
    public static int getSelectedRowModelIndex(JTable table) {
        if (table == null) {
            return -1;
        }
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return -1;
        }
        return table.convertRowIndexToModel(viewRow);
    }

    private static DefaultTableModel model(JTable table) {
        if (table == null || !(table.getModel() instanceof DefaultTableModel)) {
            throw new IllegalArgumentException("表格必须由 TableUtil.createReadOnlyTable 创建");
        }
        return (DefaultTableModel) table.getModel();
    }
}
