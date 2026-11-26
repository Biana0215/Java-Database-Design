package hospital.entity.stat;

/**
 * 科室挂号量统计结果：科室名 + 挂号人次。
 */
public class DeptCountStat {

    // 科室名称
    private String deptName;

    // 挂号人次，含已退号
    private int regCount;

    public DeptCountStat() {
    }

    public DeptCountStat(String deptName, int regCount) {
        this.deptName = deptName;
        this.regCount = regCount;
    }

    public String getDeptName() { return deptName; }

    public void setDeptName(String deptName) { this.deptName = deptName; }

    public int getRegCount() { return regCount; }

    public void setRegCount(int regCount) { this.regCount = regCount; }

    public String toString() {
        return "DeptCountStat{" +
                "deptName='" + deptName + '\'' +
                ", regCount=" + regCount +
                '}';
    }
}
