package com.capstone.payroll.controller;

import com.capstone.payroll.model.Employee;
import com.capstone.payroll.repository.DepartmentRepository;
import com.capstone.payroll.repository.EmployeeRepository;
import com.capstone.payroll.repository.PayrollRepository;
import com.capstone.payroll.repository.TeachingLoadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.math.BigDecimal;
import java.util.List;

@Controller
public class EmployeeController {

    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private PayrollRepository payrollRepository;
    @Autowired private TeachingLoadRepository teachingLoadRepository;

    // --- HTML PAGE MAPPINGS ---
    
    @GetMapping("/dashboard")
    public String showDashboard(Model model) { 
        model.addAttribute("totalEmployees", employeeRepository.count());
        model.addAttribute("totalDepartments", departmentRepository.count());
        model.addAttribute("totalPayrolls", payrollRepository.count());
        model.addAttribute("totalTeachingLoads", teachingLoadRepository.count());
        return "dashboard"; 
    }

    @GetMapping("/employees")
    public String showEmployees() { return "employee"; }
    
    @GetMapping("/compensation")
    public String showCompensation(Model model) { 
        model.addAttribute("employees", employeeRepository.findAll());
        return "compensation"; 
    }
    
    @GetMapping("/payslips")
    public String showPayslips() { return "payslips"; }

    @GetMapping("/payroll-history")
    public String showHistory() { return "payroll_history"; }

    @GetMapping("/overtime")
    public String showOvertime() { return "overtime"; }

    @GetMapping("/profile")
    public String showProfile() { return "profile"; }

    @GetMapping("/notifications")
    public String showNotifications() { return "notifications"; }

    // --- REST API ENDPOINTS ---

    @GetMapping("/api/employees")
    @ResponseBody
    public ResponseEntity<List<Employee>> getAllEmployees() {
        return ResponseEntity.ok(employeeRepository.findAll());
    }

    @GetMapping("/api/employees/search")
    @ResponseBody
    public ResponseEntity<List<Employee>> searchEmployees(@RequestParam String query) {
        if (query == null || query.trim().isEmpty()) {
            return ResponseEntity.ok(employeeRepository.findAll());
        }
        return ResponseEntity.ok(employeeRepository.searchByIdOrName(query));
    }
    
    @PostMapping("/api/employees/update-compensation")
    @ResponseBody
    public ResponseEntity<?> updateCompensation(
            @RequestParam Long id,
            @RequestParam(required = false) BigDecimal basicSalary,
            @RequestParam(required = false) BigDecimal hourlyRate,
            @RequestParam(required = false) BigDecimal adminPay,
            @RequestParam(required = false) BigDecimal allowance,
            @RequestParam(required = false) BigDecimal honorarium,
            @RequestParam(required = false) BigDecimal relocationPay,
            @RequestParam(required = false) BigDecimal deMinimis,
            @RequestParam(required = false) BigDecimal longevity,
            @RequestParam(required = false) BigDecimal cashGift,
            @RequestParam(required = false) BigDecimal incentive) {
        
        Employee emp = employeeRepository.findById(id).orElse(null);
        if (emp == null) return ResponseEntity.badRequest().body("Employee not found");
        
        // Updates all base and allowance fields instantly in the database
        emp.setBasicSalary(basicSalary);
        emp.setHourlyRate(hourlyRate);
        
        // Taxable
        emp.setAdminPay(adminPay);
        emp.setAllowance(allowance);
        emp.setHonorarium(honorarium);
        emp.setRelocationPay(relocationPay);
        
        // Non-Taxable
        emp.setDeMinimis(deMinimis);
        emp.setLongevity(longevity);
        emp.setCashGift(cashGift);
        emp.setIncentive(incentive);
        
        employeeRepository.save(emp);
        return ResponseEntity.ok("Compensation updated successfully!");
    }
   
}	