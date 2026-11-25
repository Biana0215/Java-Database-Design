package hospital.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 收费单实体，对应 charge 表。
 */
public class Charge {

    // 收费单号，对应 charge.charge_id
    private int chargeId;

    // 挂号单号，对应 charge.reg_id
    private int regId;

    // 收费金额，对应 charge.amount
    private BigDecimal amount;

    // 支付方式（现金/微信/支付宝/银行卡），对应 charge.pay_method
    private String payMethod;

    // 收费时间，对应 charge.pay_time
    private LocalDateTime payTime;

    // 退费时间，对应 charge.refund_time
    private LocalDateTime refundTime;

    // 收费单状态（已收费/已退费），对应 charge.status
    private String status;

    // 操作员编号，对应 charge.operator_id，可为空
    private Integer operatorId;

    public Charge() {
    }

    public Charge(int chargeId, int regId, BigDecimal amount, String payMethod,
                  LocalDateTime payTime, LocalDateTime refundTime, String status, Integer operatorId) {
        this.chargeId = chargeId;
        this.regId = regId;
        this.amount = amount;
        this.payMethod = payMethod;
        this.payTime = payTime;
        this.refundTime = refundTime;
        this.status = status;
        this.operatorId = operatorId;
    }

    public int getChargeId() { return chargeId; }

    public void setChargeId(int chargeId) { this.chargeId = chargeId; }

    public int getRegId() { return regId; }

    public void setRegId(int regId) { this.regId = regId; }

    public BigDecimal getAmount() { return amount; }

    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getPayMethod() { return payMethod; }

    public void setPayMethod(String payMethod) { this.payMethod = payMethod; }

    public LocalDateTime getPayTime() { return payTime; }

    public void setPayTime(LocalDateTime payTime) { this.payTime = payTime; }

    public LocalDateTime getRefundTime() { return refundTime; }

    public void setRefundTime(LocalDateTime refundTime) { this.refundTime = refundTime; }

    public String getStatus() { return status; }

    public void setStatus(String status) { this.status = status; }

    public Integer getOperatorId() { return operatorId; }

    public void setOperatorId(Integer operatorId) { this.operatorId = operatorId; }

    public String toString() {
        return "Charge{" +
                "chargeId=" + chargeId +
                ", regId=" + regId +
                ", amount=" + amount +
                ", payMethod=" + payMethod +
                ", payTime=" + payTime +
                ", refundTime=" + refundTime +
                ", status=" + status +
                ", operatorId=" + operatorId +
                '}';
    }
}
