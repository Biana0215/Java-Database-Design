package hospital.dao;

import hospital.entity.Slot;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * 号源数据访问类。
 * 本类不管理连接与事务，由 Service 传入 Connection。
 */
public class SlotDao {

    /**
     * 查询某排班下的全部号源，按 seq_no 升序返回。
     */
    public List<Slot> findBySchedule(Connection conn, int scheduleId) throws SQLException {
        String sql = "select slot_id, schedule_id, seq_no, status "
                + "from slot where schedule_id=? order by seq_no";
        List<Slot> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * 查询某排班下状态为“可挂”的号源，按 seq_no 升序返回。
     */
    public List<Slot> findAvailable(Connection conn, int scheduleId) throws SQLException {
        String sql = "select slot_id, schedule_id, seq_no, status "
                + "from slot where schedule_id=? and status='可挂' order by seq_no";
        List<Slot> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    /**
     * 统计某排班下“可挂”号源数量，直接返回 count(*)。
     */
    public int countAvailable(Connection conn, int scheduleId) throws SQLException {
        String sql = "select count(*) from slot where schedule_id=? and status='可挂'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    /**
     * 按主键查询号源，不存在时返回 null。
     */
    public Slot findById(Connection conn, int slotId) throws SQLException {
        String sql = "select slot_id, schedule_id, seq_no, status from slot where slot_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, slotId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    /**
     * 为指定排班循环生成 1..total 号源，逐条 insert 并返回成功条数；
     * 每条 insert 使用 RETURNING slot_id，由 Service 控制整体事务。
     */
    public int batchInsert(Connection conn, int scheduleId, int total) throws SQLException {
        if (total <= 0) {
            return 0;
        }
        String sql = "insert into slot (schedule_id, seq_no, status) "
                + "values (?, ?, '可挂') returning slot_id";
        int inserted = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int seqNo = 1; seqNo <= total; seqNo++) {
                ps.setInt(1, scheduleId);
                ps.setInt(2, seqNo);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        inserted++;
                    }
                }
            }
        }
        return inserted;
    }

    /**
     * 条件占号：仅当 status='可挂' 时改为 '已挂'，返回影响行数是否大于 0；
     * 返回 false 表示该号已被他人占用或已停用。
     */
    public boolean markUsed(Connection conn, int slotId) throws SQLException {
        String sql = "update slot set status='已挂' where slot_id=? and status='可挂'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, slotId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 条件释放号源：仅当 status='已挂' 时改回 '可挂'，返回是否至少更新一行。
     */
    public boolean markFree(Connection conn, int slotId) throws SQLException {
        String sql = "update slot set status='可挂' where slot_id=? and status='已挂'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, slotId);
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * 停诊处理：把指定排班下所有 '可挂' 号源改为 '停用'，返回实际更新条数。
     */
    public int disableAvailableBySchedule(Connection conn, int scheduleId) throws SQLException {
        String sql = "update slot set status='停用' "
                + "where schedule_id=? and status='可挂'";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, scheduleId);
            return ps.executeUpdate();
        }
    }

    /**
     * 将当前结果行映射为 Slot 实体。
     */
    private Slot mapRow(ResultSet rs) throws SQLException {
        Slot slot = new Slot();
        slot.setSlotId(rs.getInt("slot_id"));
        slot.setScheduleId(rs.getInt("schedule_id"));
        slot.setSeqNo(rs.getInt("seq_no"));
        slot.setStatus(rs.getString("status"));
        return slot;
    }
}
