package com.capstone.payroll.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "payroll")
public class Payroll {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonIgnore
    private Employee employee;

    private LocalDate payPeriodStart;
    private LocalDate payPeriodEnd;

    @Column(length = 20)
    private String status = "DRAFT"; 

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "teaching_pay_id", referencedColumnName = "id")
    private TeachingPay teachingPayRecord;

    // --- Earnings ---
    private BigDecimal basicSalary;
    private BigDecimal overtimePay;
    private BigDecimal honorarium;
    private BigDecimal longevity;
    private BigDecimal adminPay;
    private BigDecimal deMinimis;
    private BigDecimal thirteenthMonthPay; 
    
    // --- NEW EARNINGS & ADJUSTMENTS ---
    private BigDecimal ndPay = BigDecimal.ZERO;
    private BigDecimal ecola = BigDecimal.ZERO;
    private BigDecimal otherIncome = BigDecimal.ZERO;
    private BigDecimal adjustment = BigDecimal.ZERO;
    private BigDecimal holidayPay = BigDecimal.ZERO;
    
    // --- MANUAL ADJUSTMENTS ---
    @Column(name = "manual_addition", precision = 10, scale = 2)
    private BigDecimal manualAddition = BigDecimal.ZERO;

    @Column(name = "manual_deduction", precision = 10, scale = 2)
    private BigDecimal manualDeduction = BigDecimal.ZERO;
    
    private BigDecimal cashGift = BigDecimal.ZERO;
    private BigDecimal incentive = BigDecimal.ZERO;
    private BigDecimal allowance = BigDecimal.ZERO;
    private BigDecimal relocationPay = BigDecimal.ZERO;

    // --- Leave Fields ---
    private BigDecimal leavePay = BigDecimal.ZERO;
    private BigDecimal leaveWithoutPay = BigDecimal.ZERO;

    // --- Deductions (Employee Share) ---
    private BigDecimal lateDeduction = BigDecimal.ZERO; 
    private BigDecimal undertimeDeduction = BigDecimal.ZERO; 
    private BigDecimal absentDeduction = BigDecimal.ZERO;
    private BigDecimal govtContributions; 
    private BigDecimal sssDeduction;
    private BigDecimal philhealthDeduction;
    private BigDecimal pagibigDeduction;
    private BigDecimal loanDeductions; 

    // --- NEW SPECIFIC LOANS ---
    private BigDecimal sssLoan = BigDecimal.ZERO;
    private BigDecimal hdmfLoan = BigDecimal.ZERO;
    
    // --- Tax Details ---
    private BigDecimal taxableIncome; 
    private BigDecimal withholdingTax; 

    // --- Totals ---
    private BigDecimal totalEarnings = BigDecimal.ZERO;
    private BigDecimal grossIncome;
    private BigDecimal netPay;

    // --- AUDIT TRAIL & BREAKDOWN FIELDS ---
    private Integer lateMinutes = 0;
    private Integer undertimeMinutes = 0;
    private Integer totalAbsences = 0;
    private Double totalOvertimeHours = 0.0;
    
    private BigDecimal minuteRate = BigDecimal.ZERO; 
    private BigDecimal dailyRate = BigDecimal.ZERO;  

    // --- EMPLOYER SHARES ---
    private BigDecimal sssEmployerShare = BigDecimal.ZERO;
    private BigDecimal philhealthEmployerShare = BigDecimal.ZERO;
    private BigDecimal pagibigEmployerShare = BigDecimal.ZERO;

    // --- DE MINIMIS SPLIT ---
    private BigDecimal nonTaxableDeMinimis = BigDecimal.ZERO;
    private BigDecimal taxableDeMinimis = BigDecimal.ZERO;

    public Payroll() {}

    // --- GETTERS AND SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public LocalDate getPayPeriodStart() { return payPeriodStart; }
    public void setPayPeriodStart(LocalDate payPeriodStart) { this.payPeriodStart = payPeriodStart; }
    public LocalDate getPayPeriodEnd() { return payPeriodEnd; }
    public void setPayPeriodEnd(LocalDate payPeriodEnd) { this.payPeriodEnd = payPeriodEnd; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public TeachingPay getTeachingPayRecord() { return teachingPayRecord; }
    public void setTeachingPayRecord(TeachingPay teachingPayRecord) { this.teachingPayRecord = teachingPayRecord; }

    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }
    public BigDecimal getOvertimePay() { return overtimePay; }
    public void setOvertimePay(BigDecimal overtimePay) { this.overtimePay = overtimePay; }
    public BigDecimal getHonorarium() { return honorarium; }
    public void setHonorarium(BigDecimal honorarium) { this.honorarium = honorarium; }
    public BigDecimal getLongevity() { return longevity; }
    public void setLongevity(BigDecimal longevity) { this.longevity = longevity; }
    public BigDecimal getAdminPay() { return adminPay; }
    public void setAdminPay(BigDecimal adminPay) { this.adminPay = adminPay; }
    public BigDecimal getDeMinimis() { return deMinimis; }
    public void setDeMinimis(BigDecimal deMinimis) { this.deMinimis = deMinimis; }
    public BigDecimal getThirteenthMonthPay() { return thirteenthMonthPay; }
    public void setThirteenthMonthPay(BigDecimal thirteenthMonthPay) { this.thirteenthMonthPay = thirteenthMonthPay; }
    
    public BigDecimal getNdPay() { return ndPay; }
    public void setNdPay(BigDecimal ndPay) { this.ndPay = ndPay; }
    public BigDecimal getEcola() { return ecola; }
    public void setEcola(BigDecimal ecola) { this.ecola = ecola; }
    public BigDecimal getOtherIncome() { return otherIncome; }
    public void setOtherIncome(BigDecimal otherIncome) { this.otherIncome = otherIncome; }
    public BigDecimal getAdjustment() { return adjustment; }
    public void setAdjustment(BigDecimal adjustment) { this.adjustment = adjustment; }
    public BigDecimal getHolidayPay() { return holidayPay; }
    public void setHolidayPay(BigDecimal holidayPay) { this.holidayPay = holidayPay; }
    public BigDecimal getSssLoan() { return sssLoan; }
    public void setSssLoan(BigDecimal sssLoan) { this.sssLoan = sssLoan; }
    public BigDecimal getHdmfLoan() { return hdmfLoan; }
    public void setHdmfLoan(BigDecimal hdmfLoan) { this.hdmfLoan = hdmfLoan; }

    public BigDecimal getManualAddition() { return manualAddition; }
    public void setManualAddition(BigDecimal manualAddition) { this.manualAddition = manualAddition; }
    public BigDecimal getManualDeduction() { return manualDeduction; }
    public void setManualDeduction(BigDecimal manualDeduction) { this.manualDeduction = manualDeduction; }

    public BigDecimal getCashGift() { return cashGift; }
    public void setCashGift(BigDecimal cashGift) { this.cashGift = cashGift; }
    public BigDecimal getIncentive() { return incentive; }
    public void setIncentive(BigDecimal incentive) { this.incentive = incentive; }
    public BigDecimal getAllowance() { return allowance; }
    public void setAllowance(BigDecimal allowance) { this.allowance = allowance; }
    public BigDecimal getRelocationPay() { return relocationPay; }
    public void setRelocationPay(BigDecimal relocationPay) { this.relocationPay = relocationPay; }

    public BigDecimal getLeavePay() { return leavePay; }
    public void setLeavePay(BigDecimal leavePay) { this.leavePay = leavePay; }
    public BigDecimal getLeaveWithoutPay() { return leaveWithoutPay; }
    public void setLeaveWithoutPay(BigDecimal leaveWithoutPay) { this.leaveWithoutPay = leaveWithoutPay; }
    
    public BigDecimal getLateDeduction() { return lateDeduction; }
    public void setLateDeduction(BigDecimal lateDeduction) { this.lateDeduction = lateDeduction; }
    public BigDecimal getUndertimeDeduction() { return undertimeDeduction; }
    public void setUndertimeDeduction(BigDecimal undertimeDeduction) { this.undertimeDeduction = undertimeDeduction; }
    public BigDecimal getAbsentDeduction() { return absentDeduction; }
    public void setAbsentDeduction(BigDecimal absentDeduction) { this.absentDeduction = absentDeduction; }
    public BigDecimal getGovtContributions() { return govtContributions; }
    public void setGovtContributions(BigDecimal govtContributions) { this.govtContributions = govtContributions; }
    public BigDecimal getSssDeduction() { return sssDeduction; }
    public void setSssDeduction(BigDecimal sssDeduction) { this.sssDeduction = sssDeduction; }
    public BigDecimal getPhilhealthDeduction() { return philhealthDeduction; }
    public void setPhilhealthDeduction(BigDecimal philhealthDeduction) { this.philhealthDeduction = philhealthDeduction; }
    public BigDecimal getPagibigDeduction() { return pagibigDeduction; }
    public void setPagibigDeduction(BigDecimal pagibigDeduction) { this.pagibigDeduction = pagibigDeduction; }
    public BigDecimal getLoanDeductions() { return loanDeductions; }
    public void setLoanDeductions(BigDecimal loanDeductions) { this.loanDeductions = loanDeductions; }
    
    public BigDecimal getTaxableIncome() { return taxableIncome; }
    public void setTaxableIncome(BigDecimal taxableIncome) { this.taxableIncome = taxableIncome; }
    public BigDecimal getWithholdingTax() { return withholdingTax; }
    public void setWithholdingTax(BigDecimal withholdingTax) { this.withholdingTax = withholdingTax; }
    
    public BigDecimal getTotalEarnings() { return totalEarnings; }
    public void setTotalEarnings(BigDecimal totalEarnings) { this.totalEarnings = totalEarnings; }

    public BigDecimal getGrossIncome() { return grossIncome; }
    public void setGrossIncome(BigDecimal grossIncome) { this.grossIncome = grossIncome; }
    public BigDecimal getNetPay() { return netPay; }
    public void setNetPay(BigDecimal netPay) { this.netPay = netPay; }

    public Integer getLateMinutes() { return lateMinutes; }
    public void setLateMinutes(Integer lateMinutes) { this.lateMinutes = lateMinutes; }
    public Integer getUndertimeMinutes() { return undertimeMinutes; }
    public void setUndertimeMinutes(Integer undertimeMinutes) { this.undertimeMinutes = undertimeMinutes; }
    public Integer getTotalAbsences() { return totalAbsences; }
    public void setTotalAbsences(Integer totalAbsences) { this.totalAbsences = totalAbsences; }
    public Double getTotalOvertimeHours() { return totalOvertimeHours; }
    public void setTotalOvertimeHours(Double totalOvertimeHours) { this.totalOvertimeHours = totalOvertimeHours; }
    
    public BigDecimal getMinuteRate() { return minuteRate; }
    public void setMinuteRate(BigDecimal minuteRate) { this.minuteRate = minuteRate; }
    public BigDecimal getDailyRate() { return dailyRate; }
    public void setDailyRate(BigDecimal dailyRate) { this.dailyRate = dailyRate; }

    public BigDecimal getSssEmployerShare() { return sssEmployerShare; }
    public void setSssEmployerShare(BigDecimal sssEmployerShare) { this.sssEmployerShare = sssEmployerShare; }
    public BigDecimal getPhilhealthEmployerShare() { return philhealthEmployerShare; }
    public void setPhilhealthEmployerShare(BigDecimal philhealthEmployerShare) { this.philhealthEmployerShare = philhealthEmployerShare; }
    public BigDecimal getPagibigEmployerShare() { return pagibigEmployerShare; }
    public void setPagibigEmployerShare(BigDecimal pagibigEmployerShare) { this.pagibigEmployerShare = pagibigEmployerShare; }
    public BigDecimal getNonTaxableDeMinimis() { return nonTaxableDeMinimis; }
    public void setNonTaxableDeMinimis(BigDecimal nonTaxableDeMinimis) { this.nonTaxableDeMinimis = nonTaxableDeMinimis; }
    public BigDecimal getTaxableDeMinimis() { return taxableDeMinimis; }
    public void setTaxableDeMinimis(BigDecimal taxableDeMinimis) { this.taxableDeMinimis = taxableDeMinimis; }
}