package hospital.dao;

import hospital.entity.SysUser;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * 系统用户数据访问类。
 * 本类不管理连接与事务，由 Service 传入 Connection。
 */
public class SysUserDao {

    /**
     * 按登录名查询用户，左连接 doctor 回填 doctor_name，不存在时返回 null。
     */
    public SysUser findByUsername(Connection conn, String username) throws SQLException {
        String sql = "select u.user_id, u.username, u.pwd_hash, u.role, u.doctor_id, "
                + "u.status, d.doctor_name "
                + "from sys_user u left join doctor d on u.doctor_id=d.doctor_id "
                + "where u.username=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * 按主键查询用户，左连接 doctor 回填 doctor_name，不存在时返回 null。
     */
    public SysUser findById(Connection conn, int userId) throws SQLException {
        String sql = "select u.user_id, u.username, u.pwd_hash, u.role, u.doctor_id, "
                + "u.status, d.doctor_name "
                + "from sys_user u left join doctor d on u.doctor_id=d.doctor_id "
                + "where u.user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * 查询全部用户，左连接 doctor 回填 doctor_name，按 user_id 升序返回。
     */
    public List<SysUser> findAll(Connection conn) throws SQLException {
        String sql = "select u.user_id, u.username, u.pwd_hash, u.role, u.doctor_id, "
                + "u.status, d.doctor_name "
                + "from sys_user u left join doctor d on u.doctor_id=d.doctor_id "
                + "order by u.user_id";
        List<SysUser> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * 新增系统用户，使用 RETURNING user_id 取回数据库生成的主键。
     */
    public int insert(Connection conn, SysUser user) throws SQLException {
        String sql = "insert into sys_user (username, pwd_hash, role, doctor_id, status) "
                + "values (?, ?, ?, ?, ?) returning user_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPwdHash());
            ps.setString(3, user.getRole());
            if (user.getDoctorId() == null) {
                ps.setNull(4, Types.INTEGER);
            } else {
                ps.setInt(4, user.getDoctorId());
            }
            ps.setString(5, user.getStatus());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * 按 user_id 修改用户状态（启用/停用），返回是否至少更新一行。
     */
    public boolean updateStatus(Connection conn, int userId, String status) throws SQLException {
        String sql = "update sys_user set status=? where user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 按 user_id 更新密码摘要，返回是否至少更新一行。
     */
    public boolean updatePassword(Connection conn, int userId, String pwdHash) throws SQLException {
        String sql = "update sys_user set pwd_hash=? where user_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pwdHash);
            ps.setInt(2, userId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 将当前结果行映射为 SysUser 实体，并回填左连接取得的 doctor_name。
     */
    private SysUser mapRow(ResultSet rs) throws SQLException {
        SysUser user = new SysUser();
        user.setUserId(rs.getInt("user_id"));
        user.setUsername(rs.getString("username"));
        user.setPwdHash(rs.getString("pwd_hash"));
        user.setRole(rs.getString("role"));
        int doctorId = rs.getInt("doctor_id");
        if (!rs.wasNull()) {
            user.setDoctorId(doctorId);
        }
        user.setStatus(rs.getString("status"));
        user.setDoctorName(rs.getString("doctor_name"));
        return user;
    }
}
