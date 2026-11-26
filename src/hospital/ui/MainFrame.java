package hospital.ui;

import hospital.entity.SysUser;
import hospital.ui.common.UiTheme;
import hospital.ui.module.*;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 主窗体：顶部信息栏 + 左侧按角色生成的菜单 + 右侧 CardLayout 内容区。
 */
public class MainFrame extends JFrame {

    private static final String ROLE_ADMIN = "管理员";
    private static final String ROLE_REGISTRAR = "挂号员";
    private static final String ROLE_DOCTOR = "医生";

    private static final int MENU_WIDTH = 190;
    private static final int MENU_ITEM_HEIGHT = 38;
    private static final int TOP_BAR_HEIGHT = 52;
    private static final int SELECTED_BAR_WIDTH = 3;
    private static final String GROUP_PREFIX = "—— ";

    private final CardLayout cardLayout = new CardLayout();
    private final JPanel cardPanel = new JPanel(cardLayout);
    private final JLabel moduleTitle = new JLabel(" ", SwingConstants.LEFT);
    private final DefaultListModel<String> menuModel = new DefaultListModel<>();
    private final JList<String> menuList = new JList<>(menuModel);
    private final Map<String, MenuItem> menuIndex = new HashMap<>();

    public MainFrame(SysUser user) {
        super("医院挂号系统");
        SysUser current = user != null ? user : Session.getCurrentUser();
        if (current != null && Session.getCurrentUser() == null) {
            Session.setCurrentUser(current);
        }
        initUi();
    }

    private void initUi() {
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(UiTheme.BACKGROUND);

        add(buildTopBar(), BorderLayout.NORTH);

        List<MenuItem> items = buildMenu(Session.getRole());
        fillMenu(items);
        buildCards(items);

        JScrollPane menuScroll = new JScrollPane(menuList);
        menuScroll.setPreferredSize(new Dimension(MENU_WIDTH, 0));
        menuScroll.setBorder(BorderFactory.createEmptyBorder());
        menuScroll.getViewport().setBackground(Color.WHITE);
        menuScroll.setBackground(Color.WHITE);
        menuScroll.getVerticalScrollBar().setUnitIncrement(16);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, menuScroll, buildContentPanel());
        splitPane.setDividerLocation(MENU_WIDTH);
        splitPane.setDividerSize(1);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        splitPane.setBackground(UiTheme.DIVIDER);
        add(splitPane, BorderLayout.CENTER);

        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        selectFirstModule();
    }

    /**
     * 顶部信息栏：主色深底，左侧系统名与角色标识，右侧白字退出登录。
     */
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(UiTheme.PRIMARY_DARK);
        bar.setPreferredSize(new Dimension(0, TOP_BAR_HEIGHT));
        bar.setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 12));

        JPanel left = new JPanel(new GridBagLayout());
        left.setOpaque(false);

        JLabel brand = new JLabel("医院挂号系统");
        brand.setFont(UiTheme.titleFont(16, true));
        brand.setForeground(Color.WHITE);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 0, 10);
        left.add(brand, gbc);

        String role = roleText();
        if (!role.isEmpty()) {
            gbc.insets = new Insets(0, 0, 0, 0);
            left.add(new RoleBadge(role, roleColor(role)), gbc);
        }
        bar.add(left, BorderLayout.WEST);

        JButton logoutButton = new JButton("退出登录");
        logoutButton.setFont(UiTheme.font(Font.PLAIN, 14));
        logoutButton.setForeground(Color.WHITE);
        logoutButton.setContentAreaFilled(false);
        logoutButton.setBorder(BorderFactory.createEmptyBorder(6, 14, 6, 14));
        logoutButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        logoutButton.addActionListener(e -> logout());

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(logoutButton, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    /**
     * 右侧内容区：模块标题（18px 加粗）+ 一条浅色分隔线 + 卡片面板。
     */
    private JPanel buildContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(UiTheme.BACKGROUND);

        JPanel titleArea = new JPanel(new BorderLayout());
        titleArea.setBackground(UiTheme.BACKGROUND);
        titleArea.setBorder(BorderFactory.createEmptyBorder(16, 20, 10, 20));

        moduleTitle.setFont(UiTheme.titleFont(18, true));
        moduleTitle.setForeground(UiTheme.TEXT_PRIMARY);
        titleArea.add(moduleTitle, BorderLayout.CENTER);

        JPanel divider = new JPanel();
        divider.setBackground(UiTheme.DIVIDER);
        divider.setPreferredSize(new Dimension(0, 1));

        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UiTheme.BACKGROUND);
        header.add(titleArea, BorderLayout.CENTER);
        header.add(divider, BorderLayout.SOUTH);
        panel.add(header, BorderLayout.NORTH);

        cardPanel.setBackground(UiTheme.BACKGROUND);
        cardPanel.setBorder(BorderFactory.createEmptyBorder(12, 20, 16, 20));
        panel.add(cardPanel, BorderLayout.CENTER);
        return panel;
    }

    /**
     * 按角色生成菜单项，分组顺序与权限矩阵一致。
     */
    private static List<MenuItem> buildMenu(String role) {
        List<MenuItem> items = new ArrayList<>();
        if (ROLE_ADMIN.equals(role)) {
            addGroup(items, "基础资料", "患者管理", "科室管理", "医生管理", "用户管理");
            addGroup(items, "排班号源", "排班管理", "排班查询");
            addGroup(items, "挂号收费", "挂号", "退号", "收费", "日结汇总");
            addGroup(items, "医生工作", "今日挂号", "就诊确认");
            addGroup(items, "统计报表", "挂号量统计", "号源使用率", "退号率", "收入统计");
        } else if (ROLE_REGISTRAR.equals(role)) {
            addGroup(items, "基础资料", "患者管理");
            addGroup(items, "挂号收费", "挂号", "退号", "收费", "日结汇总");
        } else if (ROLE_DOCTOR.equals(role)) {
            addGroup(items, "医生工作", "今日挂号", "就诊确认");
        }
        return items;
    }

    private static void addGroup(List<MenuItem> items, String group, String... names) {
        for (String name : names) {
            items.add(new MenuItem(group, name));
        }
    }

    /**
     * 填充菜单列表，每组前插入一行组名作为分隔。
     */
    private void fillMenu(List<MenuItem> items) {
        menuModel.clear();
        menuIndex.clear();

        menuList.setFont(UiTheme.font(Font.PLAIN, 14));
        menuList.setFixedCellHeight(MENU_ITEM_HEIGHT);
        menuList.setBackground(Color.WHITE);
        menuList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuList.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        menuList.setCellRenderer(new MenuRenderer());
        menuList.addListSelectionListener(e -> {
            if (e.getValueIsAdjusting()) {
                return;
            }
            String value = menuList.getSelectedValue();
            if (value == null || value.startsWith(GROUP_PREFIX)) {
                // 组名只是分隔行，不允许选中
                menuList.clearSelection();
                return;
            }
            MenuItem item = menuIndex.get(value);
            if (item != null) {
                showModule(item.name);
            }
        });

        String lastGroup = null;
        for (MenuItem item : items) {
            if (!item.group.equals(lastGroup)) {
                menuModel.addElement(GROUP_PREFIX + item.group + " ——");
                lastGroup = item.group;
            }
            menuModel.addElement(item.name);
            menuIndex.put(item.name, item);
        }
    }

    /**
     * 为每个菜单项建一张卡片；面板由 createPanel 分发，各成员只改自己的 case。
     */
    private void buildCards(List<MenuItem> items) {
        if (items.isEmpty()) {
            cardPanel.add(new PlaceholderPanel("无可用功能"), "无可用功能");
            return;
        }
        Set<String> names = new LinkedHashSet<>();
        for (MenuItem item : items) {
            names.add(item.name);
        }
        for (String name : names) {
            cardPanel.add(createPanel(name), name);
        }
    }

    /**
     * 模块面板工厂：按菜单名返回对应业务面板，未接入的落到占位面板。
     */
    private JPanel createPanel(String name) {
        switch (name) {
            // A · 排班号源
            case "排班管理": return new SchedulePanel();
            case "排班查询": return new ScheduleQueryPanel();

            // B · 挂号收费
            case "挂号": return new RegisterPanel();
            case "退号": return new RefundPanel();
            case "收费": return new ChargePanel();
            case "日结汇总": return new DailySummaryPanel();

            // C · 医生工作与统计报表
            case "今日挂号": return new DoctorWorkPanel();
            case "就诊确认": return new VisitPanel();
            case "挂号量统计": return new StatPanel(0);
            case "号源使用率": return new StatPanel(1);
            case "退号率": return new StatPanel(2);
            case "收入统计": return new StatPanel(3);

            // D · 基础资料
            case "患者管理": return new PatientPanel();
            case "科室管理": return new DeptPanel();
            case "医生管理": return new DoctorPanel();
            case "用户管理": return new UserPanel();

            default:
                return new PlaceholderPanel(name);
        }
    }

    /**
     * 默认选中第一个可点击的菜单项。
     */
    private void selectFirstModule() {
        for (int i = 0; i < menuModel.size(); i++) {
            if (!menuModel.get(i).startsWith(GROUP_PREFIX)) {
                menuList.setSelectedIndex(i);
                return;
            }
        }
        moduleTitle.setText("无可用功能");
        cardLayout.show(cardPanel, "无可用功能");
    }

    private void showModule(String name) {
        moduleTitle.setText(name);
        cardLayout.show(cardPanel, name);
    }

    private void logout() {
        Session.clear();
        new LoginFrame().setVisible(true);
        dispose();
    }

    /**
     * 角色文案：未登录时不显示角色标签。
     */
    private static String roleText() {
        String role = Session.getRole();
        return role == null || role.isEmpty() ? "" : role;
    }

    /**
     * 角色标识色：医生用成功色、挂号员用警告色，其余统一浅蓝。
     */
    private static Color roleColor(String role) {
        if (ROLE_DOCTOR.equals(role)) {
            return UiTheme.SUCCESS;
        }
        if (ROLE_REGISTRAR.equals(role)) {
            return UiTheme.WARNING;
        }
        return UiTheme.PRIMARY;
    }

    /**
     * 菜单渲染：分组行 12px 灰字，选中项浅蓝底 + 左侧 3px 主色竖条。
     */
    private static final class MenuRenderer extends JPanel implements ListCellRenderer<String> {

        private final JLabel text = new JLabel();
        private boolean selectedItem;

        private MenuRenderer() {
            super(new BorderLayout());
            setOpaque(true);
            text.setOpaque(false);
            add(text, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends String> list, String value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            String raw = value == null ? "" : value;
            boolean groupRow = raw.startsWith(GROUP_PREFIX);
            selectedItem = isSelected && !groupRow;

            if (groupRow) {
                String groupName = raw.substring(GROUP_PREFIX.length()).replace(" ——", "");
                text.setText(groupName);
                text.setFont(UiTheme.font(Font.PLAIN, 12));
                text.setForeground(UiTheme.TEXT_MUTED);
                text.setBorder(BorderFactory.createEmptyBorder(12, 16, 4, 8));
                setBackground(Color.WHITE);
            } else {
                text.setText(raw);
                text.setFont(UiTheme.font(Font.PLAIN, 14));
                text.setForeground(selectedItem ? UiTheme.PRIMARY : UiTheme.TEXT_PRIMARY);
                text.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 8));
                setBackground(selectedItem ? UiTheme.PRIMARY_SOFT : Color.WHITE);
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (!selectedItem) {
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(UiTheme.PRIMARY);
            g2.fillRect(0, 0, SELECTED_BAR_WIDTH, getHeight());
            g2.dispose();
        }
    }

    /**
     * 顶部角色标签：半透明白底圆角小胶囊。
     */
    private static final class RoleBadge extends JLabel {

        private final Color accent;

        private RoleBadge(String text, Color accent) {
            super(text);
            this.accent = accent;
            setOpaque(false);
            setFont(UiTheme.font(Font.PLAIN, 12));
            setForeground(Color.WHITE);
            setBorder(BorderFactory.createEmptyBorder(3, 20, 3, 10));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(255, 255, 255, 38));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.setColor(accent);
            g2.fillOval(10, getHeight() / 2 - 3, 6, 6);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /**
     * 菜单数据：所属分组 + 模块名称。
     */
    private static final class MenuItem {
        private final String group;
        private final String name;

        private MenuItem(String group, String name) {
            this.group = group;
            this.name = name;
        }
    }
}
