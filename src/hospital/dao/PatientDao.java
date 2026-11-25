package hospital.dao;

import hospital.entity.Patient;
import hospital.util.DBUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 患者数据访问类。
 * 本类不管理连接与事务，由 Service 传入 Connection。
 */
public class PatientDao {

    /**
     * 按主键查询患者，不存在时返回 null。
     */
    public Patient findById(Connection conn, int patientId) throws SQLException {
        String sql = "select patient_id, patient_name, gender, birth_date, id_card, "
                + "phone, address, create_time from patient where patient_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, patientId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * 按身份证号精确查询患者，不存在时返回 null。
     */
    public Patient findByIdCard(Connection conn, String idCard) throws SQLException {
        String sql = "select patient_id, patient_name, gender, birth_date, id_card, "
                + "phone, address, create_time from patient where id_card=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idCard);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * 按姓名、证件号或电话模糊查询患者，按 patient_id 升序最多返回 200 条；
     * keyword 为 null 或空白时返回前 200 条。
     */
    public List<Patient> search(Connection conn, String keyword) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "select patient_id, patient_name, gender, birth_date, id_card, "
                        + "phone, address, create_time from patient");
        List<Object> params = new ArrayList<>();
        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append(" where (patient_name like ? or id_card like ? or phone like ?)");
            String likeValue = "%" + keyword.trim() + "%";
            params.add(likeValue);
            params.add(likeValue);
            params.add(likeValue);
        }
        sql.append(" order by patient_id limit 200");

        List<Patient> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setString(i + 1, (String) params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * 新增患者，使用 RETURNING patient_id 取回数据库生成的主键。
     * create_time 传入 null 时用数据库 CURRENT_TIMESTAMP 兜底，避免覆盖列默认值。
     */
    public int insert(Connection conn, Patient patient) throws SQLException {
        String sql = "insert into patient (patient_name, gender, birth_date, id_card, "
                + "phone, address, create_time) "
                + "values (?, ?, ?, ?, ?, ?, coalesce(?, current_timestamp)) "
                + "returning patient_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patient.getPatientName());
            ps.setString(2, patient.getGender());
            setNullableDate(ps, 3, patient.getBirthDate());
            ps.setString(4, patient.getIdCard());
            ps.setString(5, patient.getPhone());
            ps.setString(6, patient.getAddress());
            setNullableTimestamp(ps, 7, patient.getCreateTime());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * 按 patient_id 修改姓名、性别、出生日期、电话和地址，返回是否至少更新一行。
     */
    public boolean update(Connection conn, Patient patient) throws SQLException {
        String sql = "update patient set patient_name=?, gender=?, birth_date=?, "
                + "phone=?, address=? where patient_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, patient.getPatientName());
            ps.setString(2, patient.getGender());
            setNullableDate(ps, 3, patient.getBirthDate());
            ps.setString(4, patient.getPhone());
            ps.setString(5, patient.getAddress());
            ps.setInt(6, patient.getPatientId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 将当前结果行映射为 Patient 实体，日期字段通过 DBUtil 转换。
     */
    private Patient mapRow(ResultSet rs) throws SQLException {
        Patient patient = new Patient();
        patient.setPatientId(rs.getInt("patient_id"));
        patient.setPatientName(rs.getString("patient_name"));
        patient.setGender(rs.getString("gender"));
        patient.setBirthDate(DBUtil.toLocalDate(rs, "birth_date"));
        patient.setIdCard(rs.getString("id_card"));
        patient.setPhone(rs.getString("phone"));
        patient.setAddress(rs.getString("address"));
        patient.setCreateTime(DBUtil.toLocalDateTime(rs, "create_time"));
        return patient;
    }

    /**
     * 设置可空的 DATE 参数，null 时写入 SQL NULL。
     */
    private void setNullableDate(PreparedStatement ps, int index, LocalDate value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.DATE);
        } else {
            ps.setDate(index, Date.valueOf(value));
        }
    }

    /**
     * 设置可空的 TIMESTAMP 参数，null 时写入 SQL NULL。
     */
    private void setNullableTimestamp(PreparedStatement ps, int index, LocalDateTime value)
            throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(index, Timestamp.valueOf(value));
        }
    }
}
