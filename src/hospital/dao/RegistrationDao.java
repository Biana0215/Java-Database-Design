package hospital.dao;

import hospital.entity.Registration;
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
 * 挂号单数据访问类。
 * 本类不管理连接与事务，由 Service 传入 Connection。
 */
public class RegistrationDao {

    // 挂号单查询公共字段与联表关系，用于回填患者姓名、医生姓名
    private static final String SELECT_COLUMNS =
            "select r.reg_id, r.patient_id, r.slot_id, r.reg_time, r.reg_type, "
                    + "r.reg_fee, r.status, r.operator_id, r.refund_time, r.refund_reason, "
                    + "p.patient_name, d.doctor_name "
                    + "from registration r "
                    + "join patient p on r.patient_id=p.patient_id "
                    + "join slot sl on r.slot_id=sl.slot_id "
                    + "join schedule s on sl.schedule_id=s.schedule_id "
                    + "join doctor d on s.doctor_id=d.doctor_id";

    /**
     * 新增挂号单，使用 RETURNING reg_id 取回数据库生成的主键。
     * reg_time 传入 null 时用数据库 CURRENT_TIMESTAMP 兜底，避免覆盖列默认值。
     */
    public int insert(Connection conn, Registration registration) throws SQLException {
        String sql = "insert into registration (patient_id, slot_id, reg_time, reg_type, "
                + "reg_fee, status, operator_id, refund_time, refund_reason) "
                + "values (?, ?, coalesce(?, current_timestamp), ?, ?, ?, ?, ?, ?) "
                + "returning reg_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, registration.getPatientId());
            ps.setInt(2, registration.getSlotId());
            setNullableTimestamp(ps, 3, registration.getRegTime());
            ps.setString(4, registration.getRegType());
            ps.setBigDecimal(5, registration.getRegFee());
            ps.setString(6, registration.getStatus());
            if (registration.getOperatorId() == null) {
                ps.setNull(7, Types.INTEGER);
            } else {
                ps.setInt(7, registration.getOperatorId());
            }
            setNullableTimestamp(ps, 8, registration.getRefundTime());
            ps.setString(9, registration.getRefundReason());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * 按主键查询挂号单，联表回填 patient_name、doctor_name，不存在时返回 null。
     */
    public Registration findById(Connection conn, int regId) throws SQLException {
        String sql = SELECT_COLUMNS + " where r.reg_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, regId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * 组合查询挂号单，患者、日期、医生、状态条件均可为 null；
     * 日期条件使用 reg_time >= 当天 AND reg_time < 次日，避免对时间列 CAST；
     * 联表回填 patient_name、doctor_name，按 reg_time 倒序最多返回 500 条。
     */
    public List<Registration> search(Connection conn, Integer patientId, LocalDate date,
                                     Integer doctorId, String status) throws SQLException {
        StringBuilder sql = new StringBuilder(SELECT_COLUMNS).append(" where 1=1");
        List<Object> params = new ArrayList<>();
        if (patientId != null) {
            sql.append(" and r.patient_id=?");
            params.add(patientId);
        }
        if (date != null) {
            sql.append(" and r.reg_time >= ? and r.reg_time < ?");
            params.add(Date.valueOf(date));
            params.add(Date.valueOf(date.plusDays(1)));
        }
        if (doctorId != null) {
            sql.append(" and s.doctor_id=?");
            params.add(doctorId);
        }
        if (status != null && !status.trim().isEmpty()) {
            sql.append(" and r.status=?");
            params.add(status.trim());
        }
        sql.append(" order by r.reg_time desc limit 500");

        List<Registration> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            bindParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * 查询指定医生、指定工作日的挂号单，联表回填展示字段，按挂号时间排序返回。
     */
    public List<Registration> findByDoctorAndDate(Connection conn, int doctorId, LocalDate date)
            throws SQLException {
        String sql = SELECT_COLUMNS
                + " where s.doctor_id=? and s.work_date=? order by r.reg_time";
        List<Registration> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setDate(2, Date.valueOf(date));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * 条件退号：仅当 status='已挂号' 时更新为 '已退号' 并记录退号时间、原因；
     * 返回是否至少更新一行，false 表示状态已变化或单号不存在。
     */
    public boolean markRefunded(Connection conn, int regId,
                                LocalDateTime refundTime, String reason) throws SQLException {
        String sql = "update registration set status='已退号', refund_time=?, "
                + "refund_reason=? where reg_id=? and status='已挂号'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setNullableTimestamp(ps, 1, refundTime);
            ps.setString(2, reason);
            ps.setInt(3, regId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 条件就诊确认：仅当 status='已挂号' 时更新为 '已就诊'，返回是否至少更新一行。
     */
    public boolean markVisited(Connection conn, int regId) throws SQLException {
        String sql = "update registration set status='已就诊' "
                + "where reg_id=? and status='已挂号'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, regId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 将当前结果行映射为 Registration 实体，并回填患者姓名、医生姓名。
     */
    private Registration mapRow(ResultSet rs) throws SQLException {
        Registration registration = new Registration();
        registration.setRegId(rs.getInt("reg_id"));
        registration.setPatientId(rs.getInt("patient_id"));
        registration.setSlotId(rs.getInt("slot_id"));
        registration.setRegTime(DBUtil.toLocalDateTime(rs, "reg_time"));
        registration.setRegType(rs.getString("reg_type"));
        registration.setRegFee(rs.getBigDecimal("reg_fee"));
        registration.setStatus(rs.getString("status"));
        int operatorId = rs.getInt("operator_id");
        if (!rs.wasNull()) {
            registration.setOperatorId(operatorId);
        }
        registration.setRefundTime(DBUtil.toLocalDateTime(rs, "refund_time"));
        registration.setRefundReason(rs.getString("refund_reason"));
        registration.setPatientName(rs.getString("patient_name"));
        registration.setDoctorName(rs.getString("doctor_name"));
        return registration;
    }

    /**
     * 按参数实际类型依次绑定动态条件中的占位符。
     */
    private void bindParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object value = params.get(i);
            int index = i + 1;
            if (value instanceof Integer intValue) {
                ps.setInt(index, intValue);
            } else if (value instanceof String stringValue) {
                ps.setString(index, stringValue);
            } else if (value instanceof Date dateValue) {
                ps.setDate(index, dateValue);
            } else {
                ps.setObject(index, value);
            }
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
