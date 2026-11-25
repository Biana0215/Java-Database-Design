package hospital.entity;

/**
 * 科室实体，对应 dept 表。
 */
public class Dept {

    // 科室编号，对应 dept.dept_id
    private int deptId;

    // 科室名称，对应 dept.dept_name
    private String deptName;

    // 科室电话，对应 dept.dept_phone
    private String deptPhone;

    // 科室位置，对应 dept.location
    private String location;

    // 科室状态（启用/停用），对应 dept.status
    private String status;

    public Dept() {
    }

    public Dept(int deptId, String deptName, String deptPhone, String location, String status) {
        this.deptId = deptId;
        this.deptName = deptName;
        this.deptPhone = deptPhone;
        this.location = location;
        this.status = status;
    }

    public int getDeptId() { return deptId; }

    public void setDeptId(int deptId) { this.deptId = deptId; }

    public String getDeptName() { return deptName; }

    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getDeptPhone() { return deptPhone; }

    public void setDeptPhone(String deptPhone) { this.deptPhone = deptPhone; }

    public String getLocation() { return location; }

    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public String toString() {
        return "Dept{" +
                "deptId=" + deptId +
                ", deptName=" + deptName +
                ", deptPhone=" + deptPhone +
                ", location=" + location +
                ", status=" + status +
                '}';
    }
}
