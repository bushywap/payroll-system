package com.capstone.payroll.dto;

public class AttendanceSummaryDTO {
    private String employeeId;
    private String name;
    private String department;
    private String designationName; 
    private String employeeStatus; 
    private int periodNo;
    
    private String totalLate;
    private String totalUndertime;
    private String totalAbsent;
    private String totalOT;
    
    // --- NEW: LEAVE FIELDS ---
    private String totalLeaveWithPay;
    private String totalLeaveWithoutPay;
    
    private String totalHoliday;
    private String totalSuspension;

    // Getters and Setters
    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String string) { this.employeeId = string; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDesignationName() { return designationName; }
    public void setDesignationName(String designationName) { this.designationName = designationName; }

    public String getEmployeeStatus() { return employeeStatus; }
    public void setEmployeeStatus(String employeeStatus) { this.employeeStatus = employeeStatus; }

    public int getPeriodNo() { return periodNo; }
    public void setPeriodNo(int periodNo) { this.periodNo = periodNo; }

    public String getTotalLate() { return totalLate; }
    public void setTotalLate(String totalLate) { this.totalLate = totalLate; }

    public String getTotalUndertime() { return totalUndertime; }
    public void setTotalUndertime(String totalUndertime) { this.totalUndertime = totalUndertime; }

    public String getTotalAbsent() { return totalAbsent; }
    public void setTotalAbsent(String totalAbsent) { this.totalAbsent = totalAbsent; }

    public String getTotalOT() { return totalOT; }
    public void setTotalOT(String totalOT) { this.totalOT = totalOT; }

    // --- NEW: LEAVE GETTERS & SETTERS ---
    public String getTotalLeaveWithPay() { return totalLeaveWithPay; }
    public void setTotalLeaveWithPay(String totalLeaveWithPay) { this.totalLeaveWithPay = totalLeaveWithPay; }

    public String getTotalLeaveWithoutPay() { return totalLeaveWithoutPay; }
    public void setTotalLeaveWithoutPay(String totalLeaveWithoutPay) { this.totalLeaveWithoutPay = totalLeaveWithoutPay; }

    public String getTotalHoliday() { return totalHoliday; }
    public void setTotalHoliday(String totalHoliday) { this.totalHoliday = totalHoliday; }

    public String getTotalSuspension() { return totalSuspension; }
    public void setTotalSuspension(String totalSuspension) { this.totalSuspension = totalSuspension; }
}