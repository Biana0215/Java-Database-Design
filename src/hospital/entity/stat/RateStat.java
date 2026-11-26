package hospital.entity.stat;

/**
 * 比率统计结果：科室名 + 分母总数 + 分子命中数 + 百分比（保留 1 位小数）。
 */
public class RateStat {

    // 科室名称
    private String deptName;

    // 分母：号源总数或挂号数
    private int total;

    // 分子：已使用号源数或退号数
    private int hit;

    // 百分比，已保留 1 位小数，total 为 0 时记 0.0
    private double rate;

    public RateStat() {
    }

    public RateStat(String deptName, int total, int hit, double rate) {
        this.deptName = deptName;
        this.total = total;
        this.hit = hit;
        this.rate = rate;
    }

    public String getDeptName() { return deptName; }

    public void setDeptName(String deptName) { this.deptName = deptName; }

    public int getTotal() { return total; }

    public void setTotal(int total) { this.total = total; }

    public int getHit() { return hit; }

    public void setHit(int hit) { this.hit = hit; }

    public double getRate() { return rate; }

    public void setRate(double rate) { this.rate = rate; }

    public String toString() {
        return "RateStat{" +
                "deptName='" + deptName + '\'' +
                ", total=" + total +
                ", hit=" + hit +
                ", rate=" + rate +
                '}';
    }
}
