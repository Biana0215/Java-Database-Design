package hospital.service;

import hospital.dao.SlotDao;
import hospital.entity.Slot;
import hospital.util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 号源查询业务类：只读操作，不开启事务，每次查询使用一个连接。
 */
public class SlotService {

    private final SlotDao slotDao = new SlotDao();

    /**
     * 查询指定排班下的全部号源。
     */
    public List<Slot> listBySchedule(int scheduleId) {
        if (scheduleId <= 0) {
            throw new BusinessException("排班编号非法：" + scheduleId);
        }
        try (Connection conn = DBUtil.getConnection()) {
            return slotDao.findBySchedule(conn, scheduleId);
        } catch (SQLException e) {
            throw new BusinessException("查询号源失败：" + e.getMessage(), e);
        }
    }

    /**
     * 查询指定排班下状态为“可挂”的号源。
     */
    public List<Slot> listAvailable(int scheduleId) {
        if (scheduleId <= 0) {
            throw new BusinessException("排班编号非法：" + scheduleId);
        }
        try (Connection conn = DBUtil.getConnection()) {
            return slotDao.findAvailable(conn, scheduleId);
        } catch (SQLException e) {
            throw new BusinessException("查询号源失败：" + e.getMessage(), e);
        }
    }

    /**
     * 统计指定排班下“可挂”号源数量，剩余号数实时计算、不落库。
     */
    public int countAvailable(int scheduleId) {
        if (scheduleId <= 0) {
            throw new BusinessException("排班编号非法：" + scheduleId);
        }
        try (Connection conn = DBUtil.getConnection()) {
            return slotDao.countAvailable(conn, scheduleId);
        } catch (SQLException e) {
            throw new BusinessException("统计剩余号源失败：" + e.getMessage(), e);
        }
    }
}
