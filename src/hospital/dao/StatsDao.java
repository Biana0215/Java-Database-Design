package hospital.dao;

import hospital.entity.stat.DeptCountStat;
import hospital.entity.stat.IncomeStat;
import hospital.entity.stat.RateStat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 统计报表数据访问类：全部按时间段聚合查询。
 * 本类不管理连接与事务，由 Service 传入 Connection。
 *
 * 统一约定：
 * 1. 时间范围一律写成 {@code 列 >= ? and 列 < ?}，左闭右开，不使用 CAST(... AS date) = ?；
 * 2. 条件计数一律 SUM(CASE WHEN ... THEN 1 ELSE 0 END)，不使用 FILTER 子句；
 * 3. 入参 from、to 均为含首含尾的日期，内部换算成 [from 00:00, to+1 00:00) 的区间。
 */
public class StatsDao {

    /**
     * 各科室挂号量：科室左连接医生→排班→号源→挂号，统计挂号数（含已退号），按数量倒序。
     */
    public List<DeptCountStat> countRegByDept(Connection conn, LocalDate from, LocalDate to)
            throws SQLException {
        String sql = "select d.dept_name as dept_name, count(r.reg_id) as reg_count "
                + "from dept d "
                + "left join doctor doc on doc.dept_id = d.dept_id "
                + "left join schedule s on s.doctor_id = doc.doctor_id "
                + "left join slot sl on sl.schedule_id = s.schedule_id "
                + "left join registration r on r.slot_id = sl.slot_id "
                + "    and r.reg_time >= ? and r.reg_time < ? "
                + "group by d.dept_id, d.dept_name "
                + "order by count(r.reg_id) desc, d.dept_name";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, startOfDay(from));
            ps.setTimestamp(2, startOfNextDay(to));
            try (ResultSet rs = ps.executeQuery()) {
                List<DeptCountStat> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new DeptCountStat(rs.getString("dept_name"), rs.getInt("reg_count")));
                }
                return result;
            }
        }
    }

    /**
     * 各科室号源使用率：total 为出诊日期在区间内的号源总数（含停用），
     * hit 为状态为“已挂”的号源数，rate = hit / total * 100。
     * 口径说明：分子只算真正被挂走的号，停诊产生的“停用”号源不计入分子，
     * 否则停诊会让使用率虚高；分母保留全部号源，故“号源总数”列始终是排班放出号数之和。
     * 号源没有挂号时间，时间维度取排班出诊日期 work_date。
     */
    public List<RateStat> slotUsageByDept(Connection conn, LocalDate from, LocalDate to)
            throws SQLException {
        String sql = "select d.dept_name as dept_name, "
                + "count(sl.slot_id) as total_count, "
                + "sum(case when sl.status = '已挂' then 1 else 0 end) as hit_count "
                + "from dept d "
                + "left join doctor doc on doc.dept_id = d.dept_id "
                + "left join schedule s on s.doctor_id = doc.doctor_id "
                + "    and s.work_date >= ? and s.work_date < ? "
                + "left join slot sl on sl.schedule_id = s.schedule_id "
                + "group by d.dept_id, d.dept_name "
                + "order by d.dept_name";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(from));
            ps.setDate(2, Date.valueOf(to.plusDays(1)));
            try (ResultSet rs = ps.executeQuery()) {
                List<RateStat> result = new ArrayList<>();
                while (rs.next()) {
                    int total = rs.getInt("total_count");
                    int hit = intOrZero(rs, "hit_count");
                    result.add(new RateStat(rs.getString("dept_name"), total, hit, rateOf(hit, total)));
                }
                return result;
            }
        }
    }

    /**
     * 各科室退号率：total 为挂号数（含已退号），hit 为退号数，rate = hit / total * 100。
     */
    public List<RateStat> refundRateByDept(Connection conn, LocalDate from, LocalDate to)
            throws SQLException {
        String sql = "select d.dept_name as dept_name, "
                + "count(r.reg_id) as total_count, "
                + "sum(case when r.status = '已退号' then 1 else 0 end) as hit_count "
                + "from dept d "
                + "left join doctor doc on doc.dept_id = d.dept_id "
                + "left join schedule s on s.doctor_id = doc.doctor_id "
                + "left join slot sl on sl.schedule_id = s.schedule_id "
                + "left join registration r on r.slot_id = sl.slot_id "
                + "    and r.reg_time >= ? and r.reg_time < ? "
                + "group by d.dept_id, d.dept_name "
                + "order by d.dept_name";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, startOfDay(from));
            ps.setTimestamp(2, startOfNextDay(to));
            try (ResultSet rs = ps.executeQuery()) {
                List<RateStat> result = new ArrayList<>();
                while (rs.next()) {
                    int total = rs.getInt("total_count");
                    int hit = intOrZero(rs, "hit_count");
                    result.add(new RateStat(rs.getString("dept_name"), total, hit, rateOf(hit, total)));
                }
                return result;
            }
        }
    }

    /**
     * 各科室收入：只统计 charge.status='已收费' 的收费单，
     * 金额为 SUM(amount)，笔数为收费单条数，按金额倒序。
     */
    public List<IncomeStat> incomeByDept(Connection conn, LocalDate from, LocalDate to)
            throws SQLException {
        String sql = "select d.dept_name as dept_name, "
                + "coalesce(sum(c.amount), 0) as total_amount, "
                + "count(c.charge_id) as charge_count "
                + "from dept d "
                + "left join doctor doc on doc.dept_id = d.dept_id "
                + "left join schedule s on s.doctor_id = doc.doctor_id "
                + "left join slot sl on sl.schedule_id = s.schedule_id "
                + "left join registration r on r.slot_id = sl.slot_id "
                + "left join charge c on c.reg_id = r.reg_id and c.status = '已收费' "
                + "    and c.pay_time >= ? and c.pay_time < ? "
                + "group by d.dept_id, d.dept_name "
                + "order by coalesce(sum(c.amount), 0) desc, d.dept_name";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, startOfDay(from));
            ps.setTimestamp(2, startOfNextDay(to));
            try (ResultSet rs = ps.executeQuery()) {
                List<IncomeStat> result = new ArrayList<>();
                while (rs.next()) {
                    BigDecimal amount = rs.getBigDecimal("total_amount");
                    result.add(new IncomeStat(rs.getString("dept_name"),
                            amount == null ? BigDecimal.ZERO : amount,
                            rs.getInt("charge_count")));
                }
                return result;
            }
        }
    }

    /**
     * 某日按支付方式汇总：已收费计正、已退费计负，笔数为该方式当日的收费单条数，
     * 时间维度取收费时间 pay_time。
     */
    public List<IncomeStat> dailySummary(Connection conn, LocalDate date) throws SQLException {
        String sql = "select coalesce(pay_method, '未填写') as label, "
                + "coalesce(sum(case when status = '已收费' then amount else -amount end), 0) as total_amount, "
                + "count(*) as charge_count "
                + "from charge "
                + "where pay_time >= ? and pay_time < ? "
                + "group by coalesce(pay_method, '未填写') "
                + "order by coalesce(pay_method, '未填写')";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, startOfDay(date));
            ps.setTimestamp(2, startOfNextDay(date));
            try (ResultSet rs = ps.executeQuery()) {
                List<IncomeStat> result = new ArrayList<>();
                while (rs.next()) {
                    BigDecimal amount = rs.getBigDecimal("total_amount");
                    result.add(new IncomeStat(rs.getString("label"),
                            amount == null ? BigDecimal.ZERO : amount,
                            rs.getInt("charge_count")));
                }
                return result;
            }
        }
    }

    /**
     * 百分比：保留 1 位小数，分母为 0 时返回 0.0。
     */
    private static double rateOf(int hit, int total) {
        if (total <= 0) {
            return 0.0;
        }
        return Math.round(hit * 1000.0 / total) / 10.0;
    }

    /**
     * SUM 在无匹配行时返回 null，此处统一按 0 处理。
     */
    private static int intOrZero(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? 0 : value;
    }

    private static Timestamp startOfDay(LocalDate date) {
        return Timestamp.valueOf(date.atStartOfDay());
    }

    private static Timestamp startOfNextDay(LocalDate date) {
        return Timestamp.valueOf(date.plusDays(1).atStartOfDay());
    }
}
