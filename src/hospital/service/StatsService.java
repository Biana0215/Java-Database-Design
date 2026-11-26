package hospital.service;

import hospital.dao.StatsDao;
import hospital.entity.stat.DeptCountStat;
import hospital.entity.stat.IncomeStat;
import hospital.entity.stat.RateStat;
import hospital.util.DBUtil;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * 统计报表业务类：只读聚合，每次统计使用一个连接，不开启事务。
 * 日期参数为 null 时，起点默认本月 1 日、终点默认今天。
 */
public class StatsService {

    private final StatsDao statsDao = new StatsDao();

    /**
     * 各科室挂号量（含已退号），按数量倒序。
     */
    public List<DeptCountStat> countRegByDept(LocalDate from, LocalDate to) {
        LocalDate start = defaultFrom(from);
        LocalDate end = defaultTo(to);
        checkRange(start, end);
        try (Connection conn = DBUtil.getConnection()) {
            return statsDao.countRegByDept(conn, start, end);
        } catch (SQLException e) {
            throw new BusinessException("统计科室挂号量失败：" + e.getMessage(), e);
        }
    }

    /**
     * 各科室号源使用率，时间维度为排班出诊日期。
     */
    public List<RateStat> slotUsageByDept(LocalDate from, LocalDate to) {
        LocalDate start = defaultFrom(from);
        LocalDate end = defaultTo(to);
        checkRange(start, end);
        try (Connection conn = DBUtil.getConnection()) {
            return statsDao.slotUsageByDept(conn, start, end);
        } catch (SQLException e) {
            throw new BusinessException("统计号源使用率失败：" + e.getMessage(), e);
        }
    }

    /**
     * 各科室退号率。
     */
    public List<RateStat> refundRateByDept(LocalDate from, LocalDate to) {
        LocalDate start = defaultFrom(from);
        LocalDate end = defaultTo(to);
        checkRange(start, end);
        try (Connection conn = DBUtil.getConnection()) {
            return statsDao.refundRateByDept(conn, start, end);
        } catch (SQLException e) {
            throw new BusinessException("统计退号率失败：" + e.getMessage(), e);
        }
    }

    /**
     * 各科室收入，按收费时间落在区间内的已收费单汇总。
     */
    public List<IncomeStat> incomeByDept(LocalDate from, LocalDate to) {
        LocalDate start = defaultFrom(from);
        LocalDate end = defaultTo(to);
        checkRange(start, end);
        try (Connection conn = DBUtil.getConnection()) {
            return statsDao.incomeByDept(conn, start, end);
        } catch (SQLException e) {
            throw new BusinessException("统计科室收入失败：" + e.getMessage(), e);
        }
    }

    /**
     * 某日按支付方式汇总，已收费计正、已退费计负，供日结面板使用。
     */
    public List<IncomeStat> dailySummary(LocalDate date) {
        LocalDate target = date == null ? LocalDate.now() : date;
        try (Connection conn = DBUtil.getConnection()) {
            return statsDao.dailySummary(conn, target);
        } catch (SQLException e) {
            throw new BusinessException("统计日结汇总失败：" + e.getMessage(), e);
        }
    }

    /**
     * 起始日期默认本月 1 日。
     */
    private static LocalDate defaultFrom(LocalDate from) {
        return from == null ? LocalDate.now().withDayOfMonth(1) : from;
    }

    /**
     * 结束日期默认今天。
     */
    private static LocalDate defaultTo(LocalDate to) {
        return to == null ? LocalDate.now() : to;
    }

    private static void checkRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new BusinessException("起始日期不能晚于结束日期");
        }
    }
}
