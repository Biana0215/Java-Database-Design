package hospital.service;

import hospital.dao.ChargeDao;
import hospital.dao.DoctorDao;
import hospital.dao.PatientDao;
import hospital.dao.RegistrationDao;
import hospital.dao.ScheduleDao;
import hospital.dao.SlotDao;
import hospital.entity.Charge;
import hospital.entity.Doctor;
import hospital.entity.Registration;
import hospital.entity.Schedule;
import hospital.entity.Slot;
import hospital.util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 挂号业务类：负责挂号单查询、挂号占号与退号回滚，占号只依赖 markUsed 的条件更新结果。
 */
public class RegistrationService {

    private final RegistrationDao registrationDao = new RegistrationDao();
    private final PatientDao patientDao = new PatientDao();
    private final SlotDao slotDao = new SlotDao();
    private final ScheduleDao scheduleDao = new ScheduleDao();
    private final DoctorDao doctorDao = new DoctorDao();
    private final ChargeDao chargeDao = new ChargeDao();

    /**
     * 组合查询挂号单，条件均可为空。
     */
    public List<Registration> search(Integer patientId, LocalDate date,
                                     Integer doctorId, String status) {
        String statusValue = status == null ? null : status.trim();
        if (statusValue != null && statusValue.isEmpty()) {
            statusValue = null;
        }
        if (statusValue != null && !"已挂号".equals(statusValue)
                && !"已就诊".equals(statusValue) && !"已退号".equals(statusValue)) {
            throw new BusinessException("状态值非法：" + statusValue);
        }
        try (Connection conn = DBUtil.getConnection()) {
            return registrationDao.search(conn, patientId, date, doctorId, statusValue);
        } catch (SQLException e) {
            throw new BusinessException("查询挂号单失败：" + e.getMessage(), e);
        }
    }

    /**
     * 按编号查询挂号单，不存在时抛出业务异常。
     */
    public Registration get(int regId) {
        if (regId <= 0) {
            throw new BusinessException("挂号单编号非法：" + regId);
        }
        try (Connection conn = DBUtil.getConnection()) {
            Registration registration = registrationDao.findById(conn, regId);
            if (registration == null) {
                throw new BusinessException("挂号单不存在");
            }
            return registration;
        } catch (SQLException e) {
            throw new BusinessException("查询挂号单失败：" + e.getMessage(), e);
        }
    }

    /**
     * 查询指定医生、指定日期的挂号单。
     */
    public List<Registration> findByDoctorAndDate(int doctorId, LocalDate date) {
        if (doctorId <= 0) {
            throw new BusinessException("医生编号非法：" + doctorId);
        }
        if (date == null) {
            throw new BusinessException("查询日期不能为空");
        }
        try (Connection conn = DBUtil.getConnection()) {
            return registrationDao.findByDoctorAndDate(conn, doctorId, date);
        } catch (SQLException e) {
            throw new BusinessException("查询挂号单失败：" + e.getMessage(), e);
        }
    }

    /**
     * 挂号：按患者、号源、排班、医生顺序校验，用 markUsed 的条件更新原子占号，再插入挂号单。
     */
    public int register(int patientId, int slotId, int operatorId) {
        if (patientId <= 0) {
            throw new BusinessException("患者编号非法：" + patientId);
        }
        if (slotId <= 0) {
            throw new BusinessException("号源编号非法：" + slotId);
        }

        LocalDate today = LocalDate.now();
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // 1. 患者必须存在
            if (patientDao.findById(conn, patientId) == null) {
                throw new BusinessException("患者不存在");
            }

            // 2. 号源必须存在
            Slot slot = slotDao.findById(conn, slotId);
            if (slot == null) {
                throw new BusinessException("号源不存在");
            }

            // 3. 号源对应的排班必须存在
            Schedule schedule = scheduleDao.findById(conn, slot.getScheduleId());
            if (schedule == null) {
                throw new BusinessException("排班不存在");
            }

            // 4. 停诊排班不能挂号
            if ("停诊".equals(schedule.getStatus())) {
                throw new BusinessException("该医生已停诊");
            }

            // 5. 过期号源不能挂号
            if (schedule.getWorkDate() == null || schedule.getWorkDate().isBefore(today)) {
                throw new BusinessException("该号已过期，不能挂号");
            }

            // 6. 取医生职称与挂号费快照
            Doctor doctor = doctorDao.findById(conn, schedule.getDoctorId());
            if (doctor == null) {
                throw new BusinessException("医生不存在");
            }
            if (doctor.getRegFee() == null) {
                throw new BusinessException("该医生挂号费未配置，无法挂号");
            }

            // 7. 唯一的并发防线：仅当号源仍为“可挂”时才能占号
            if (!slotDao.markUsed(conn, slotId)) {
                throw new BusinessException("该号已被挂走，请重选");
            }

            // 8. 构造挂号单，挂号费取医生当前费用的快照
            Registration registration = new Registration();
            registration.setPatientId(patientId);
            registration.setSlotId(slotId);
            registration.setRegTime(LocalDateTime.now());
            registration.setRegType(toRegType(doctor.getTitle()));
            registration.setRegFee(doctor.getRegFee());
            registration.setStatus("已挂号");
            registration.setOperatorId(operatorId);

            // 9. 插入挂号单并提交
            int regId = registrationDao.insert(conn, registration);
            if (regId <= 0) {
                throw new SQLException("未取回新增挂号单的编号");
            }

            conn.commit();
            return regId;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            if (isUniqueViolation(e)) {
                throw new BusinessException("该号已被挂走，请重选", e);
            }
            throw new BusinessException("挂号失败：" + e.getMessage(), e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * 退号：释放号源、挂号单置为已退号、已收费单置为已退费，三步在同一事务内完成。
     */
    public void refund(int regId, int operatorId, String reason) {
        if (regId <= 0) {
            throw new BusinessException("挂号单编号非法：" + regId);
        }
        String refundReason = trimToNull(reason);
        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // 1. 挂号单必须存在
            Registration registration = registrationDao.findById(conn, regId);
            if (registration == null) {
                throw new BusinessException("挂号单不存在");
            }

            // 2. 只有“已挂号”可退
            if (!"已挂号".equals(registration.getStatus())) {
                throw new BusinessException("该挂号单状态为"
                        + statusText(registration.getStatus()) + "，不能退号");
            }

            // 3. 未过期的号都可以退：只有出诊日期早于今天才拒绝，今天及未来均可退
            Slot slot = slotDao.findById(conn, registration.getSlotId());
            if (slot == null) {
                throw new BusinessException("号源不存在");
            }
            Schedule schedule = scheduleDao.findById(conn, slot.getScheduleId());
            if (schedule == null) {
                throw new BusinessException("排班不存在");
            }
            if (schedule.getWorkDate() != null && schedule.getWorkDate().isBefore(today)) {
                throw new BusinessException("该号已过期，不能退号");
            }

            // 4. 释放号源
            if (!slotDao.markFree(conn, slot.getSlotId())) {
                throw new BusinessException("号源状态异常，无法释放");
            }

            // 5. 挂号单改为已退号；失败时由统一的 BusinessException 分支回滚已释放的号源
            if (!registrationDao.markRefunded(conn, regId, now, refundReason)) {
                throw new BusinessException("退号失败，状态已变更");
            }

            // 6. 已收费的收费单同步退费
            Charge charge = chargeDao.findByRegId(conn, regId);
            if (charge != null && "已收费".equals(charge.getStatus())) {
                if (!chargeDao.markRefunded(conn, charge.getChargeId(), now)) {
                    throw new BusinessException("退费失败，收费单状态已变更");
                }
            }

            // 7. 三步全部成功才提交
            conn.commit();
        } catch (BusinessException e) {
            // 业务校验失败时统一回滚，避免已释放号源或已改状态残留在未提交事务中
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw e;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw new BusinessException("退号失败：" + e.getMessage(), e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * 就诊确认：仅“已挂号”状态可改为“已就诊”。
     */
    public void markVisited(int regId) {
        if (regId <= 0) {
            throw new BusinessException("挂号单编号非法：" + regId);
        }

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            Registration registration = registrationDao.findById(conn, regId);
            if (registration == null) {
                throw new BusinessException("挂号单不存在");
            }
            if (!"已挂号".equals(registration.getStatus())) {
                throw new BusinessException("只有已挂号状态可标记就诊");
            }

            // 未来号不能提前标记就诊；今天及以前的号允许补标
            Slot slot = slotDao.findById(conn, registration.getSlotId());
            if (slot == null) {
                throw new BusinessException("号源或排班不存在");
            }
            Schedule schedule = scheduleDao.findById(conn, slot.getScheduleId());
            if (schedule == null) {
                throw new BusinessException("号源或排班不存在");
            }
            if (schedule.getWorkDate() != null && schedule.getWorkDate().isAfter(LocalDate.now())) {
                throw new BusinessException("该号尚未到就诊日期，不能标记就诊");
            }

            // 条件更新仍然保留，防止校验后状态被并发修改
            if (!registrationDao.markVisited(conn, regId)) {
                throw new BusinessException("只有已挂号状态可标记就诊");
            }

            conn.commit();
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            throw new BusinessException("就诊确认失败：" + e.getMessage(), e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * 按医生职称推导挂号类型：主任/副主任医师为专家，其余为普通。
     */
    private String toRegType(String title) {
        if ("主任医师".equals(title) || "副主任医师".equals(title)) {
            return "专家";
        }
        return "普通";
    }

    /**
     * 状态为空时用“未知”兜底，保证提示可读。
     */
    private String statusText(String status) {
        return status == null || status.trim().isEmpty() ? "未知" : status;
    }

    /**
     * 可空文本去空白，空串转 null。
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 判断是否为唯一约束冲突。
     */
    private static boolean isUniqueViolation(SQLException e) {
        return "23505".equals(e.getSQLState());
    }
}
