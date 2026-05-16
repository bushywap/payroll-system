package com.capstone.payroll.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.List;

// --- NEW IMPORTS TO STOP INFINITE JSON LOOPS ---
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "employee")
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // The physical school ID string (e.g., "1-00000")
    @Column(name = "employee_id", length = 50, unique = true, nullable = false)
    private String employeeId;

    @Column(name = "first_name", length = 50, nullable = false)
    private String firstName;
    
    @Column(name = "last_name", length = 50, nullable = false)
    private String lastName;

    @Column(length = 100, unique = true)
    private String email;

    @Column(name = "account_status", length = 50)
    private String accountStatus = "Active"; 

    @Column(name = "employee_status", length = 50)
    private String employeeStatus; 

    @Column(name = "bank_account_number", length = 50)
    private String bankAccountNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "department_code")
    // FIX: Prevents loop if Department has a List<Employee>
    @JsonIgnoreProperties({"employees", "hibernateLazyInitializer", "handler"})
    private Department department;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "designation") 
    // FIX: Prevents loop if Designation has a List<Employee>
    @JsonIgnoreProperties({"employees", "hibernateLazyInitializer", "handler"})
    private EmpDesignation designation; 

    @Column(name = "admin_pay", precision = 10, scale = 2)
    private BigDecimal adminPay = BigDecimal.ZERO; 
    
    @Column(name = "basic_salary", precision = 10, scale = 2)
    private BigDecimal basicSalary = BigDecimal.ZERO;

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private BigDecimal hourlyRate = BigDecimal.ZERO; 

    @Column(name = "de_minimis", precision = 10, scale = 2)
    private BigDecimal deMinimis = BigDecimal.ZERO; 

    @Column(precision = 10, scale = 2)
    private BigDecimal longevity = BigDecimal.ZERO; 

    @Column(precision = 10, scale = 2)
    private BigDecimal honorarium = BigDecimal.ZERO; 

    @Column(precision = 10, scale = 2)
    private BigDecimal cashGift = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal incentive = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal allowance = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    private BigDecimal relocationPay = BigDecimal.ZERO;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    // FIX: Completely ignores LeaveBalances to stop Employee->LeaveBalance->Employee loop
    @JsonIgnore 
    private List<LeaveBalance> leaveBalances;

    public Employee() {}

    // --- GETTERS AND SETTERS ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAccountStatus() { return accountStatus; }
    public void setAccountStatus(String accountStatus) { this.accountStatus = accountStatus; }
    
    public String getEmployeeStatus() { return employeeStatus; }
    public void setEmployeeStatus(String employeeStatus) { this.employeeStatus = employeeStatus; }

    public String getBankAccountNumber() { return bankAccountNumber; }
    public void setBankAccountNumber(String bankAccountNumber) { this.bankAccountNumber = bankAccountNumber; }

    public Department getDepartment() { return department; }
    public void setDepartment(Department department) { this.department = department; }

    public EmpDesignation getDesignation() { return designation; }
    public void setDesignation(EmpDesignation designation) { this.designation = designation; }

    public BigDecimal getAdminPay() { return adminPay; }
    public void setAdminPay(BigDecimal adminPay) { this.adminPay = adminPay; }
    
    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal basicSalary) { this.basicSalary = basicSalary; }
    
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    
    public BigDecimal getDeMinimis() { return deMinimis; }
    public void setDeMinimis(BigDecimal deMinimis) { this.deMinimis = deMinimis; }
    
    public BigDecimal getLongevity() { return longevity; }
    public void setLongevity(BigDecimal longevity) { this.longevity = longevity; }
    
    public BigDecimal getHonorarium() { return honorarium; }
    public void setHonorarium(BigDecimal honorarium) { this.honorarium = honorarium; }

    public BigDecimal getCashGift() { return cashGift; }
    public void setCashGift(BigDecimal cashGift) { this.cashGift = cashGift; }

    public BigDecimal getIncentive() { return incentive; }
    public void setIncentive(BigDecimal incentive) { this.incentive = incentive; }

    public BigDecimal getAllowance() { return allowance; }
    public void setAllowance(BigDecimal allowance) { this.allowance = allowance; }

    public BigDecimal getRelocationPay() { return relocationPay; }
    public void setRelocationPay(BigDecimal relocationPay) { this.relocationPay = relocationPay; }
    
    public List<LeaveBalance> getLeaveBalances() { return leaveBalances; }
    public void setLeaveBalances(List<LeaveBalance> leaveBalances) { this.leaveBalances = leaveBalances; }
}