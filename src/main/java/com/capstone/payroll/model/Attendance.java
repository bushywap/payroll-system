package com.capstone.payroll.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id")
    private String employeeId;

    private LocalDate date;
    private LocalTime timeIn;
    private LocalTime timeOut;
    
    private Integer totalHours; 
    private Integer minutesLate;
    private Integer undertimeHours; 
    private Integer overtimeHours; 

    // ✅ ADDED: Transient field to send "Holiday" / "Absent" / "Present" to the frontend
    @Transient
    private String remark;

    public Attendance() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getTimeIn() { return timeIn; }
    public void setTimeIn(LocalTime timeIn) { this.timeIn = timeIn; }
    public LocalTime getTimeOut() { return timeOut; }
    public void setTimeOut(LocalTime timeOut) { this.timeOut = timeOut; }
    
    public Integer getTotalHours() { return totalHours; }
    public void setTotalHours(Integer totalHours) { this.totalHours = totalHours; }
    
    public Integer getMinutesLate() { return minutesLate; }
    public void setMinutesLate(Integer minutesLate) { this.minutesLate = minutesLate; }
    
    public Integer getUndertimeHours() { return undertimeHours; }
    public void setUndertimeHours(Integer undertimeHours) { this.undertimeHours = undertimeHours; }
    
    public Integer getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(Integer overtimeHours) { this.overtimeHours = overtimeHours; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}