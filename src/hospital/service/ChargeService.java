package hospital.service;

import hospital.dao.ChargeDao;
import hospital.dao.RegistrationDao;
import hospital.dao.ScheduleDao;
import hospital.dao.SlotDao;
import hospital.entity.Charge;
import hospital.entity.Registration;
import hospital.entity.Schedule;
import hospital.entity.Slot;
import hospital.util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * 收费业务类：负责按挂号单收费与收费单查询，金额取挂号时的挂号费快照。
 */
public class ChargeService {

    private final ChargeDao chargeDao = new ChargeDao();
    private final RegistrationDao registrationDao = new RegistrationDao();
    private final SlotDao slotDao = new SlotDao();
    private final ScheduleDao scheduleDao = new ScheduleDao();

    /**
     * 按挂号单号查询收费单，未收费时抛出业务异常。
     */
    public Charge getByRegId(int regId) {
        if (regId <= 0) {
            throw new BusinessException("挂号单编号非法：" + regId);
        }
        try (Connection conn = DBUtil.getConnection()) {
            Charge charge = chargeDao.findByRegId(conn, regId);
            if (charge == null) {
                throw new BusinessException("该挂号单尚未收费");
            }
            return charge;
        } catch (SQLException e) {
            throw new BusinessException("查询收费单失败：" + e.getMessage(), e);
        }
    }

    /**
     * 对已挂号或已就诊的挂号单收费，返回收费单编号。
     */
    public int charge(int regId, String payMethod, int operatorId) {
        if (regId <= 0) {
            throw new BusinessException("挂号单编号非法：" + regId);
        }
        String method = requirePayMethod(payMethod);

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // 1. 挂号单必须存在
            Registration registration = registrationDao.findById(conn, regId);
            if (registration == null) {
                throw new BusinessException("挂号单不存在");
            }

            // 2. 只有已挂号或已就诊可收费
            String status = registration.getStatus();
            if (!"已挂号".equals(status) && !"已就诊".equals(status)) {
                throw new BusinessException("该挂号单状态为"
                        + statusText(status) + "，不能收费");
            }

            // 3. 停诊排班不能收费，避免患者交了钱却看不了病
            Slot slot = slotDao.findById(conn, registration.getSlotId());
            if (slot == null) {
                throw new BusinessException("号源或排班不存在");
            }
            Schedule schedule = scheduleDao.findById(conn, slot.getScheduleId());
            if (schedule == null) {
                throw new BusinessException("号源或排班不存在");
            }
            if ("停诊".equals(schedule.getStatus())) {
                throw new BusinessException("该排班已停诊，请先办理退号");
            }

            // 4. 同一挂号单只能收费一次
            if (chargeDao.findByRegId(conn, regId) != null) {
                throw new BusinessException("该挂号单已收费");
            }
            if (registration.getRegFee() == null) {
                throw new BusinessException("挂号费缺失，无法收费");
            }

            // 5. 金额取挂号单上的挂号费快照
            Charge charge = new Charge();
            charge.setRegId(regId);
            charge.setAmount(registration.getRegFee());
            charge.setPayMethod(method);
            charge.setPayTime(LocalDateTime.now());
            charge.setStatus("已收费");
            charge.setOperatorId(operatorId);

            int chargeId = chargeDao.insert(conn, charge);
            if (chargeId <= 0) {
                throw new SQLException("未取回新增收费单的编号");
            }

            // 6. 提交
            conn.commit();
            return chargeId;
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ignored) {
                }
            }
            if (isUniqueViolation(e)) {
                throw new BusinessException("该挂号单已收费", e);
            }
            throw new BusinessException("收费失败：" + e.getMessage(), e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * 校验支付方式只能是表结构规定的四种。
     */
    private String requirePayMethod(String payMethod) {
        String value = payMethod == null ? "" : payMethod.trim();
        if (value.isEmpty()) {
            throw new BusinessException("支付方式不能为空");
        }
        if (!"现金".equals(value) && !"微信".equals(value)
                && !"支付宝".equals(value) && !"银行卡".equals(value)) {
            throw new BusinessException("支付方式非法：" + value);
        }
        return value;
    }

    /**
     * 状态为空时用“未知”兜底，保证提示可读。
     */
    private String statusText(String status) {
        return status == null || status.trim().isEmpty() ? "未知" : status;
    }

    /**
     * 判断是否为唯一约束冲突。
     */
    private static boolean isUniqueViolation(SQLException e) {
        return "23505".equals(e.getSQLState());
    }
}
