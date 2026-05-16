package com.capstone.payroll.config;

import com.capstone.payroll.model.Attendance;
import com.capstone.payroll.model.Department;
import com.capstone.payroll.model.Employee;
import com.capstone.payroll.repository.AttendanceRepository;
import com.capstone.payroll.repository.DepartmentRepository;
import com.capstone.payroll.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    
    private AttendanceRepository attendanceRepository;

    @Override
    public void run(String... args) throws Exception {

        // 1. SEED DEPARTMENTS
        if (departmentRepository.count() == 0) {
            List<Department> departments = Arrays.asList(
                new Department("SAS", "School of Arts and Sciences"),
                new Department("SBE", "School of Business Education"),
                new Department("SCR", "School of Criminology"),
                new Department("SET", "School of Engineering and Technology"),
                new Department("SHTM", "School of Hospitality and Tourism Management"),
                new Department("SMT", "School of Medical Technology"),
                new Department("SMD", "School of Medicine"),
                new Department("SON", "School of Nursing"),
                new Department("SOT", "School of PTOTRT"),
                new Department("SPH", "School of Pharmacy"),
                new Department("SMC", "School of Midwifery & Caregiving"),
                new Department("SND", "School of Nutrition Dietretian")
            );
            departmentRepository.saveAll(departments);
            System.out.println("✅ Departments initialized successfully.");
        }
    }
}