package hospital.ui.module.dialog;

import hospital.entity.Patient;
import hospital.service.PatientService;
import hospital.ui.common.DateSpinner;
import hospital.ui.common.UiUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.time.LocalDate;

/**
 * 患者编辑对话框：新增或修改患者档案，证件号建档后不可改（与 PatientService.update 的字段范围一致）。
 */
public class PatientEditDialog extends JDialog {

    private static final String[] GENDERS = {"男", "女", "未填写"};
    private static final String GENDER_EMPTY = "未填写";
    private static final int DIALOG_WIDTH = 420;
    private static final int DIALOG_HEIGHT = 330;
    private static final int FIELD_WIDTH = 220;
    private static final int FIELD_HEIGHT = 26;
    private static final int DEFAULT_AGE = 30;

    private final PatientService patientService = new PatientService();

    private final JTextField nameField = new JTextField();
    private final JComboBox<String> genderCombo = new JComboBox<>(GENDERS);
    private final DateSpinner birthSpinner = new DateSpinner(LocalDate.now().minusYears(DEFAULT_AGE));
    private final JTextField idCardField = new JTextField();
    private final JTextField phoneField = new JTextField();
    private final JTextField addressField = new JTextField();

    /** 为空表示新增，非空表示修改。 */
    private final Patient target;
    private boolean confirmed;

    public PatientEditDialog(Component parent, Patient patient) {
        super(SwingUtilities.getWindowAncestor(parent),
                patient == null ? "新增患者" : "修改患者", ModalityType.APPLICATION_MODAL);
        this.target = patient;
        initUi(parent);
        fillFields();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void initUi(Component parent) {
        setLayout(new BorderLayout());
        setResizable(false);

        JPanel form = new JPanel(new GridLayout(6, 2, 10, 10));
        form.setBorder(BorderFactory.createEmptyBorder(18, 18, 8, 18));
        form.add(new JLabel("姓名："));
        form.add(nameField);
        form.add(new JLabel("性别："));
        form.add(wrapLeft(genderCombo));
        form.add(new JLabel("出生日期："));
        form.add(wrapLeft(birthSpinner));
        form.add(new JLabel("证件号："));
        form.add(idCardField);
        form.add(new JLabel("电话："));
        form.add(phoneField);
        form.add(new JLabel("住址："));
        form.add(addressField);

        JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 8));
        JButton okButton = new JButton("确定");
        JButton cancelButton = new JButton("取消");
        okButton.addActionListener(e -> onConfirm());
        cancelButton.addActionListener(e -> dispose());
        buttonBar.add(okButton);
        buttonBar.add(cancelButton);

        add(form, BorderLayout.CENTER);
        add(buttonBar, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(okButton);

        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setLocationRelativeTo(parent);
    }

    private JPanel wrapLeft(JComponent component) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        component.setPreferredSize(new Dimension(FIELD_WIDTH, FIELD_HEIGHT));
        panel.add(component);
        return panel;
    }

    /** 修改时回填原值，并把证件号锁死（后端 update 也不改证件号）。 */
    private void fillFields() {
        if (target == null) {
            return;
        }
        nameField.setText(value(target.getPatientName()));
        genderCombo.setSelectedItem(target.getGender() == null ? GENDER_EMPTY : target.getGender());
        if (target.getBirthDate() != null) {
            birthSpinner.setLocalDate(target.getBirthDate());
        }
        idCardField.setText(value(target.getIdCard()));
        idCardField.setEditable(false);
        idCardField.setFocusable(false);
        idCardField.setToolTipText("证件号建档后不可修改");
        phoneField.setText(value(target.getPhone()));
        addressField.setText(value(target.getAddress()));
    }

    private void onConfirm() {
        String name = nameField.getText() == null ? "" : nameField.getText().trim();
        if (name.isEmpty()) {
            UiUtil.warn(this, "患者姓名不能为空");
            return;
        }
        String gender = (String) genderCombo.getSelectedItem();
        if (GENDER_EMPTY.equals(gender)) {
            gender = null;
        }
        String idCard = idCardField.getText() == null ? "" : idCardField.getText().trim();
        String phone = phoneField.getText() == null ? "" : phoneField.getText().trim();
        String address = addressField.getText() == null ? "" : addressField.getText().trim();

        Patient patient = target == null ? new Patient() : target;
        patient.setPatientName(name);
        patient.setGender(gender);
        patient.setBirthDate(birthSpinner.getLocalDate());
        patient.setIdCard(idCard.isEmpty() ? null : idCard);
        patient.setPhone(phone.isEmpty() ? null : phone);
        patient.setAddress(address.isEmpty() ? null : address);

        try {
            if (target == null) {
                patientService.create(patient);
                UiUtil.info(this, "患者建档成功");
            } else {
                patientService.update(patient);
                UiUtil.info(this, "患者信息已保存");
            }
            confirmed = true;
            dispose();
        } catch (Exception e) {
            // 证件号重复等提示由后端给出，原样展示且不关闭对话框，方便直接改
            UiUtil.error(this, e.getMessage() == null || e.getMessage().trim().isEmpty()
                    ? "保存患者失败，请稍后重试" : e.getMessage());
        }
    }

    private static String value(String text) {
        return text == null ? "" : text;
    }
}
