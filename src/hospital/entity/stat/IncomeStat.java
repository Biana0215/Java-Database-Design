package hospital.entity.stat;

import java.math.BigDecimal;

/**
 * 收入统计结果：类别标签（科室名或支付方式）+ 金额 + 笔数。
 */
public class IncomeStat {

    // 类别标签，按科室统计时为科室名，日结时为支付方式
    private String label;

    // 金额，日结口径下退费记负数
    private BigDecimal amount;

    // 笔数
    private int count;

    public IncomeStat() {
    }

    public IncomeStat(String label, BigDecimal amount, int count) {
        this.label = label;
        this.amount = amount;
        this.count = count;
    }

    public String getLabel() { return label; }

    public void setLabel(String label) { this.label = label; }

    public BigDecimal getAmount() { return amount; }

    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public int getCount() { return count; }

    public void setCount(int count) { this.count = count; }

    public String toString() {
        return "IncomeStat{" +
                "label='" + label + '\'' +
                ", amount=" + amount +
                ", count=" + count +
                '}';
    }
}
