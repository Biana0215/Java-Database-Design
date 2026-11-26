package hospital.service;

import hospital.dao.DoctorDao;
import hospital.dao.ScheduleDao;
import hospital.dao.SlotDao;
import hospital.entity.Doctor;
import hospital.entity.Schedule;
import hospital.util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * 排班业务类：负责排班查询、排班与号源同步生成、停诊及号源停用，写操作保持同一事务。
 */
public class ScheduleService {

    private final ScheduleDao scheduleDao = new ScheduleDao();
    private final SlotDao slotDao = new SlotDao();
    private final DoctorDao doctorDao = new DoctorDao();

    /**
     * 查询指定科室、指定日期的排班。
     */
    public List<Schedule> listByDeptAndDate(int deptId, LocalDate date) {
        if (deptId <= 0) {
            throw new BusinessException("科室编号非法：" + deptId);
        }
        if (date == null) {
            throw new BusinessException("查询日期不能为空");
        }
        try (Connection conn = DBUtil.getConnection()) {
            return scheduleDao.findByDeptAndDate(conn, deptId, date);
        } catch (SQLException e) {
            throw new BusinessException("查询排班失败：" + e.getMessage(), e);
        }
    }

    /**
     * 查询指定医生的全部排班。
     */
    public List<Schedule> listByDoctor(int doctorId) {
        if (doctorId <= 0) {
            throw new BusinessException("医生编号非法：" + doctorId);
        }
        try (Connection conn = DBUtil.getConnection()) {
            return scheduleDao.findByDoctor(conn, doctorId);
        } catch (SQLException e) {
            throw new BusinessException("查询排班失败：" + e.getMessage(), e);
        }
    }

    /**
     * 新增排班并同步生成号源，返回排班编号；号源生成失败时排班一并回滚。
     */
    public int createSchedule(int doctorId, LocalDate date, String period, int slotTotal) {
        if (doctorId <= 0) {
            throw new BusinessException("医生编号非法：" + doctorId);
        }
        if (date == null) {
            throw new BusinessException("排班日期不能为空");
        }
        if (date.isBefore(LocalDate.now())) {
            throw new BusinessException("排班日期不能早于今天");
        }
        String periodValue = requirePeriod(period);
        if (slotTotal < 1 || slotTotal > 100) {
            throw new BusinessException("号源总数必须在 1~100 之间");
        }

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            Doctor doctor = doctorDao.findById(conn, doctorId);
            if (doctor == null) {
                throw new BusinessException("医生不存在");
            }
            if (!"在职".equals(doctor.getStatus())) {
                throw new BusinessException("该医生已离职，不能排班");
            }
            if (scheduleDao.existsByDoctorDatePeriod(conn, doctorId, date, periodValue)) {
                throw new BusinessException("该医生该时段已有排班");
            }

            Schedule schedule = new Schedule();
            schedule.setDoctorId(doctorId);
            schedule.setWorkDate(date);
            schedule.setPeriod(periodValue);
            schedule.setSlotTotal(slotTotal);
            schedule.setStatus("正常");

            int scheduleId = scheduleDao.insert(conn, schedule);
            if (scheduleId <= 0) {
                throw new SQLException("未取回新增排班的编号");
            }
            int inserted = slotDao.batchInsert(conn, scheduleId, slotTotal);
            if (inserted != slotTotal) {
                throw new SQLException("号源生成数量不正确，排班已回滚");
            }

            conn.commit();
            return scheduleId;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            if (isUniqueViolation(e)) {
                throw new BusinessException("该医生该时段已有排班", e);
            }
            throw new BusinessException("新增排班失败：" + e.getMessage(), e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * 停诊：排班改为“停诊”，并把该排班下所有“可挂”号源改为“停用”，返回停用号源数。
     */
    public int stopSchedule(int scheduleId) {
        if (scheduleId <= 0) {
            throw new BusinessException("排班编号非法：" + scheduleId);
        }

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            Schedule schedule = scheduleDao.findById(conn, scheduleId);
            if (schedule == null) {
                throw new BusinessException("排班不存在");
            }
            if ("停诊".equals(schedule.getStatus())) {
                throw new BusinessException("该排班已是停诊状态");
            }
            if (!scheduleDao.updateStatus(conn, scheduleId, "停诊")) {
                throw new BusinessException("停诊失败，排班状态已变更");
            }
            int disabled = slotDao.disableAvailableBySchedule(conn, scheduleId);

            conn.commit();
            return disabled;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw new BusinessException("停诊失败：" + e.getMessage(), e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * 校验出诊时段只能是“上午/下午”。
     */
    private String requirePeriod(String period) {
        String value = period == null ? "" : period.trim();
        if (value.isEmpty()) {
            throw new BusinessException("出诊时段不能为空");
        }
        if (!"上午".equals(value) && !"下午".equals(value)) {
            throw new BusinessException("时段值非法：" + value);
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
