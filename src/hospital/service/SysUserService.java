package hospital.service;

import hospital.dao.SysUserDao;
import hospital.entity.SysUser;
import hospital.util.DBUtil;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 系统用户业务类：负责登录校验、用户维护与 SHA-256 密码摘要，登录失败提示不泄露账号是否存在。
 */
public class SysUserService {

    private final SysUserDao userDao = new SysUserDao();

    /**
     * 登录：校验账号状态与 SHA-256 密码摘要，成功后返回去掉密码摘要的用户对象。
     */
    public SysUser login(String username, String rawPassword) {
        String name = username == null ? "" : username.trim();
        if (name.isEmpty() || rawPassword == null || rawPassword.isEmpty()) {
            throw new BusinessException("用户名或密码错误");
        }

        try (Connection conn = DBUtil.getConnection()) {
            // 1. 用户不存在与密码错误使用同一提示，避免泄露账号信息
            SysUser user = userDao.findByUsername(conn, name);
            if (user == null) {
                throw new BusinessException("用户名或密码错误");
            }

            // 2. 停用账号单独提示
            if ("停用".equals(user.getStatus())) {
                throw new BusinessException("该账号已停用，请联系管理员");
            }

            // 3. 摘要比较忽略大小写
            String hash = sha256(rawPassword);
            if (user.getPwdHash() == null || !hash.equalsIgnoreCase(user.getPwdHash())) {
                throw new BusinessException("用户名或密码错误");
            }

            // 4. 不把密码摘要带出业务层
            user.setPwdHash(null);
            return user;
        } catch (SQLException e) {
            throw new BusinessException("登录失败：" + e.getMessage(), e);
        }
    }

    /**
     * 查询全部用户，返回前清空密码摘要。
     */
    public List<SysUser> listAll() {
        try (Connection conn = DBUtil.getConnection()) {
            List<SysUser> users = userDao.findAll(conn);
            for (SysUser user : users) {
                user.setPwdHash(null);
            }
            return users;
        } catch (SQLException e) {
            throw new BusinessException("查询用户失败：" + e.getMessage(), e);
        }
    }

    /**
     * 新增用户，返回用户编号；用户名重复时给出业务提示。
     */
    public int create(SysUser user, String rawPassword) {
        if (user == null) {
            throw new BusinessException("用户信息不能为空");
        }
        String username = requireText(user.getUsername(), "用户名不能为空");
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new BusinessException("初始密码不能为空");
        }
        String role = requireRole(user.getRole());
        String status = user.getStatus() == null || user.getStatus().trim().isEmpty()
                ? "启用" : requireStatus(user.getStatus());

        user.setUsername(username);
        user.setRole(role);
        user.setStatus(status);
        user.setPwdHash(sha256(rawPassword));

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            if (userDao.findByUsername(conn, username) != null) {
                throw new BusinessException("该用户名已存在");
            }
            int userId = userDao.insert(conn, user);
            if (userId <= 0) {
                throw new SQLException("未取回新增用户的编号");
            }

            conn.commit();
            user.setUserId(userId);
            return userId;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            if (isUniqueViolation(e)) {
                throw new BusinessException("该用户名已存在", e);
            }
            throw new BusinessException("新增用户失败：" + e.getMessage(), e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * 启用或停用账号，status 只允许“启用/停用”。
     */
    public void changeStatus(int userId, String status) {
        if (userId <= 0) {
            throw new BusinessException("用户编号非法：" + userId);
        }
        String value = requireStatus(status);

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            if (userDao.findById(conn, userId) == null) {
                throw new BusinessException("用户不存在");
            }
            if (!userDao.updateStatus(conn, userId, value)) {
                throw new BusinessException("用户状态修改失败，请刷新后重试");
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw new BusinessException("修改用户状态失败：" + e.getMessage(), e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * 重置密码，新密码以 SHA-256 摘要保存。
     */
    public void resetPassword(int userId, String rawPassword) {
        if (userId <= 0) {
            throw new BusinessException("用户编号非法：" + userId);
        }
        if (rawPassword == null || rawPassword.isEmpty()) {
            throw new BusinessException("新密码不能为空");
        }
        String hash = sha256(rawPassword);

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            if (userDao.findById(conn, userId) == null) {
                throw new BusinessException("用户不存在");
            }
            if (!userDao.updatePassword(conn, userId, hash)) {
                throw new BusinessException("密码重置失败，请刷新后重试");
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw new BusinessException("重置密码失败：" + e.getMessage(), e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * 计算 SHA-256 摘要，输出小写十六进制 64 位字符串。
     */
    private static String sha256(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : bytes) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException("密码加密失败：" + e.getMessage(), e);
        }
    }

    /**
     * 校验文本必填并去除首尾空白。
     */
    private String requireText(String value, String message) {
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.isEmpty()) {
            throw new BusinessException(message);
        }
        return trimmed;
    }

    /**
     * 校验用户角色枚举值。
     */
    private String requireRole(String role) {
        String value = role == null ? "" : role.trim();
        if (value.isEmpty()) {
            throw new BusinessException("用户角色不能为空");
        }
        if (!"挂号员".equals(value) && !"医生".equals(value) && !"管理员".equals(value)) {
            throw new BusinessException("角色值非法：" + value);
        }
        return value;
    }

    /**
     * 校验用户状态枚举值。
     */
    private String requireStatus(String status) {
        String value = status == null ? "" : status.trim();
        if (value.isEmpty()) {
            throw new BusinessException("状态值不能为空");
        }
        if (!"启用".equals(value) && !"停用".equals(value)) {
            throw new BusinessException("状态值非法：" + value);
        }
        return value;
    }

    /**
     * 判断是否为唯一约束冲突。
     */
    private static boolean isUniqueViolation(SQLException e) {
        return "23505".equals(e.getSQLState());
    }
}
