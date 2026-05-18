package com.capstone.payroll.model;

import jakarta.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    @JsonIgnore
    private Employee employee;

    private String loanType; // e.g., "SSS Salary", "Pag-IBIG Calamity", "Cash Advance"
    
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal remainingBalance = BigDecimal.ZERO;
    private BigDecimal deductionPerCutoff = BigDecimal.ZERO;
    
    private LocalDate dateGranted;
    private String status = "ACTIVE"; // ACTIVE or COMPLETED

    public Loan() {}

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Employee getEmployee() { return employee; }
    public void setEmployee(Employee employee) { this.employee = employee; }
    public String getLoanType() { return loanType; }
    public void setLoanType(String loanType) { this.loanType = loanType; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getRemainingBalance() { return remainingBalance; }
    public void setRemainingBalance(BigDecimal remainingBalance) { this.remainingBalance = remainingBalance; }
    public BigDecimal getDeductionPerCutoff() { return deductionPerCutoff; }
    public void setDeductionPerCutoff(BigDecimal deductionPerCutoff) { this.deductionPerCutoff = deductionPerCutoff; }
    public LocalDate getDateGranted() { return dateGranted; }
    public void setDateGranted(LocalDate dateGranted) { this.dateGranted = dateGranted; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}