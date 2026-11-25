package hospital.entity;

import java.math.BigDecimal;

/**
 * 医生实体，对应 doctor 表。
 * deptName 为非表字段，仅用于查询结果回填，不参与 INSERT/UPDATE。
 */
public class Doctor {

    // 医生编号，对应 doctor.doctor_id
    private int doctorId;

    // 医生姓名，对应 doctor.doctor_name
    private String doctorName;

    // 所属科室编号，对应 doctor.dept_id
    private int deptId;

    // 职称，对应 doctor.title
    private String title;

    // 挂号费，对应 doctor.reg_fee
    private BigDecimal regFee;

    // 联系电话，对应 doctor.phone
    private String phone;

    // 在职状态（在职/离职），对应 doctor.status
    private String status;

    // 科室名称，非表字段，仅用于查询结果回填，不参与 INSERT/UPDATE
    private String deptName;

    public Doctor() {
    }

    public Doctor(int doctorId, String doctorName, int deptId, String title,
                  BigDecimal regFee, String phone, String status) {
        this.doctorId = doctorId;
        this.doctorName = doctorName;
        this.deptId = deptId;
        this.title = title;
        this.regFee = regFee;
        this.phone = phone;
        this.status = status;
    }

    public int getDoctorId() { return doctorId; }

    public void setDoctorId(int doctorId) { this.doctorId = doctorId; }

    public String getDoctorName() { return doctorName; }

    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public int getDeptId() { return deptId; }

    public void setDeptId(int deptId) { this.deptId = deptId; }

    public String getTitle() { return title; }

    public void setTitle(String title) { this.title = title; }

    public BigDecimal getRegFee() { return regFee; }

    public void setRegFee(BigDecimal regFee) { this.regFee = regFee; }

    public String getPhone() { return phone; }

    public void setPhone(String phone) { this.phone = phone; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String getDeptName() { return deptName; }

    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String toString() {
        return "Doctor{" +
                "doctorId=" + doctorId +
                ", doctorName=" + doctorName +
                ", deptId=" + deptId +
                ", title=" + title +
                ", regFee=" + regFee +
                ", phone=" + phone +
                ", status=" + status +
                ", deptName=" + deptName +
                '}';
    }
}
