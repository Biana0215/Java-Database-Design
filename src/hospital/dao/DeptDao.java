package hospital.dao;

import hospital.entity.Dept;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 科室数据访问类。
 * 本类不管理连接与事务，由 Service 传入 Connection。
 */
public class DeptDao {

    /**
     * 查询全部科室，按 dept_id 升序返回。
     */
    public List<Dept> findAll(Connection conn) throws SQLException {
        String sql = "select dept_id, dept_name, dept_phone, location, status "
                + "from dept order by dept_id";
        List<Dept> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * 查询启用的科室，仅取 status='启用' 的记录，按 dept_id 升序返回。
     */
    public List<Dept> findEnabled(Connection conn) throws SQLException {
        String sql = "select dept_id, dept_name, dept_phone, location, status "
                + "from dept where status='启用' order by dept_id";
        List<Dept> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * 按主键查询科室，不存在时返回 null。
     */
    public Dept findById(Connection conn, int deptId) throws SQLException {
        String sql = "select dept_id, dept_name, dept_phone, location, status "
                + "from dept where dept_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deptId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * 按科室名称精确查询科室，不存在时返回 null。
     */
    public Dept findByName(Connection conn, String deptName) throws SQLException {
        String sql = "select dept_id, dept_name, dept_phone, location, status "
                + "from dept where dept_name=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, deptName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * 新增科室，使用 RETURNING dept_id 取回数据库生成的主键。
     */
    public int insert(Connection conn, Dept dept) throws SQLException {
        String sql = "insert into dept (dept_name, dept_phone, location, status) "
                + "values (?, ?, ?, ?) returning dept_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dept.getDeptName());
            ps.setString(2, dept.getDeptPhone());
            ps.setString(3, dept.getLocation());
            ps.setString(4, dept.getStatus());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * 按 dept_id 修改科室名称、电话和位置，返回是否至少更新一行。
     */
    public boolean update(Connection conn, Dept dept) throws SQLException {
        String sql = "update dept set dept_name=?, dept_phone=?, location=? where dept_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, dept.getDeptName());
            ps.setString(2, dept.getDeptPhone());
            ps.setString(3, dept.getLocation());
            ps.setInt(4, dept.getDeptId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 按 dept_id 修改科室状态，返回是否至少更新一行。
     */
    public boolean updateStatus(Connection conn, int deptId, String status) throws SQLException {
        String sql = "update dept set status=? where dept_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, deptId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 将当前结果行映射为 Dept 实体。
     */
    private Dept mapRow(ResultSet rs) throws SQLException {
        Dept dept = new Dept();
        dept.setDeptId(rs.getInt("dept_id"));
        dept.setDeptName(rs.getString("dept_name"));
        dept.setDeptPhone(rs.getString("dept_phone"));
        dept.setLocation(rs.getString("location"));
        dept.setStatus(rs.getString("status"));
        return dept;
    }
}
