package hospital.entity;

/**
 * 号源实体，对应 slot 表。
 */
public class Slot {

    // 号源编号，对应 slot.slot_id
    private int slotId;

    // 所属排班编号，对应 slot.schedule_id
    private int scheduleId;

    // 序号，对应 slot.seq_no
    private int seqNo;

    // 号源状态（可挂/已挂/停用），对应 slot.status
    private String status;

    public Slot() {
    }

    public Slot(int slotId, int scheduleId, int seqNo, String status) {
        this.slotId = slotId;
        this.scheduleId = scheduleId;
        this.seqNo = seqNo;
        this.status = status;
    }

    public int getSlotId() { return slotId; }

    public void setSlotId(int slotId) { this.slotId = slotId; }

    public int getScheduleId() { return scheduleId; }

    public void setScheduleId(int scheduleId) { this.scheduleId = scheduleId; }

    public int getSeqNo() { return seqNo; }

    public void setSeqNo(int seqNo) { this.seqNo = seqNo; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String toString() {
        return "Slot{" +
                "slotId=" + slotId +
                ", scheduleId=" + scheduleId +
                ", seqNo=" + seqNo +
                ", status=" + status +
                '}';
    }
}
