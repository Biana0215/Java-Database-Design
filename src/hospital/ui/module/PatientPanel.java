package hospital.ui.module;

import hospital.entity.Patient;
import hospital.service.PatientService;
import hospital.ui.common.TableUtil;
import hospital.ui.common.UiUtil;
import hospital.ui.module.dialog.PatientEditDialog;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * 患者管理：按姓名/证件号/电话检索患者档案，支持新增与修改。
 */
public class PatientPanel extends JPanel {

    private static final String[] COLUMNS = {"编号", "姓名", "性别", "出生日期", "证件号",
            "电话", "住址", "建档时间"};
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final PatientService patientService = new PatientService();

    private final JTextField keywordField = new JTextField(14);
    private final JTable table = TableUtil.createReadOnlyTable(COLUMNS);
    private final JButton addButton = new JButton("新增");
    private final JButton editButton = new JButton("修改");
    private final JButton refreshButton = new JButton("刷新");

    private final List<Patient> rows = new ArrayList<>();

    private JScrollPane tableScroll;

    public PatientPanel() {
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
        bar.add(new JLabel("关键词："));
        keywordField.setToolTipText("姓名 / 证件号 / 电话");
        bar.add(keywordField);
        JButton queryButton = new JButton("查询");
        queryButton.addActionListener(e -> reload());
        bar.add(queryButton);
        JButton topAddButton = new JButton("新增");
        topAddButton.addActionListener(e -> openEditor(null));
        bar.add(topAddButton);
        return bar;
    }

    private JScrollPane buildTableArea() {
        tableScroll = new JScrollPane(table);
        tableScroll.setBorder(BorderFactory.createTitledBorder("患者档案（共 0 条）"));
        return tableScroll;
    }

    private JPanel buildButtonBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        bar.add(addButton);
        bar.add(editButton);
        bar.add(refreshButton);
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
        addButton.addActionListener(e -> openEditor(null));
        editButton.addActionListener(e -> openEditor(selectedPatient()));
        refreshButton.addActionListener(e -> reload());
        keywordField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    reload();
                }
            }
        });
    }

    private void reload() {
        try {
            List<Patient> list = patientService.search(keywordField.getText());
            rows.clear();
            List<Object[]> data = new ArrayList<>();
            for (Patient patient : list) {
                rows.add(patient);
                data.add(new Object[]{
                        patient.getPatientId(),
                        value(patient.getPatientName()),
                        value(patient.getGender()),
                        format(patient.getBirthDate()),
                        value(patient.getIdCard()),
                        value(patient.getPhone()),
                        value(patient.getAddress()),
                        format(patient.getCreateTime())
                });
            }
            TableUtil.fillRows(table, data);
            setTableTitle("患者档案（共 " + list.size() + " 条）");
        } catch (Exception e) {
            UiUtil.showException(this, e);
        }
        updateButtons();
    }

    private void openEditor(Patient patient) {
        PatientEditDialog dialog = new PatientEditDialog(this, patient);
        dialog.setVisible(true);
        if (dialog.isConfirmed()) {
            reload();
        }
    }

    private void updateButtons() {
        editButton.setEnabled(selectedPatient() != null);
    }

    private Patient selectedPatient() {
        int index = TableUtil.getSelectedRowModelIndex(table);
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return rows.get(index);
    }

    private void setTableTitle(String title) {
        if (tableScroll != null) {
            tableScroll.setBorder(BorderFactory.createTitledBorder(title));
        }
    }

    private static String format(LocalDate date) {
        return date == null ? "" : date.format(DATE_FORMAT);
    }

    private static String format(LocalDateTime time) {
        return time == null ? "" : time.format(TIME_FORMAT);
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }
}
