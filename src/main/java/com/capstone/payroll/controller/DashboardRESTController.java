package com.capstone.payroll.controller;

import com.capstone.payroll.model.Employee;
import com.capstone.payroll.model.Payroll;
import com.capstone.payroll.repository.EmployeeRepository;
import com.capstone.payroll.repository.PayrollRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardRESTController {

    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private PayrollRepository payrollRepository;

    @GetMapping("/stats")
    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();

        List<Employee> allEmployees = employeeRepository.findAll();
        long totalTeaching = 0;
        long totalRegular = 0; 

        // 1. Identify Employee Types for the "Total" Denominator
        for (Employee e : allEmployees) {
            String status = e.getEmployeeStatus() != null ? e.getEmployeeStatus().toLowerCase() : "";
            String desig = (e.getDesignation() != null && e.getDesignation().getDesignation() != null) 
                            ? e.getDesignation().getDesignation().toLowerCase() : "";
            
            if (status.contains("part") || status.contains("flexi") || status.contains("teaching") || desig.contains("faculty")) {
                totalTeaching++;
            } else {
                totalRegular++;
            }
        }

        // 2. Find the Latest SAVED Payroll to anchor the Cut-off Dates
        List<Payroll> allPayrolls = payrollRepository.findAll();
        LocalDate latestStart = null;
        LocalDate latestEnd = null;

        if (!allPayrolls.isEmpty()) {
            // Sort to get the most recent officially saved payslip
            allPayrolls.sort((p1, p2) -> {
                LocalDate d1 = p1.getPayPeriodEnd();
                LocalDate d2 = p2.getPayPeriodEnd();
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1; 
                if (d2 == null) return -1;
                return d2.compareTo(d1);
            });
            if (allPayrolls.get(0).getPayPeriodEnd() != null) {
                latestStart = allPayrolls.get(0).getPayPeriodStart();
                latestEnd = allPayrolls.get(0).getPayPeriodEnd();
            }
        }

        // Fallback if the entire database is wiped clean
        if (latestStart == null || latestEnd == null) {
            LocalDate today = LocalDate.now();
            if (today.getDayOfMonth() <= 15) {
                latestStart = today.withDayOfMonth(1);
                latestEnd = today.withDayOfMonth(15);
            } else {
                latestStart = today.withDayOfMonth(16);
                latestEnd = today.withDayOfMonth(today.lengthOfMonth());
            }
        }

        String cutoffPeriod = latestStart.format(DateTimeFormatter.ofPattern("MMM dd")) + " - " +
                              latestEnd.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"));

        final LocalDate finalStart = latestStart;
        final LocalDate finalEnd = latestEnd;

        // 3. Count only officially saved records matching those specific Cut-off dates
        long processedRegular = 0;
        long processedTeaching = 0;

        for (Payroll p : allPayrolls) {
            if (p.getPayPeriodStart() != null && p.getPayPeriodStart().equals(finalStart) &&
                p.getPayPeriodEnd() != null && p.getPayPeriodEnd().equals(finalEnd)) {
                
                Employee e = p.getEmployee();
                if (e != null) {
                    String status = e.getEmployeeStatus() != null ? e.getEmployeeStatus().toLowerCase() : "";
                    String desig = (e.getDesignation() != null && e.getDesignation().getDesignation() != null) 
                                    ? e.getDesignation().getDesignation().toLowerCase() : "";
                    
                    if (status.contains("part") || status.contains("flexi") || status.contains("teaching") || desig.contains("faculty")) {
                        processedTeaching++;
                    } else {
                        processedRegular++;
                    }
                }
            }
        }

        stats.put("cutoffPeriod", cutoffPeriod);
        stats.put("totalRegular", totalRegular);
        stats.put("processedRegular", processedRegular);
        stats.put("totalTeaching", totalTeaching);
        stats.put("processedTeaching", processedTeaching);

        return stats;
    }
}