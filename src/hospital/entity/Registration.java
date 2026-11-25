package hospital.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 挂号单实体，对应 registration 表。
 * patientName、doctorName 为非表字段，仅用于查询结果回填，不参与 INSERT/UPDATE。
 */
public class Registration {

    // 挂号单号，对应 registration.reg_id
    private int regId;

    // 患者编号，对应 registration.patient_id
    private int patientId;

    // 号源编号，对应 registration.slot_id
    private int slotId;

    // 挂号时间，对应 registration.reg_time
    private LocalDateTime regTime;

    // 挂号类型（普通/专家），对应 registration.reg_type
    private String regType;

    // 挂号费快照，对应 registration.reg_fee
    private BigDecimal regFee;

    // 挂号单状态（已挂号/已就诊/已退号），对应 registration.status
    private String status;

    // 操作员编号，对应 registration.operator_id，可为空
    private Integer operatorId;

    // 退号时间，对应 registration.refund_time
    private LocalDateTime refundTime;

    // 退号原因，对应 registration.refund_reason
    private String refundReason;

    // 患者姓名，非表字段，仅用于查询结果回填，不参与 INSERT/UPDATE
    private String patientName;

    // 医生姓名，非表字段，仅用于查询结果回填，不参与 INSERT/UPDATE
    private String doctorName;

    public Registration() {
    }

    public Registration(int regId, int patientId, int slotId, LocalDateTime regTime,
                        String regType, BigDecimal regFee, String status, Integer operatorId,
                        LocalDateTime refundTime, String refundReason) {
        this.regId = regId;
        this.patientId = patientId;
        this.slotId = slotId;
        this.regTime = regTime;
        this.regType = regType;
        this.regFee = regFee;
        this.status = status;
        this.operatorId = operatorId;
        this.refundTime = refundTime;
        this.refundReason = refundReason;
    }

    public int getRegId() { return regId; }

    public void setRegId(int regId) { this.regId = regId; }

    public int getPatientId() { return patientId; }

    public void setPatientId(int patientId) { this.patientId = patientId; }

    public int getSlotId() { return slotId; }

    public void setSlotId(int slotId) { this.slotId = slotId; }

    public LocalDateTime getRegTime() { return regTime; }

    public void setRegTime(LocalDateTime regTime) { this.regTime = regTime; }

    public String getRegType() { return regType; }

    public void setRegType(String regType) { this.regType = regType; }

    public BigDecimal getRegFee() { return regFee; }

    public void setRegFee(BigDecimal regFee) { this.regFee = regFee; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public Integer getOperatorId() { return operatorId; }

    public void setOperatorId(Integer operatorId) { this.operatorId = operatorId; }

    public LocalDateTime getRefundTime() { return refundTime; }

    public void setRefundTime(LocalDateTime refundTime) { this.refundTime = refundTime; }

    public String getRefundReason() { return refundReason; }

    public void setRefundReason(String refundReason) { this.refundReason = refundReason; }

    public String getPatientName() { return patientName; }

    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getDoctorName() { return doctorName; }

    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String toString() {
        return "Registration{" +
                "regId=" + regId +
                ", patientId=" + patientId +
                ", slotId=" + slotId +
                ", regTime=" + regTime +
                ", regType=" + regType +
                ", regFee=" + regFee +
                ", status=" + status +
                ", operatorId=" + operatorId +
                ", refundTime=" + refundTime +
                ", refundReason=" + refundReason +
                ", patientName=" + patientName +
                ", doctorName=" + doctorName +
                '}';
    }
}
