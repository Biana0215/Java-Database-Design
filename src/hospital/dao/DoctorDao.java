package hospital.dao;

import hospital.entity.Doctor;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 医生数据访问类。
 * 本类不管理连接与事务，由 Service 传入 Connection。
 */
public class DoctorDao {

    /**
     * 查询全部医生，联合 dept 回填 dept_name，按 doctor_id 升序返回。
     */
    public List<Doctor> findAll(Connection conn) throws SQLException {
        String sql = "select d.doctor_id, d.doctor_name, d.dept_id, d.title, "
                + "d.reg_fee, d.phone, d.status, dp.dept_name "
                + "from doctor d join dept dp on d.dept_id=dp.dept_id "
                + "order by d.doctor_id";
        List<Doctor> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * 查询指定科室下状态为“在职”的医生，按职称、doctor_id 排序返回。
     */
    public List<Doctor> findActiveByDept(Connection conn, int deptId) throws SQLException {
        String sql = "select d.doctor_id, d.doctor_name, d.dept_id, d.title, "
                + "d.reg_fee, d.phone, d.status, dp.dept_name "
                + "from doctor d join dept dp on d.dept_id=dp.dept_id "
                + "where d.dept_id=? and d.status='在职' "
                + "order by d.title, d.doctor_id";
        List<Doctor> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deptId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * 按主键查询医生，联合 dept 回填 dept_name，不存在时返回 null。
     */
    public Doctor findById(Connection conn, int doctorId) throws SQLException {
        String sql = "select d.doctor_id, d.doctor_name, d.dept_id, d.title, "
                + "d.reg_fee, d.phone, d.status, dp.dept_name "
                + "from doctor d join dept dp on d.dept_id=dp.dept_id "
                + "where d.doctor_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * 新增医生，使用 RETURNING doctor_id 取回数据库生成的主键。
     */
    public int insert(Connection conn, Doctor doctor) throws SQLException {
        String sql = "insert into doctor (doctor_name, dept_id, title, reg_fee, phone, status) "
                + "values (?, ?, ?, ?, ?, ?) returning doctor_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doctor.getDoctorName());
            ps.setInt(2, doctor.getDeptId());
            ps.setString(3, doctor.getTitle());
            ps.setBigDecimal(4, doctor.getRegFee());
            ps.setString(5, doctor.getPhone());
            ps.setString(6, doctor.getStatus());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * 按 doctor_id 修改医生姓名、科室、职称、挂号费、电话和状态，返回是否至少更新一行。
     */
    public boolean update(Connection conn, Doctor doctor) throws SQLException {
        String sql = "update doctor set doctor_name=?, dept_id=?, title=?, "
                + "reg_fee=?, phone=?, status=? where doctor_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, doctor.getDoctorName());
            ps.setInt(2, doctor.getDeptId());
            ps.setString(3, doctor.getTitle());
            ps.setBigDecimal(4, doctor.getRegFee());
            ps.setString(5, doctor.getPhone());
            ps.setString(6, doctor.getStatus());
            ps.setInt(7, doctor.getDoctorId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 按 doctor_id 修改医生状态（在职/离职），返回是否至少更新一行。
     */
    public boolean updateStatus(Connection conn, int doctorId, String status) throws SQLException {
        String sql = "update doctor set status=? where doctor_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, doctorId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 将当前结果行映射为 Doctor 实体，并回填联表取得的 dept_name。
     */
    private Doctor mapRow(ResultSet rs) throws SQLException {
        Doctor doctor = new Doctor();
        doctor.setDoctorId(rs.getInt("doctor_id"));
        doctor.setDoctorName(rs.getString("doctor_name"));
        doctor.setDeptId(rs.getInt("dept_id"));
        doctor.setTitle(rs.getString("title"));
        doctor.setRegFee(rs.getBigDecimal("reg_fee"));
        doctor.setPhone(rs.getString("phone"));
        doctor.setStatus(rs.getString("status"));
        doctor.setDeptName(rs.getString("dept_name"));
        return doctor;
    }
}
