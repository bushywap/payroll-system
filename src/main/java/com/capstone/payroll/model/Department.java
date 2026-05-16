package com.capstone.payroll.model;

import jakarta.persistence.*;

@Entity
@Table(name = "department")
public class Department {

    @Id
    @Column(name = "department_code", length = 20, nullable = false)
    private String departmentCode;

    @Column(name = "department_name", length = 100, nullable = false)
    private String departmentName;

    public Department() {}

    public Department(String departmentCode, String departmentName) {
        this.departmentCode = departmentCode;
        this.departmentName = departmentName;
    }

    public String getDepartmentCode() { return departmentCode; }
    public void setDepartmentCode(String departmentCode) { this.departmentCode = departmentCode; }

    public String getDepartmentName() { return departmentName; }
    public void setDepartmentName(String departmentName) { this.departmentName = departmentName; }
}