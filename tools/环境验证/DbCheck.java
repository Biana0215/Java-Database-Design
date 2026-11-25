import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * 课设环境自检工具：一次跑完驱动 / 连接 / 建表 / 事务 / 约束 / 中文 六项检查。
 *
 * 用法（Windows 命令行，注意 -cp 用分号分隔）：
 *   javac -encoding UTF-8 -cp ".;opengauss-jdbc.jar" DbCheck.java
 *   java  -cp ".;." DbCheck db-opengauss.properties
 *
 * 在 IDEA 里直接右键 Run 也行，把配置文件路径填进 Program arguments。
 *
 * 注意：本类只会在目标库里建一张临时表 env_check，跑完自动删除，不会碰你已有的表。
 */
public class DbCheck {

    private static final String TMP_TABLE = "env_check";

    public static void main(String[] args) {
        String cfgFile = args.length > 0 ? args[0] : "db-opengauss.properties";
        Properties cfg = new Properties();
        try (Reader r = new InputStreamReader(new FileInputStream(cfgFile), StandardCharsets.UTF_8)) {
            cfg.load(r);
        } catch (Exception e) {
            System.out.println("[FAIL] 读不到配置文件 " + cfgFile + " : " + e.getMessage());
            return;
        }

        String driver   = cfg.getProperty("driver", "").trim();
        String url      = cfg.getProperty("url", "").trim();
        String user     = cfg.getProperty("user", "").trim();
        String password = cfg.getProperty("password", "");

        System.out.println("配置文件 : " + cfgFile);
        System.out.println("驱动类   : " + driver);
        System.out.println("URL      : " + url);
        System.out.println("用户     : " + user);
        System.out.println("------------------------------------------------------------");

        // ---- 1. 驱动 ----
        try {
            Class.forName(driver);
            System.out.println("[OK]   1. 驱动加载成功");
        } catch (ClassNotFoundException e) {
            System.out.println("[FAIL] 1. 找不到驱动类 " + driver);
            System.out.println("       把驱动 jar 加进 classpath / IDEA 的 Libraries 后再试");
            return;
        }

        // ---- 2. 连接 ----
        Connection conn = null;
        try {
            DriverManager.setLoginTimeout(8);
            conn = DriverManager.getConnection(url, user, password);
            DatabaseMetaData md = conn.getMetaData();
            System.out.println("[OK]   2. 连接成功");
            System.out.println("       数据库 : " + md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            System.out.println("       驱动   : " + md.getDriverName() + " " + md.getDriverVersion());
        } catch (SQLException e) {
            System.out.println("[FAIL] 2. 连接失败 : " + e.getMessage());
            System.out.println("       SQLState=" + e.getSQLState() + " errorCode=" + e.getErrorCode());
            System.out.println("       排查看 README.md 的「常见报错对照表」");
            return;
        }

        try {
            // ---- 3. 版本查询（验证能执行 SQL、能读结果集）----
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT version()")) {
                if (rs.next()) {
                    String v = rs.getString(1);
                    System.out.println("[OK]   3. 执行查询成功，服务端版本首行：");
                    System.out.println("       " + (v.length() > 90 ? v.substring(0, 90) + "..." : v));
                }
            } catch (SQLException e) {
                System.out.println("[FAIL] 3. 查询失败 : " + e.getMessage());
            }

            // ---- 4. 建临时表 ----
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS " + TMP_TABLE
                        + " (id INT PRIMARY KEY, note VARCHAR(50))");
                st.executeUpdate("DELETE FROM " + TMP_TABLE);
                System.out.println("[OK]   4. 建表 / 清空成功（" + TMP_TABLE + "）");
            } catch (SQLException e) {
                System.out.println("[FAIL] 4. 建表失败 : " + e.getMessage());
                System.out.println("       如果是权限问题，换成你有建表权限的库/模式再试");
                return;
            }

            // ---- 5. 事务回滚 ----
            try {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO " + TMP_TABLE + " (id, note) VALUES (?, ?)")) {
                    ps.setInt(1, 901); ps.setString(2, "rollback-A"); ps.executeUpdate();
                    ps.setInt(1, 902); ps.setString(2, "rollback-B"); ps.executeUpdate();
                }
                conn.rollback();
                conn.setAutoCommit(true);
                int n = count(conn);
                System.out.println((n == 0 ? "[OK]   5. 事务回滚生效，回滚后剩 " + n + " 行"
                                           : "[FAIL] 5. 回滚后仍有 " + n + " 行，事务没生效"));
            } catch (SQLException e) {
                System.out.println("[FAIL] 5. 事务测试出错 : " + e.getMessage());
                try { conn.setAutoCommit(true); } catch (SQLException ignore) { }
            }

            // ---- 6. 事务提交 + 中文写入 ----
            try {
                conn.setAutoCommit(false);
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO " + TMP_TABLE + " (id, note) VALUES (?, ?)")) {
                    ps.setInt(1, 903); ps.setString(2, "中文提交测试");
                    ps.executeUpdate();
                }
                conn.commit();
                conn.setAutoCommit(true);

                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT note FROM " + TMP_TABLE + " WHERE id = 903")) {
                    String note = rs.next() ? rs.getString(1) : null;
                    boolean ok = "中文提交测试".equals(note);
                    System.out.println((ok ? "[OK]   6. 提交成功且中文无乱码"
                                           : "[FAIL] 6. 读回的内容是「" + note + "」，中文可能乱码"));
                }
            } catch (SQLException e) {
                System.out.println("[FAIL] 6. 提交测试出错 : " + e.getMessage());
                try { conn.setAutoCommit(true); } catch (SQLException ignore) { }
            }

            // ---- 7. 主键约束（课设里"订房冲突/号源重复"就靠它兜底）----
            try (PreparedStatement ps = conn.prepareStatement(
                    "INSERT INTO " + TMP_TABLE + " (id, note) VALUES (?, ?)")) {
                ps.setInt(1, 903); ps.setString(2, "重复主键");
                ps.executeUpdate();
                System.out.println("[FAIL] 7. 重复主键竟然插入成功，约束没生效");
            } catch (SQLException e) {
                System.out.println("[OK]   7. 主键唯一约束生效，重复插入被拦下");
                System.out.println("       SQLState=" + e.getSQLState() + " errorCode=" + e.getErrorCode());
                System.out.println("       （代码里据此判断重复预约，不要靠先查后插）");
            }

            System.out.println("------------------------------------------------------------");
            System.out.println("自检结束。上面 7 项如果全是 [OK]，你的 JDBC 环境可以直接开工。");

        } finally {
            // 清理：删掉临时表，不留痕迹
            try (Statement st = conn.createStatement()) {
                st.executeUpdate("DROP TABLE IF EXISTS " + TMP_TABLE);
            } catch (SQLException ignore) { }
            try { conn.close(); } catch (SQLException ignore) { }
        }
    }

    private static int count(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + TMP_TABLE)) {
            return rs.next() ? rs.getInt(1) : -1;
        }
    }
}
