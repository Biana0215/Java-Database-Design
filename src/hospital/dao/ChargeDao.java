package hospital.dao;

import hospital.entity.Charge;
import hospital.util.DBUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

/**
 * 收费单数据访问类。
 * 本类不管理连接与事务，由 Service 传入 Connection。
 */
public class ChargeDao {

    /**
     * 新增收费单，使用 RETURNING charge_id 取回数据库生成的主键。
     * pay_time 传入 null 时用数据库 CURRENT_TIMESTAMP 兜底，避免覆盖列默认值。
     */
    public int insert(Connection conn, Charge charge) throws SQLException {
        String sql = "insert into charge (reg_id, amount, pay_method, pay_time, "
                + "refund_time, status, operator_id) "
                + "values (?, ?, ?, coalesce(?, current_timestamp), ?, ?, ?) "
                + "returning charge_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, charge.getRegId());
            ps.setBigDecimal(2, charge.getAmount());
            ps.setString(3, charge.getPayMethod());
            setNullableTimestamp(ps, 4, charge.getPayTime());
            setNullableTimestamp(ps, 5, charge.getRefundTime());
            ps.setString(6, charge.getStatus());
            if (charge.getOperatorId() == null) {
                ps.setNull(7, Types.INTEGER);
            } else {
                ps.setInt(7, charge.getOperatorId());
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * 按挂号单号查询收费单，不存在时返回 null。
     */
    public Charge findByRegId(Connection conn, int regId) throws SQLException {
        String sql = "select charge_id, reg_id, amount, pay_method, pay_time, "
                + "refund_time, status, operator_id from charge where reg_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, regId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * 条件退费：仅当 status='已收费' 时更新为 '已退费' 并记录退费时间；
     * 返回是否至少更新一行。
     */
    public boolean markRefunded(Connection conn, int chargeId, LocalDateTime refundTime)
            throws SQLException {
        String sql = "update charge set status='已退费', refund_time=? "
                + "where charge_id=? and status='已收费'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setNullableTimestamp(ps, 1, refundTime);
            ps.setInt(2, chargeId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 将当前结果行映射为 Charge 实体，金额与时间字段分别使用专用读取方法。
     */
    private Charge mapRow(ResultSet rs) throws SQLException {
        Charge charge = new Charge();
        charge.setChargeId(rs.getInt("charge_id"));
        charge.setRegId(rs.getInt("reg_id"));
        charge.setAmount(rs.getBigDecimal("amount"));
        charge.setPayMethod(rs.getString("pay_method"));
        charge.setPayTime(DBUtil.toLocalDateTime(rs, "pay_time"));
        charge.setRefundTime(DBUtil.toLocalDateTime(rs, "refund_time"));
        charge.setStatus(rs.getString("status"));
        int operatorId = rs.getInt("operator_id");
        if (!rs.wasNull()) {
            charge.setOperatorId(operatorId);
        }
        return charge;
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
