package hospital.dao;

import hospital.entity.Schedule;
import hospital.util.DBUtil;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 排班数据访问类。
 * 本类不管理连接与事务，由 Service 传入 Connection。
 */
public class ScheduleDao {

    /**
     * 查询指定科室、指定工作日的排班，联合 doctor 回填 doctor_name；
     * 按上午在前、doctor_id 升序返回。
     */
    public List<Schedule> findByDeptAndDate(Connection conn, int deptId, LocalDate date)
            throws SQLException {
        String sql = "select s.schedule_id, s.doctor_id, s.work_date, s.period, "
                + "s.slot_total, s.status, d.doctor_name "
                + "from schedule s join doctor d on s.doctor_id=d.doctor_id "
                + "where d.dept_id=? and s.work_date=? "
                + "order by case when s.period='上午' then 0 else 1 end, s.doctor_id";
        List<Schedule> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, deptId);
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
     * 查询指定医生的全部排班，回填 doctor_name，按 work_date 倒序返回。
     */
    public List<Schedule> findByDoctor(Connection conn, int doctorId) throws SQLException {
        String sql = "select s.schedule_id, s.doctor_id, s.work_date, s.period, "
                + "s.slot_total, s.status, d.doctor_name "
                + "from schedule s join doctor d on s.doctor_id=d.doctor_id "
                + "where s.doctor_id=? "
                + "order by s.work_date desc, case when s.period='上午' then 0 else 1 end";
        List<Schedule> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * 按主键查询排班，回填 doctor_name，不存在时返回 null。
     */
    public Schedule findById(Connection conn, int scheduleId) throws SQLException {
        String sql = "select s.schedule_id, s.doctor_id, s.work_date, s.period, "
                + "s.slot_total, s.status, d.doctor_name "
                + "from schedule s join doctor d on s.doctor_id=d.doctor_id "
                + "where s.schedule_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * 判断某医生在某天某时段是否已有排班，用于配合唯一约束提前提示。
     */
    public boolean existsByDoctorDatePeriod(Connection conn, int doctorId,
                                            LocalDate date, String period) throws SQLException {
        String sql = "select 1 from schedule "
                + "where doctor_id=? and work_date=? and period=? limit 1";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, doctorId);
            ps.setDate(2, Date.valueOf(date));
            ps.setString(3, period);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * 新增排班，使用 RETURNING schedule_id 取回数据库生成的主键。
     */
    public int insert(Connection conn, Schedule schedule) throws SQLException {
        String sql = "insert into schedule (doctor_id, work_date, period, slot_total, status) "
                + "values (?, ?, ?, ?, ?) returning schedule_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, schedule.getDoctorId());
            if (schedule.getWorkDate() == null) {
                ps.setNull(2, Types.DATE);
            } else {
                ps.setDate(2, Date.valueOf(schedule.getWorkDate()));
            }
            ps.setString(3, schedule.getPeriod());
            ps.setInt(4, schedule.getSlotTotal());
            ps.setString(5, schedule.getStatus());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * 按 schedule_id 修改排班状态（正常/停诊），返回是否至少更新一行。
     */
    public boolean updateStatus(Connection conn, int scheduleId, String status) throws SQLException {
        String sql = "update schedule set status=? where schedule_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, scheduleId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 将当前结果行映射为 Schedule 实体，并回填联表取得的 doctor_name。
     */
    private Schedule mapRow(ResultSet rs) throws SQLException {
        Schedule schedule = new Schedule();
        schedule.setScheduleId(rs.getInt("schedule_id"));
        schedule.setDoctorId(rs.getInt("doctor_id"));
        schedule.setWorkDate(DBUtil.toLocalDate(rs, "work_date"));
        schedule.setPeriod(rs.getString("period"));
        schedule.setSlotTotal(rs.getInt("slot_total"));
        schedule.setStatus(rs.getString("status"));
        schedule.setDoctorName(rs.getString("doctor_name"));
        return schedule;
    }
}
