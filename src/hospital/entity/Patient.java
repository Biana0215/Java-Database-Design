package hospital.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 患者实体，对应 patient 表。
 */
public class Patient {

    // 患者编号，对应 patient.patient_id
    private int patientId;

    // 患者姓名，对应 patient.patient_name
    private String patientName;

    // 性别（男/女），对应 patient.gender
    private String gender;

    // 出生日期，对应 patient.birth_date
    private LocalDate birthDate;

    // 身份证号，对应 patient.id_card
    private String idCard;

    // 联系电话，对应 patient.phone
    private String phone;

    // 家庭住址，对应 patient.address
    private String address;

    // 建档时间，对应 patient.create_time
    private LocalDateTime createTime;

    public Patient() {
    }

    public Patient(int patientId, String patientName, String gender, LocalDate birthDate,
                   String idCard, String phone, String address, LocalDateTime createTime) {
        this.patientId = patientId;
        this.patientName = patientName;
        this.gender = gender;
        this.birthDate = birthDate;
        this.idCard = idCard;
        this.phone = phone;
        this.address = address;
        this.createTime = createTime;
    }

    public int getPatientId() { return patientId; }

    public void setPatientId(int patientId) { this.patientId = patientId; }

    public String getPatientName() { return patientName; }

    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getGender() { return gender; }

    public void setGender(String gender) { this.gender = gender; }

    public LocalDate getBirthDate() { return birthDate; }

    public void setBirthDate(LocalDate birthDate) { this.birthDate = birthDate; }

    public String getIdCard() { return idCard; }

    public void setIdCard(String idCard) { this.idCard = idCard; }

    public String getPhone() { return phone; }

    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }

    public void setAddress(String address) { this.address = address; }

    public LocalDateTime getCreateTime() { return createTime; }

    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String toString() {
        return "Patient{" +
                "patientId=" + patientId +
                ", patientName=" + patientName +
                ", gender=" + gender +
                ", birthDate=" + birthDate +
                ", idCard=" + idCard +
                ", phone=" + phone +
                ", address=" + address +
                ", createTime=" + createTime +
                '}';
    }
}
