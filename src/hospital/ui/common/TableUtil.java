package hospital.ui.common;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.List;

/**
 * 表格创建与数据填充工具，JScrollPane 由调用方负责添加。
 * <p>
 * 斑马纹、选中底色、数字列右对齐都做在 {@link JTable#prepareRenderer} 上：
 * 各业务面板会为列单独装渲染器，只有在这一层拦截才能让两种渲染器叠加生效。
 */
public final class TableUtil {

    private static final int ROW_HEIGHT = 30;
    private static final int CELL_PADDING = 8;
    private static final int FONT_SIZE = 14;

    /** 列名含这些关键字时按键数字列处理（右对齐）。 */
    private static final String[] NUMERIC_KEYWORDS = {"数", "量", "额", "率", "人次", "笔数", "号"};

    /** 选中行底色：主色 15% 叠加白底。 */
    private static final Color SELECTION_BACKGROUND = UiTheme.blend(UiTheme.PRIMARY, Color.WHITE, 0.15);

    private TableUtil() {
    }

    /**
     * 创建单元格不可编辑、整行单选、表头可排序的表格，并统一行高、斑马纹与对齐方式。
     */
    public static JTable createReadOnlyTable(String[] columnNames) {
        final String[] names = columnNames == null ? new String[0] : columnNames.clone();
        DefaultTableModel model = new DefaultTableModel(names, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component cell = super.prepareRenderer(renderer, row, column);
                if (isRowSelected(row)) {
                    cell.setBackground(SELECTION_BACKGROUND);
                    cell.setForeground(UiTheme.TEXT_PRIMARY);
                } else {
                    // 1 基的偶数行铺浅灰，形成斑马纹
                    cell.setBackground(row % 2 == 1 ? UiTheme.ZEBRA : Color.WHITE);
                }
                if (cell instanceof JLabel) {
                    JLabel label = (JLabel) cell;
                    int modelColumn = convertColumnIndexToModel(column);
                    if (modelColumn >= 0 && isNumericColumn(getModel().getColumnName(modelColumn))) {
                        label.setHorizontalAlignment(SwingConstants.RIGHT);
                    }
                    label.setBorder(cellPadding(label.getBorder()));
                }
                return cell;
            }
        };
        table.setRowHeight(ROW_HEIGHT);
        table.setFont(UiTheme.font(Font.PLAIN, FONT_SIZE));
        table.setForeground(UiTheme.TEXT_PRIMARY);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(SELECTION_BACKGROUND);
        table.setSelectionForeground(UiTheme.TEXT_PRIMARY);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(false);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(false);
        table.setGridColor(UiTheme.DIVIDER);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        header.setFont(UiTheme.font(Font.BOLD, FONT_SIZE));
        header.setPreferredSize(new Dimension(0, 34));
        header.setDefaultRenderer(new HeaderRenderer());
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

    /**
     * 列名关键字判定：数、量、额、率、人次、笔数、号 视为数字列。
     */
    private static boolean isNumericColumn(String columnName) {
        if (columnName == null) {
            return false;
        }
        for (String keyword : NUMERIC_KEYWORDS) {
            if (columnName.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 左右各留 8px；只替换空白边框，避免覆盖渲染器自带的装饰边框。
     */
    private static Border cellPadding(Border current) {
        if (current == null || current instanceof EmptyBorder) {
            return BorderFactory.createEmptyBorder(0, CELL_PADDING, 0, CELL_PADDING);
        }
        return current;
    }

    private static DefaultTableModel model(JTable table) {
        if (table == null || !(table.getModel() instanceof DefaultTableModel)) {
            throw new IllegalArgumentException("表格必须由 TableUtil.createReadOnlyTable 创建");
        }
        return (DefaultTableModel) table.getModel();
    }

    /**
     * 表头：浅灰底、加粗、居中，高度与主题一致。
     */
    private static final class HeaderRenderer extends DefaultTableCellRenderer {

        private HeaderRenderer() {
            setOpaque(true);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(UiTheme.font(Font.BOLD, FONT_SIZE));
            setBackground(UiTheme.TABLE_HEADER_BG);
            setForeground(UiTheme.TEXT_PRIMARY);
            setBorder(BorderFactory.createEmptyBorder(0, CELL_PADDING, 0, CELL_PADDING));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(SwingConstants.CENTER);
            setFont(UiTheme.font(Font.BOLD, FONT_SIZE));
            setBackground(UiTheme.TABLE_HEADER_BG);
            setForeground(UiTheme.TEXT_PRIMARY);
            setBorder(BorderFactory.createEmptyBorder(0, CELL_PADDING, 0, CELL_PADDING));
            return this;
        }
    }
}
