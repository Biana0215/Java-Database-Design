package hospital.ui;

import hospital.entity.SysUser;
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
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 主窗体：顶部用户信息 + 左侧按角色生成的菜单 + 右侧 CardLayout 内容区。
 */
public class MainFrame extends JFrame {

    private static final String ROLE_ADMIN = "管理员";
    private static final String ROLE_REGISTRAR = "挂号员";
    private static final String ROLE_DOCTOR = "医生";

    private static final int MENU_WIDTH = 180;
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

        add(buildTopBar(), BorderLayout.NORTH);

        List<MenuItem> items = buildMenu(Session.getRole());
        fillMenu(items);
        buildCards(items);

        JScrollPane menuScroll = new JScrollPane(menuList);
        menuScroll.setPreferredSize(new Dimension(MENU_WIDTH, 0));
        menuScroll.setBorder(BorderFactory.createEmptyBorder());
        menuScroll.getVerticalScrollBar().setUnitIncrement(16);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, menuScroll, buildContentPanel());
        splitPane.setDividerLocation(MENU_WIDTH);
        splitPane.setDividerSize(6);
        splitPane.setContinuousLayout(true);
        splitPane.setBorder(BorderFactory.createEmptyBorder());
        add(splitPane, BorderLayout.CENTER);

        setSize(1100, 700);
        setMinimumSize(new Dimension(900, 600));
        setLocationRelativeTo(null);

        selectFirstModule();
    }

    /**
     * 顶部信息栏：左侧当前用户，右侧退出登录。
     */
    private JPanel buildTopBar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setPreferredSize(new Dimension(0, 40));
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(0, 12, 0, 12)));

        JLabel userLabel = new JLabel("当前用户：" + Session.getDisplayName());
        userLabel.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        bar.add(userLabel, BorderLayout.WEST);

        JButton logoutButton = new JButton("退出登录");
        logoutButton.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        logoutButton.addActionListener(e -> logout());
        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(logoutButton, BorderLayout.CENTER);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    /**
     * 右侧内容区：顶部模块标题 + 卡片面板。
     */
    private JPanel buildContentPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        moduleTitle.setFont(new Font("微软雅黑", Font.BOLD, 16));
        moduleTitle.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));
        panel.add(moduleTitle, BorderLayout.NORTH);
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

        menuList.setFont(new Font("微软雅黑", Font.PLAIN, 14));
        menuList.setFixedCellHeight(30);
        menuList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        menuList.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
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
