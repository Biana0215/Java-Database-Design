package hospital.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Properties;

/**
 * 数据库工具类，只负责取得连接与释放资源。
 * 事务由 Service 层通过 conn.setAutoCommit(false) 控制。
 */
public final class DBUtil {

    private static final String DEFAULT_DRIVER = "org.opengauss.Driver";
    private static final Properties CONFIG;
    private static final RuntimeException CONFIG_ERROR;

    static {
        // 先读配置，再按 db.properties 中的 driver 加载驱动；配置缺失时留到取连接时报告。
        Properties loaded = null;
        RuntimeException error = null;
        try {
            loaded = loadConfig();
        } catch (RuntimeException e) {
            error = e;
        }
        CONFIG = loaded;
        CONFIG_ERROR = error;

        String driver = DEFAULT_DRIVER;
        if (loaded != null) {
            String configuredDriver = loaded.getProperty("driver");
            if (configuredDriver != null && !configuredDriver.trim().isEmpty()) {
                driver = configuredDriver.trim();
            }
        }
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(
                    "加载 openGauss JDBC 驱动失败：未找到 " + driver
                            + "，请确认 lib/opengauss-jdbc-5.1.0-og.jar 已加入项目 Libraries。", e);
        }
    }

    private DBUtil() {
    }

    private static Properties loadConfig() {
        // 1. 先读 classpath 下的 /db.properties（IDEA 标记 src 为 Sources Root 时可直接生效）
        Properties props = new Properties();
        try (InputStream in = DBUtil.class.getResourceAsStream("/db.properties")) {
            if (in != null) {
                try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                    props.load(reader);
                }
                return props;
            }
        } catch (IOException e) {
            throw new RuntimeException("读取 classpath 下的 db.properties 失败：" + e.getMessage(), e);
        }

        // 2. 再读当前工作目录下的 db.properties
        Path path = Paths.get("db.properties");
        if (Files.isRegularFile(path)) {
            try (InputStream in = Files.newInputStream(path);
                 Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                props.load(reader);
                return props;
            } catch (IOException e) {
                throw new RuntimeException(
                        "读取工作目录下的 db.properties 失败：" + path.toAbsolutePath(), e);
            }
        }

        // 3. 两处都没有
        throw new RuntimeException("未找到 db.properties，请复制 src/db.properties.example 并填写密码");
    }

    /**
     * 从 db.properties 读取连接信息并取得数据库连接。
     */
    public static Connection getConnection() throws SQLException {
        if (CONFIG_ERROR != null) {
            throw CONFIG_ERROR;
        }
        String url = required(CONFIG, "url");
        String user = required(CONFIG, "user");
        String password = CONFIG.getProperty("password", "").trim();
        if (password.isEmpty()) {
            throw new SQLException("db.properties 中的 password 未填写，请编辑 src/db.properties");
        }
        return DriverManager.getConnection(url, user, password);
    }

    private static String required(Properties props, String key) throws SQLException {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new SQLException("db.properties 缺少 " + key + " 配置");
        }
        return value.trim();
    }

    /**
     * 静默关闭结果集、语句和连接，参数允许为 null。
     */
    public static void close(ResultSet rs, Statement st, Connection conn) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ignored) {
                // 关闭资源时的异常不向外抛出
            }
        }
        if (st != null) {
            try {
                st.close();
            } catch (SQLException ignored) {
                // 关闭资源时的异常不向外抛出
            }
        }
        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException ignored) {
                // 关闭资源时的异常不向外抛出
            }
        }
    }

    /**
     * 静默关闭连接，参数允许为 null。
     */
    public static void close(Connection conn) {
        close(null, null, conn);
    }

    /**
     * 把结果集中的 DATE 列转换为 LocalDate，空值返回 null。
     */
    public static LocalDate toLocalDate(ResultSet rs, String column) throws SQLException {
        java.sql.Date date = rs.getDate(column);
        return date == null ? null : date.toLocalDate();
    }

    /**
     * 把结果集中的 TIMESTAMP 列转换为 LocalDateTime，空值返回 null。
     */
    public static LocalDateTime toLocalDateTime(ResultSet rs, String column) throws SQLException {
        java.sql.Timestamp timestamp = rs.getTimestamp(column);
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    /**
     * 连接自检入口：连接一次数据库，打印产品名、版本和当前库名后关闭。
     */
    public static void main(String[] args) {
        try (Connection conn = getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println("数据库产品名：" + meta.getDatabaseProductName());
            System.out.println("数据库版本：" + meta.getDatabaseProductVersion());
            String catalog = conn.getCatalog();
            System.out.println("当前库名：" + (catalog == null || catalog.isBlank() ? "未获取到" : catalog));
            System.out.println("数据库连接自检成功。");
        } catch (Exception e) {
            System.err.println("数据库连接自检失败：" + e.getMessage());
            System.err.println("常见原因：");
            System.err.println("1. 驱动未加入 Libraries：lib/opengauss-jdbc-5.1.0-og.jar 未加入项目依赖。");
            System.err.println("2. db.properties 缺失：请复制 src/db.properties.example 并填写密码。");
            System.err.println("3. 网络或密码错误：数据库地址、端口不可达，或用户名/密码不正确。");
        }
    }
}
