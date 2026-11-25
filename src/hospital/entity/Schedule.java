package hospital.entity;

import java.time.LocalDate;

/**
 * 排班实体，对应 schedule 表。
 * doctorName 为非表字段，仅用于查询结果回填，不参与 INSERT/UPDATE。
 */
public class Schedule {

    // 排班编号，对应 schedule.schedule_id
    private int scheduleId;

    // 医生编号，对应 schedule.doctor_id
    private int doctorId;

    // 出诊日期，对应 schedule.work_date
    private LocalDate workDate;

    // 出诊时段（上午/下午），对应 schedule.period
    private String period;

    // 号源总数，对应 schedule.slot_total
    private int slotTotal;

    // 排班状态（正常/停诊），对应 schedule.status
    private String status;

    // 医生姓名，非表字段，仅用于查询结果回填，不参与 INSERT/UPDATE
    private String doctorName;

    public Schedule() {
    }

    public Schedule(int scheduleId, int doctorId, LocalDate workDate, String period,
                    int slotTotal, String status) {
        this.scheduleId = scheduleId;
        this.doctorId = doctorId;
        this.workDate = workDate;
        this.period = period;
        this.slotTotal = slotTotal;
        this.status = status;
    }

    public int getScheduleId() { return scheduleId; }

    public void setScheduleId(int scheduleId) { this.scheduleId = scheduleId; }

    public int getDoctorId() { return doctorId; }

    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }

    public LocalDate getWorkDate() { return workDate; }

    public void setWorkDate(LocalDate workDate) { this.workDate = workDate; }

    public String getPeriod() { return period; }

    public void setPeriod(String period) { this.period = period; }

    public int getSlotTotal() { return slotTotal; }

    public void setSlotTotal(int slotTotal) { this.slotTotal = slotTotal; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String getDoctorName() { return doctorName; }

    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String toString() {
        return "Schedule{" +
                "scheduleId=" + scheduleId +
                ", doctorId=" + doctorId +
                ", workDate=" + workDate +
                ", period=" + period +
                ", slotTotal=" + slotTotal +
                ", status=" + status +
                ", doctorName=" + doctorName +
                '}';
    }
}
