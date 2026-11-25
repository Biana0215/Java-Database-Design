package hospital.entity;

/**
 * 系统用户实体，对应 sys_user 表。
 * doctorName 为非表字段，仅用于查询结果回填，不参与 INSERT/UPDATE。
 */
public class SysUser {

    // 用户编号，对应 sys_user.user_id
    private int userId;

    // 登录名，对应 sys_user.username
    private String username;

    // 密码的 SHA-256 摘要，对应 sys_user.pwd_hash
    private String pwdHash;

    // 角色（挂号员/医生/管理员），对应 sys_user.role
    private String role;

    // 关联的医生编号，对应 sys_user.doctor_id，可为空
    private Integer doctorId;

    // 用户状态（启用/停用），对应 sys_user.status
    private String status;

    // 医生姓名，非表字段，仅用于查询结果回填，不参与 INSERT/UPDATE
    private String doctorName;

    public SysUser() {
    }

    public SysUser(int userId, String username, String pwdHash, String role,
                   Integer doctorId, String status) {
        this.userId = userId;
        this.username = username;
        this.pwdHash = pwdHash;
        this.role = role;
        this.doctorId = doctorId;
        this.status = status;
    }

    public int getUserId() { return userId; }

    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public String getPwdHash() { return pwdHash; }

    public void setPwdHash(String pwdHash) { this.pwdHash = pwdHash; }

    public String getRole() { return role; }

    public void setRole(String role) { this.role = role; }

    public Integer getDoctorId() { return doctorId; }

    public void setDoctorId(Integer doctorId) { this.doctorId = doctorId; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String getDoctorName() { return doctorName; }

    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }

    public String toString() {
        String maskedPwdHash;
        if (pwdHash == null) {
            maskedPwdHash = null;
        } else if (pwdHash.length() <= 8) {
            maskedPwdHash = pwdHash;
        } else {
            maskedPwdHash = pwdHash.substring(0, 8);
        }
        return "SysUser{" +
                "userId=" + userId +
                ", username=" + username +
                ", pwdHash=" + maskedPwdHash +
                ", role=" + role +
                ", doctorId=" + doctorId +
                ", status=" + status +
                ", doctorName=" + doctorName +
                '}';
    }
}
