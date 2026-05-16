package com.capstone.payroll.controller;

import com.capstone.payroll.model.Employee;
import com.capstone.payroll.model.Payroll;
import com.capstone.payroll.model.TeachingLoad;
import com.capstone.payroll.repository.DepartmentRepository;
import com.capstone.payroll.repository.EmployeeRepository;
import com.capstone.payroll.repository.PayrollRepository;
import com.capstone.payroll.repository.TeachingLoadRepository;
import com.capstone.payroll.service.EmailService; 
import com.capstone.payroll.service.EmployeeService;
import com.capstone.payroll.service.ExcelHelper;
import com.capstone.payroll.service.PayrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
public class PayrollController {

    @Autowired private PayrollService payrollService;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private PayrollRepository payrollRepository; 
    @Autowired private EmployeeService employeeService;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private TeachingLoadRepository teachingLoadRepository; 
    @Autowired private EmailService emailService;

    @GetMapping("/payroll")
    public String showPayrollPage(Model model) {
        model.addAttribute("payrollList", payrollRepository.findAll());
        model.addAttribute("departments", departmentRepository.findAll()); 
        return "payroll";
    }

    @GetMapping("/payroll_register")
    public String showPayrollRegisterPage(Model model) {
        List<Payroll> allPayrolls = payrollRepository.findAll();

        java.util.Map<String, List<Payroll>> groupedByDept = allPayrolls.stream()
            .collect(java.util.stream.Collectors.groupingBy(p -> {
                if (p.getEmployee() != null && p.getEmployee().getDepartment() != null) {
                    return p.getEmployee().getDepartment().getDepartmentName();
                }
                return "Unassigned Department";
            }));

        List<DeptPayrollGroup> groupedPayrolls = new ArrayList<>();
        for (java.util.Map.Entry<String, List<Payroll>> entry : groupedByDept.entrySet()) {
            groupedPayrolls.add(new DeptPayrollGroup(entry.getKey(), entry.getValue()));
        }
        groupedPayrolls.sort(java.util.Comparator.comparing(DeptPayrollGroup::getDepartmentName));
        model.addAttribute("groupedPayrolls", groupedPayrolls);

        BigDecimal grandBasicPay = BigDecimal.ZERO, grandLongvty = BigDecimal.ZERO, grandLeavePaid = BigDecimal.ZERO;
        BigDecimal grandHonorarium = BigDecimal.ZERO, grandTeach = BigDecimal.ZERO, grandOvertime = BigDecimal.ZERO;
        BigDecimal grandAdjustment = BigDecimal.ZERO, grandTotalEarn = BigDecimal.ZERO, grandPenalties = BigDecimal.ZERO;
        BigDecimal grandGrossPay = BigDecimal.ZERO, grandSSS = BigDecimal.ZERO, grandPhilHealth = BigDecimal.ZERO;
        BigDecimal grandPagIbig = BigDecimal.ZERO, grandSLoan = BigDecimal.ZERO, grandHLoan = BigDecimal.ZERO;
        BigDecimal grandOLoan = BigDecimal.ZERO, grandTax = BigDecimal.ZERO, grandLeaveUnpaid = BigDecimal.ZERO;
        BigDecimal grandTotalDed = BigDecimal.ZERO, grandNetPay = BigDecimal.ZERO;

        for (Payroll p : allPayrolls) {
            grandBasicPay = grandBasicPay.add(p.getBasicSalary() != null ? p.getBasicSalary() : BigDecimal.ZERO);
            grandLongvty = grandLongvty.add(p.getLongevity() != null ? p.getLongevity() : BigDecimal.ZERO);
            grandLeavePaid = grandLeavePaid.add(p.getLeavePay() != null ? p.getLeavePay() : BigDecimal.ZERO);
            grandHonorarium = grandHonorarium.add(p.getHonorarium() != null ? p.getHonorarium() : BigDecimal.ZERO);
            grandAdjustment = grandAdjustment.add(p.getAdjustment() != null ? p.getAdjustment() : BigDecimal.ZERO); 
            
            BigDecimal teachNet = p.getTeachingPayRecord() != null && p.getTeachingPayRecord().getTotalTeachingPay() != null ? p.getTeachingPayRecord().getTotalTeachingPay() : BigDecimal.ZERO;
            BigDecimal teachDed = BigDecimal.ZERO;
            if(p.getTeachingPayRecord() != null) {
                BigDecimal hrRate = p.getTeachingPayRecord().getHourlyRate() != null ? p.getTeachingPayRecord().getHourlyRate() : BigDecimal.ZERO;
                double safeHrs = p.getTeachingPayRecord().getTotalDeductionHours() != null ? p.getTeachingPayRecord().getTotalDeductionHours() : 0.0;
                teachDed = hrRate.multiply(BigDecimal.valueOf(safeHrs)).setScale(2, RoundingMode.HALF_UP);
            }
            BigDecimal teachGross = teachNet.add(teachDed); 
            
            grandTeach = grandTeach.add(teachGross);
            
            grandOvertime = grandOvertime.add(p.getOvertimePay() != null ? p.getOvertimePay() : BigDecimal.ZERO);
            grandTotalEarn = grandTotalEarn.add(p.getTotalEarnings() != null ? p.getTotalEarnings() : BigDecimal.ZERO);
            
            BigDecimal penalties = (p.getAbsentDeduction() != null ? p.getAbsentDeduction() : BigDecimal.ZERO)
                    .add(p.getLateDeduction() != null ? p.getLateDeduction() : BigDecimal.ZERO)
                    .add(p.getUndertimeDeduction() != null ? p.getUndertimeDeduction() : BigDecimal.ZERO);
            grandPenalties = grandPenalties.add(penalties);
            
            grandGrossPay = grandGrossPay.add(p.getGrossIncome() != null ? p.getGrossIncome() : BigDecimal.ZERO);
            grandSSS = grandSSS.add(p.getSssDeduction() != null ? p.getSssDeduction() : BigDecimal.ZERO);
            grandPhilHealth = grandPhilHealth.add(p.getPhilhealthDeduction() != null ? p.getPhilhealthDeduction() : BigDecimal.ZERO);
            grandPagIbig = grandPagIbig.add(p.getPagibigDeduction() != null ? p.getPagibigDeduction() : BigDecimal.ZERO);
            grandSLoan = grandSLoan.add(p.getSssLoan() != null ? p.getSssLoan() : BigDecimal.ZERO);
            grandHLoan = grandHLoan.add(p.getHdmfLoan() != null ? p.getHdmfLoan() : BigDecimal.ZERO);
            grandOLoan = grandOLoan.add(p.getLoanDeductions() != null ? p.getLoanDeductions() : BigDecimal.ZERO);
            grandTax = grandTax.add(p.getWithholdingTax() != null ? p.getWithholdingTax() : BigDecimal.ZERO);
            grandLeaveUnpaid = grandLeaveUnpaid.add(p.getLeaveWithoutPay() != null ? p.getLeaveWithoutPay() : BigDecimal.ZERO);
            
            BigDecimal totalDed = (p.getSssDeduction() != null ? p.getSssDeduction() : BigDecimal.ZERO)
                    .add(p.getPhilhealthDeduction() != null ? p.getPhilhealthDeduction() : BigDecimal.ZERO)
                    .add(p.getPagibigDeduction() != null ? p.getPagibigDeduction() : BigDecimal.ZERO)
                    .add(p.getSssLoan() != null ? p.getSssLoan() : BigDecimal.ZERO)
                    .add(p.getHdmfLoan() != null ? p.getHdmfLoan() : BigDecimal.ZERO)
                    .add(p.getLoanDeductions() != null ? p.getLoanDeductions() : BigDecimal.ZERO)
                    .add(p.getWithholdingTax() != null ? p.getWithholdingTax() : BigDecimal.ZERO)
                    .add(p.getLeaveWithoutPay() != null ? p.getLeaveWithoutPay() : BigDecimal.ZERO);
            grandTotalDed = grandTotalDed.add(totalDed);
            
            grandNetPay = grandNetPay.add(p.getNetPay() != null ? p.getNetPay() : BigDecimal.ZERO);
        }

        model.addAttribute("grandBasicPay", grandBasicPay); 
        model.addAttribute("grandLongvty", grandLongvty); 
        model.addAttribute("grandLeavePaid", grandLeavePaid); 
        model.addAttribute("grandHonorarium", grandHonorarium); 
        model.addAttribute("grandAdjustment", grandAdjustment); 
        model.addAttribute("grandTeach", grandTeach); 
        model.addAttribute("grandOvertime", grandOvertime); 
        model.addAttribute("grandTotalEarn", grandTotalEarn); 
        model.addAttribute("grandPenalties", grandPenalties); 
        model.addAttribute("grandGrossPay", grandGrossPay); 
        model.addAttribute("grandSSS", grandSSS); 
        model.addAttribute("grandPhilHealth", grandPhilHealth); 
        model.addAttribute("grandPagIbig", grandPagIbig); 
        model.addAttribute("grandSLoan", grandSLoan); 
        model.addAttribute("grandHLoan", grandHLoan); 
        model.addAttribute("grandOLoan", grandOLoan); 
        model.addAttribute("grandTax", grandTax); 
        model.addAttribute("grandLeaveUnpaid", grandLeaveUnpaid); 
        model.addAttribute("grandTotalDed", grandTotalDed); 
        model.addAttribute("grandNetPay", grandNetPay);
        return "payroll_register";
    }

    @GetMapping("/gov_deductions")
    public String showGovDeductionsPage(Model model) {
        model.addAttribute("payrollList", payrollRepository.findAll());
        return "gov_deductions";
    }

    private Employee getEmployeeFromQuery(String query) {
        List<Employee> emps = employeeRepository.searchByIdOrName(query);
        return emps.isEmpty() ? null : emps.get(0);
    }
    
    private String extractErrorMessage(Exception e) {
        e.printStackTrace();
        String msg = e.getMessage();
        if (e.getCause() != null) {
            msg += " -> " + e.getCause().getMessage();
            if (e.getCause().getCause() != null) {
                msg += " -> " + e.getCause().getCause().getMessage();
            }
        }
        return msg;
    }

    @PostMapping("/api/payroll/calculate")
    @ResponseBody
    public ResponseEntity<?> calculate(
            @RequestParam String employeeQuery,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "0") BigDecimal sssLoan,
            @RequestParam(defaultValue = "0") BigDecimal hdmfLoan,
            @RequestParam(defaultValue = "0") BigDecimal adjustment,
            @RequestParam(defaultValue = "0") BigDecimal honorarium,
            @RequestParam(defaultValue = "0") BigDecimal longevity) {
        try {
            Employee emp = getEmployeeFromQuery(employeeQuery);
            if (emp == null) return ResponseEntity.badRequest().body("Employee not found");
            
            List<TeachingLoad> loads = teachingLoadRepository.findByEmployeeId(emp.getId());
            boolean isFaculty = loads != null && !loads.isEmpty();
            
            Payroll result;
            if (isFaculty) {
                BigDecimal hourly = emp.getHourlyRate() != null ? emp.getHourlyRate() : BigDecimal.ZERO;
                BigDecimal totalLoans = sssLoan.add(hdmfLoan);
                
                result = payrollService.calculateTeachingPayrollPreview(emp, start, end, 0.0, 0.0, hourly, BigDecimal.ZERO, BigDecimal.ZERO, totalLoans);
                
                if (result != null) {
                    if (adjustment.compareTo(BigDecimal.ZERO) != 0) result.setAdjustment(adjustment);
                    if (honorarium.compareTo(BigDecimal.ZERO) != 0) result.setHonorarium(honorarium);
                    if (longevity.compareTo(BigDecimal.ZERO) != 0) result.setLongevity(longevity);
                    
                    result.setSssLoan(sssLoan);
                    result.setHdmfLoan(hdmfLoan);
                    result.setLoanDeductions(sssLoan.add(hdmfLoan));
                    
                    payrollService.computeTaxableIncomeAndTax(result); 
                }
            } else {
                result = payrollService.calculatePayrollPreview(emp, start, end, sssLoan, hdmfLoan, adjustment, honorarium, longevity);
            }
            
            return ResponseEntity.ok(mapSingleToSafeResponse(result)); 
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Backend Error: " + extractErrorMessage(e));
        }
    }

    @PostMapping("/api/payroll/send")
    @ResponseBody
    public ResponseEntity<?> send(
            @RequestParam String employeeQuery,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "0") BigDecimal sssLoan,
            @RequestParam(defaultValue = "0") BigDecimal hdmfLoan,
            @RequestParam(defaultValue = "0") BigDecimal adjustment,
            @RequestParam(defaultValue = "0") BigDecimal honorarium,
            @RequestParam(defaultValue = "0") BigDecimal longevity,
            @RequestParam(defaultValue = "PUBLISHED") String status) { 
        try {
            Employee emp = getEmployeeFromQuery(employeeQuery);
            if (emp == null) return ResponseEntity.badRequest().body("Employee not found");
            
            List<TeachingLoad> loads = teachingLoadRepository.findByEmployeeId(emp.getId());
            boolean isFaculty = loads != null && !loads.isEmpty();
            
            Payroll preview;
            if (isFaculty) {
                BigDecimal hourly = emp.getHourlyRate() != null ? emp.getHourlyRate() : BigDecimal.ZERO;
                BigDecimal totalLoans = sssLoan.add(hdmfLoan);
                
                preview = payrollService.calculateTeachingPayrollPreview(emp, start, end, 0.0, 0.0, hourly, BigDecimal.ZERO, BigDecimal.ZERO, totalLoans);
                
                if (preview != null) {
                    if (adjustment.compareTo(BigDecimal.ZERO) != 0) preview.setAdjustment(adjustment);
                    if (honorarium.compareTo(BigDecimal.ZERO) != 0) preview.setHonorarium(honorarium);
                    if (longevity.compareTo(BigDecimal.ZERO) != 0) preview.setLongevity(longevity);
                    
                    preview.setSssLoan(sssLoan);
                    preview.setHdmfLoan(hdmfLoan);
                    preview.setLoanDeductions(sssLoan.add(hdmfLoan));
                    
                    payrollService.computeTaxableIncomeAndTax(preview); 
                }
            } else {
                preview = payrollService.calculatePayrollPreview(emp, start, end, sssLoan, hdmfLoan, adjustment, honorarium, longevity);
            }
            
            preview.setStatus(status); 
            
            Payroll existing = payrollRepository.findByEmployeeIdAndPayPeriodStartAndPayPeriodEnd(emp.getId(), start, end);
            if (existing != null) {
                preview.setId(existing.getId());
                if (preview.getTeachingPayRecord() != null && existing.getTeachingPayRecord() != null) {
                    preview.getTeachingPayRecord().setId(existing.getTeachingPayRecord().getId());
                }
            }
            
            Payroll savedPayroll = payrollService.savePayroll(preview);
            return ResponseEntity.ok(mapSingleToSafeResponse(savedPayroll)); 
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Backend Error: " + extractErrorMessage(e));
        }
    }

    @PostMapping("/api/payroll/calculate-all")
    @ResponseBody
    public ResponseEntity<?> calculateAll(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "ALL") String department) { 
        try {
            List<Employee> allEmployees = employeeRepository.findAll();
            List<java.util.Map<String, Object>> safeList = new ArrayList<>();
            StringBuilder errors = new StringBuilder();

            List<TeachingLoad> allLoads = teachingLoadRepository.findAll();
            java.util.Set<Long> facultyIds = new java.util.HashSet<>();
            for (TeachingLoad load : allLoads) {
                if (load.getEmployee() != null) facultyIds.add(load.getEmployee().getId());
            }

            for (Employee emp : allEmployees) {
                try {
                    if (!"ALL".equalsIgnoreCase(department) && (emp.getDepartment() == null || !emp.getDepartment().getDepartmentName().equalsIgnoreCase(department))) continue; 
                    
                    BigDecimal zeroDec = new BigDecimal("0.00");
                    Payroll preview;

                    if (facultyIds.contains(emp.getId())) {
                        BigDecimal hourly = emp.getHourlyRate() != null ? emp.getHourlyRate() : BigDecimal.ZERO;
                        preview = payrollService.calculateTeachingPayrollPreview(emp, start, end, 0.0, 0.0, hourly, zeroDec, zeroDec, zeroDec);
                    } else {
                        preview = payrollService.calculatePayrollPreview(emp, start, end, zeroDec, zeroDec, zeroDec, zeroDec, zeroDec);
                    }
                        
                    if(preview != null) safeList.add(mapSingleToSafeResponse(preview));
                } catch (Exception innerE) {
                    innerE.printStackTrace();
                    errors.append("Emp ID ").append(emp.getId()).append(": ").append(extractErrorMessage(innerE)).append("\n");
                }
            }
            
            if (safeList.isEmpty() && errors.length() > 0) {
                return ResponseEntity.status(500).body("Backend Error:\n" + errors.toString());
            }
            return ResponseEntity.ok(safeList);
        } catch (Exception e) { 
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error computing payroll: " + e.getMessage()); 
        }
    }

    @PostMapping("/api/payroll/send-all")
    @ResponseBody
    public ResponseEntity<?> sendAll(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "PUBLISHED") String status,
            @RequestParam(defaultValue = "ALL") String department) { 
        try {
            List<Employee> allEmployees = employeeRepository.findAll();
            List<java.util.Map<String, Object>> safeList = new ArrayList<>();
            StringBuilder errors = new StringBuilder();

            List<TeachingLoad> allLoads = teachingLoadRepository.findAll();
            java.util.Set<Long> facultyIds = new java.util.HashSet<>();
            for (TeachingLoad load : allLoads) {
                if (load.getEmployee() != null) facultyIds.add(load.getEmployee().getId());
            }

            for (Employee emp : allEmployees) {
                try {
                    if (!"ALL".equalsIgnoreCase(department) && (emp.getDepartment() == null || !emp.getDepartment().getDepartmentName().equalsIgnoreCase(department))) continue; 
                    
                    BigDecimal zeroDec = new BigDecimal("0.00"); 
                    Payroll preview;

                    if (facultyIds.contains(emp.getId())) {
                        BigDecimal hourly = emp.getHourlyRate() != null ? emp.getHourlyRate() : BigDecimal.ZERO;
                        preview = payrollService.calculateTeachingPayrollPreview(emp, start, end, 0.0, 0.0, hourly, zeroDec, zeroDec, zeroDec);
                    } else {
                        preview = payrollService.calculatePayrollPreview(emp, start, end, zeroDec, zeroDec, zeroDec, zeroDec, zeroDec);
                    }
                    
                    if (preview != null && preview.getGrossIncome() != null) {
                        preview.setStatus(status); 
                        
                        Payroll existing = payrollRepository.findByEmployeeIdAndPayPeriodStartAndPayPeriodEnd(emp.getId(), start, end);
                        if (existing != null) {
                            preview.setId(existing.getId());
                            if (preview.getTeachingPayRecord() != null && existing.getTeachingPayRecord() != null) {
                                preview.getTeachingPayRecord().setId(existing.getTeachingPayRecord().getId());
                            }
                        }

                        Payroll savedPayroll = payrollService.savePayroll(preview);
                        safeList.add(mapSingleToSafeResponse(savedPayroll));
                    }
                } catch (Exception innerE) {
                    innerE.printStackTrace();
                    errors.append("Emp ID ").append(emp.getId()).append(": ").append(extractErrorMessage(innerE)).append("\n");
                }
            }
            
            if (safeList.isEmpty() && errors.length() > 0) {
                return ResponseEntity.status(500).body("Backend Error:\n" + errors.toString());
            }
            return ResponseEntity.ok(safeList);
        } catch (Exception e) { 
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error saving payroll: " + e.getMessage()); 
        }
    }
    
    @PostMapping("/api/payroll/teaching/calculate-all")
    @ResponseBody
    public ResponseEntity<?> calculateAllTeaching(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end, @RequestParam(defaultValue = "ALL") String department) { 
        try {
            List<TeachingLoad> allLoads = teachingLoadRepository.findAll();
            if (allLoads.isEmpty()) {
                return ResponseEntity.status(400).body("Backend Error: No teaching loads exist in the database. Please assign faculty loads first.");
            }

            java.util.Set<Long> employeeIds = new java.util.HashSet<>();
            for (TeachingLoad load : allLoads) {
                if (load.getEmployee() != null) employeeIds.add(load.getEmployee().getId());
            }
            
            List<java.util.Map<String, Object>> safeList = new ArrayList<>();
            StringBuilder errors = new StringBuilder();

            for (Long empId : employeeIds) {
                Employee emp = employeeRepository.findById(empId).orElse(null);
                if (emp == null) continue;
                
                if (!"ALL".equalsIgnoreCase(department) && (emp.getDepartment() == null || !emp.getDepartment().getDepartmentName().equalsIgnoreCase(department))) continue; 
                try { 
                    BigDecimal hourly = emp.getHourlyRate() != null ? emp.getHourlyRate() : BigDecimal.ZERO;
                    
                    Payroll p = payrollService.calculateTeachingPayrollPreview(emp, start, end, 0.0, 0.0, hourly, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
                    if (p != null && p.getGrossIncome() != null) safeList.add(mapSingleToSafeResponse(p)); 
                } catch (Exception e) {
                    e.printStackTrace();
                    errors.append("Emp ID ").append(emp.getId()).append(": ").append(extractErrorMessage(e)).append("\n");
                }
            }
            
            if (safeList.isEmpty() && errors.length() > 0) {
                return ResponseEntity.status(500).body("Backend Error during Teaching Pay calculation:\n" + errors.toString());
            }
            return ResponseEntity.ok(safeList);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error computing batch teaching: " + e.getMessage());
        }
    }

    @PostMapping("/api/payroll/teaching/send-all")
    @ResponseBody
    public ResponseEntity<?> sendAllTeaching(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start, @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end, @RequestParam(defaultValue = "ALL") String department) { 
        try {
            List<TeachingLoad> allLoads = teachingLoadRepository.findAll();
            
            java.util.Set<Long> employeeIds = new java.util.HashSet<>();
            for (TeachingLoad load : allLoads) {
                if (load.getEmployee() != null) employeeIds.add(load.getEmployee().getId());
            }
            
            List<java.util.Map<String, Object>> savedList = new ArrayList<>();
            StringBuilder errors = new StringBuilder();

            for (Long empId : employeeIds) {
                Employee emp = employeeRepository.findById(empId).orElse(null);
                if (emp == null) continue;
                
                if (!"ALL".equalsIgnoreCase(department) && (emp.getDepartment() == null || !emp.getDepartment().getDepartmentName().equalsIgnoreCase(department))) continue; 
                try {
                    BigDecimal hourly = emp.getHourlyRate() != null ? emp.getHourlyRate() : BigDecimal.ZERO;
                    
                    Payroll p = payrollService.calculateTeachingPayrollPreview(emp, start, end, 0.0, 0.0, hourly, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
                    if (p != null && p.getGrossIncome() != null) {
                        p.setStatus("PUBLISHED");
                        
                        Payroll existing = payrollRepository.findByEmployeeIdAndPayPeriodStartAndPayPeriodEnd(emp.getId(), start, end);
                        if (existing != null) {
                            p.setId(existing.getId());
                        }

                        Payroll savedPayroll = payrollService.savePayroll(p);
                        
                        savedList.add(mapSingleToSafeResponse(savedPayroll));
                    }
                } catch (Exception e) { 
                    e.printStackTrace(); 
                    errors.append("Emp ID ").append(emp.getId()).append(": ").append(extractErrorMessage(e)).append("\n");
                }
            }
            
            if (savedList.isEmpty() && errors.length() > 0) {
                return ResponseEntity.status(500).body("Backend Error during Teaching Pay save:\n" + errors.toString());
            }
            return ResponseEntity.ok(savedList);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error saving batch teaching: " + e.getMessage());
        }
    }

    @PostMapping("/api/payroll/teaching/calculate")
    @ResponseBody
    public ResponseEntity<?> calculateTeaching(
            @RequestParam String employeeQuery,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam Double lec, @RequestParam Double lab, @RequestParam BigDecimal rate,
            @RequestParam BigDecimal holiday, @RequestParam BigDecimal suspension,
            @RequestParam BigDecimal loan) {
        try {
            Employee emp = getEmployeeFromQuery(employeeQuery);
            if (emp == null) return ResponseEntity.badRequest().body("Employee not found");
            Payroll result = payrollService.calculateTeachingPayrollPreview(emp, start, end, lec, lab, rate, holiday, suspension, loan);
            return ResponseEntity.ok(mapSingleToSafeResponse(result)); 
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Backend Error: " + extractErrorMessage(e));
        }
    }

    @PostMapping("/api/payroll/teaching/send")
    @ResponseBody
    public ResponseEntity<?> sendTeaching(
            @RequestParam String employeeQuery,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam Double lec, @RequestParam Double lab, @RequestParam BigDecimal rate,
            @RequestParam BigDecimal holiday, @RequestParam BigDecimal suspension,
            @RequestParam BigDecimal loan,
            @RequestParam(defaultValue = "PUBLISHED") String status) { 
        try {
            Employee emp = getEmployeeFromQuery(employeeQuery);
            if (emp == null) return ResponseEntity.badRequest().body("Employee not found");
            Payroll preview = payrollService.calculateTeachingPayrollPreview(emp, start, end, lec, lab, rate, holiday, suspension, loan);
            preview.setStatus(status); 
            
            Payroll existing = payrollRepository.findByEmployeeIdAndPayPeriodStartAndPayPeriodEnd(emp.getId(), start, end);
            if (existing != null) {
                preview.setId(existing.getId());
            }

            Payroll savedPayroll = payrollService.savePayroll(preview);
            
            return ResponseEntity.ok(mapSingleToSafeResponse(savedPayroll)); 
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Backend Error: " + extractErrorMessage(e));
        }
    }

    @PostMapping("/api/payroll/update-manual")
    @ResponseBody
    public ResponseEntity<?> updateManualAdjustments(
            @RequestParam Long payrollId,
            @RequestParam(defaultValue = "0") Integer lateMinutes,
            @RequestParam(defaultValue = "0") Integer undertimeMinutes,
            @RequestParam(defaultValue = "0") Integer totalAbsences,
            @RequestParam(defaultValue = "0") BigDecimal sssLoan,
            @RequestParam(defaultValue = "0") BigDecimal hdmfLoan,
            @RequestParam(defaultValue = "0") BigDecimal otherLoan,
            @RequestParam(defaultValue = "0") BigDecimal manualAddition,
            @RequestParam(defaultValue = "0") BigDecimal manualDeduction) {
        try {
            Payroll payroll = payrollRepository.findById(payrollId).orElse(null);
            if (payroll == null) return ResponseEntity.badRequest().body("Payroll record not found.");

            payroll.setLateMinutes(lateMinutes);
            payroll.setUndertimeMinutes(undertimeMinutes);
            payroll.setTotalAbsences(totalAbsences);
            payroll.setSssLoan(sssLoan);
            payroll.setHdmfLoan(hdmfLoan);
            payroll.setLoanDeductions(otherLoan);
            payroll.setManualAddition(manualAddition);
            payroll.setManualDeduction(manualDeduction);

            payrollService.computeTaxableIncomeAndTax(payroll);

            BigDecimal gross = payroll.getGrossIncome() != null ? payroll.getGrossIncome() : BigDecimal.ZERO;
            BigDecimal gov = payroll.getGovtContributions() != null ? payroll.getGovtContributions() : BigDecimal.ZERO;
            BigDecimal tax = payroll.getWithholdingTax() != null ? payroll.getWithholdingTax() : BigDecimal.ZERO;
            
            BigDecimal net = gross.subtract(gov).subtract(tax).subtract(sssLoan).subtract(hdmfLoan).subtract(otherLoan).subtract(manualDeduction);
            if (net.compareTo(BigDecimal.ZERO) < 0) net = BigDecimal.ZERO;
            payroll.setNetPay(net);

            Payroll saved = payrollRepository.save(payroll);
            return ResponseEntity.ok(mapSingleToSafeResponse(saved));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error updating adjustment: " + extractErrorMessage(e));
        }
    }

    private java.util.Map<String, Object> mapSingleToSafeResponse(Payroll p) {
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        java.util.Map<String, Object> empMap = new java.util.HashMap<>();
        if (p.getEmployee() != null) {
            empMap.put("id", p.getEmployee().getId());
            empMap.put("firstName", p.getEmployee().getFirstName());
            empMap.put("lastName", p.getEmployee().getLastName());
            empMap.put("employeeStatus", p.getEmployee().getEmployeeStatus()); 
        }
        map.put("employee", empMap);
        map.put("basicSalary", p.getBasicSalary() != null ? p.getBasicSalary() : 0);
        map.put("longevity", p.getLongevity() != null ? p.getLongevity() : 0); 
        map.put("leavePay", p.getLeavePay() != null ? p.getLeavePay() : 0);
        map.put("holidayPay", p.getHolidayPay() != null ? p.getHolidayPay() : 0); 
        map.put("honorarium", p.getHonorarium() != null ? p.getHonorarium() : 0); 
        map.put("overtimePay", p.getOvertimePay() != null ? p.getOvertimePay() : 0);
        map.put("deMinimis", p.getDeMinimis() != null ? p.getDeMinimis() : 0);
        map.put("adminPay", p.getEmployee() != null && p.getEmployee().getAdminPay() != null ? p.getEmployee().getAdminPay() : 0);
        map.put("allowance", p.getAllowance() != null ? p.getAllowance() : 0);
        map.put("adjustment", p.getAdjustment() != null ? p.getAdjustment() : 0);
        
        map.put("manualAddition", p.getManualAddition() != null ? p.getManualAddition() : 0); 
        map.put("manualDeduction", p.getManualDeduction() != null ? p.getManualDeduction() : 0); 
        
        map.put("taxableIncome", p.getTaxableIncome() != null ? p.getTaxableIncome() : 0);
        map.put("totalEarnings", p.getTotalEarnings() != null ? p.getTotalEarnings() : 0); 
        map.put("grossIncome", p.getGrossIncome() != null ? p.getGrossIncome() : 0); 
        map.put("lateDeduction", p.getLateDeduction() != null ? p.getLateDeduction() : 0);
        map.put("undertimeDeduction", p.getUndertimeDeduction() != null ? p.getUndertimeDeduction() : 0);
        map.put("absentDeduction", p.getAbsentDeduction() != null ? p.getAbsentDeduction() : 0);
        map.put("leaveWithoutPay", p.getLeaveWithoutPay() != null ? p.getLeaveWithoutPay() : 0);
        map.put("sssDeduction", p.getSssDeduction() != null ? p.getSssDeduction() : 0);
        map.put("philhealthDeduction", p.getPhilhealthDeduction() != null ? p.getPhilhealthDeduction() : 0);
        map.put("pagibigDeduction", p.getPagibigDeduction() != null ? p.getPagibigDeduction() : 0);
        map.put("sssLoan", p.getSssLoan() != null ? p.getSssLoan() : 0); 
        map.put("hdmfLoan", p.getHdmfLoan() != null ? p.getHdmfLoan() : 0); 
        map.put("loanDeductions", p.getLoanDeductions() != null ? p.getLoanDeductions() : 0); 
        map.put("withholdingTax", p.getWithholdingTax() != null ? p.getWithholdingTax() : 0);
        map.put("netPay", p.getNetPay() != null ? p.getNetPay() : 0);
        
        map.put("sssEmployerShare", p.getSssEmployerShare() != null ? p.getSssEmployerShare() : 0);
        map.put("philhealthEmployerShare", p.getPhilhealthEmployerShare() != null ? p.getPhilhealthEmployerShare() : 0);
        map.put("pagibigEmployerShare", p.getPagibigEmployerShare() != null ? p.getPagibigEmployerShare() : 0);
        
        if (p.getTeachingPayRecord() != null) {
            java.util.Map<String, Object> teachMap = new java.util.HashMap<>();
            teachMap.put("totalLecUnits", p.getTeachingPayRecord().getTotalLecUnits());
            teachMap.put("totalLabUnits", p.getTeachingPayRecord().getTotalLabUnits());
            teachMap.put("excessLecHours", p.getTeachingPayRecord().getExcessLecHours());
            teachMap.put("excessLabHours", p.getTeachingPayRecord().getExcessLabHours());
            teachMap.put("totalExcessHours", p.getTeachingPayRecord().getTotalExcessHours());
            teachMap.put("hourlyRate", p.getTeachingPayRecord().getHourlyRate());
            teachMap.put("adminPay", p.getTeachingPayRecord().getAdminPay());
            teachMap.put("lecPay", p.getTeachingPayRecord().getLecPay());
            teachMap.put("labPay", p.getTeachingPayRecord().getLabPay());
            
            teachMap.put("substituteHours", p.getTeachingPayRecord().getSubstituteHours());
            teachMap.put("substitutePay", p.getTeachingPayRecord().getSubstitutePay());
            teachMap.put("absentDeductionHours", p.getTeachingPayRecord().getAbsentDeductionHours());
            
            BigDecimal extraPays = (p.getTeachingPayRecord().getRlePay() != null ? p.getTeachingPayRecord().getRlePay() : BigDecimal.ZERO)
                .add(p.getTeachingPayRecord().getSgdPay() != null ? p.getTeachingPayRecord().getSgdPay() : BigDecimal.ZERO)
                .add(p.getTeachingPayRecord().getTutorialLecPay() != null ? p.getTeachingPayRecord().getTutorialLecPay() : BigDecimal.ZERO)
                .add(p.getTeachingPayRecord().getTutorialLabPay() != null ? p.getTeachingPayRecord().getTutorialLabPay() : BigDecimal.ZERO);
            teachMap.put("rleAndOtherPay", extraPays);
            
            BigDecimal tpNet = p.getTeachingPayRecord().getTotalTeachingPay() != null ? p.getTeachingPayRecord().getTotalTeachingPay() : BigDecimal.ZERO;
            BigDecimal hrRate = p.getTeachingPayRecord().getHourlyRate() != null ? p.getTeachingPayRecord().getHourlyRate() : BigDecimal.ZERO;
            double safeHrs = p.getTeachingPayRecord().getTotalDeductionHours() != null ? p.getTeachingPayRecord().getTotalDeductionHours() : 0.0;
            BigDecimal tpDed = hrRate.multiply(BigDecimal.valueOf(safeHrs)).setScale(2, RoundingMode.HALF_UP);
            BigDecimal tpGross = tpNet.add(tpDed); 

            teachMap.put("totalTeachingPay", tpNet);
            teachMap.put("teachingPayGross", tpGross); 
            
            teachMap.put("suspensionDeduction", p.getTeachingPayRecord().getSuspensionDeduction() != null ? p.getTeachingPayRecord().getSuspensionDeduction() : BigDecimal.ZERO);
            teachMap.put("appliedSuspensionDates", p.getTeachingPayRecord().getAppliedSuspensionDates());
            
            map.put("teachingPayRecord", teachMap);
        }
        return map;
    }
    
    @GetMapping("/api/payroll/export-report")
    public ResponseEntity<org.springframework.core.io.Resource> exportPayrollRegister() {
        // 1. Fetch payroll data
        List<Payroll> payrolls = payrollRepository.findAll();
        
        // 2. Actually call the ExcelHelper (Notice the // are removed)
        java.io.ByteArrayInputStream in = ExcelHelper.payrollsToExcel(payrolls);
        org.springframework.core.io.InputStreamResource file = new org.springframework.core.io.InputStreamResource(in);
        
        // 3. Return the actual file instead of null
        return ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Payroll_Register_Report.xlsx")
            .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(file); 
    }

    @PostMapping("/api/payroll/email-single")
    @ResponseBody
    public ResponseEntity<?> emailSinglePayslip(
            @RequestParam String employeeQuery,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        try {
            Employee emp = getEmployeeFromQuery(employeeQuery);
            if (emp == null) return ResponseEntity.badRequest().body("Employee not found.");

            Payroll payroll = payrollRepository.findByEmployeeIdAndPayPeriodStartAndPayPeriodEnd(emp.getId(), start, end);
            if (payroll == null) return ResponseEntity.badRequest().body("No saved payroll found for this period. Please generate and save it first.");

            emailService.sendPayslipEmail(emp, payroll);
            return ResponseEntity.ok("Payslip emailed successfully to " + emp.getFirstName() + " " + emp.getLastName() + "!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error sending email: " + extractErrorMessage(e));
        }
    }

    @PostMapping("/api/payroll/email-all")
    @ResponseBody
    public ResponseEntity<?> emailAllPayslips(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "ALL") String department) {
        try {
            List<Employee> allEmployees = employeeRepository.findAll();
            int sentCount = 0;
            StringBuilder errors = new StringBuilder();

            for (Employee emp : allEmployees) {
                if (!"ALL".equalsIgnoreCase(department) && (emp.getDepartment() == null || !emp.getDepartment().getDepartmentName().equalsIgnoreCase(department))) {
                    continue;
                }
                
                Payroll payroll = payrollRepository.findByEmployeeIdAndPayPeriodStartAndPayPeriodEnd(emp.getId(), start, end);
                if (payroll != null) {
                    try {
                        emailService.sendPayslipEmail(emp, payroll);
                        sentCount++;
                    } catch (Exception e) {
                        errors.append("Failed for ").append(emp.getFirstName()).append(": ").append(e.getMessage()).append("\n");
                    }
                }
            }
            
            if (sentCount == 0 && errors.length() == 0) {
                return ResponseEntity.ok("0 sent successfully."); 
            } else if (errors.length() > 0) {
                return ResponseEntity.status(500).body("Sent " + sentCount + " emails, but had errors:\n" + errors.toString());
            }
            
            return ResponseEntity.ok(sentCount + " payslips sent successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error sending emails: " + extractErrorMessage(e));
        }
    }
    
    @GetMapping("/api/payroll/export/pbcom")
    public ResponseEntity<org.springframework.core.io.InputStreamResource> exportPBCOM(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "ALL") String department) {
        
        List<Payroll> allPayrolls = payrollRepository.findAll();
        List<Payroll> filteredPayrolls = new ArrayList<>();

        for (Payroll p : allPayrolls) {
            boolean matchesDate = true;
            if (start != null && end != null) {
                if (p.getPayPeriodStart() == null || p.getPayPeriodStart().isBefore(start) || 
                    p.getPayPeriodEnd() == null || p.getPayPeriodEnd().isAfter(end)) {
                    matchesDate = false;
                }
            }
            
            boolean matchesDept = true;
            if (!"ALL".equalsIgnoreCase(department)) {
                if (p.getEmployee() == null || p.getEmployee().getDepartment() == null || 
                    !p.getEmployee().getDepartment().getDepartmentName().equalsIgnoreCase(department)) {
                    matchesDept = false;
                }
            }
            
            if (matchesDate && matchesDept) {
                filteredPayrolls.add(p);
            }
        }

        ByteArrayInputStream in = ExcelHelper.pbcomExport(filteredPayrolls);
        
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=PBCOM_Bank_Export.xlsx");
        
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new org.springframework.core.io.InputStreamResource(in));
    }

    @PostMapping("/api/payroll/email-selected")
    @ResponseBody
    public ResponseEntity<?> emailSelectedPayslips(
            @RequestParam List<String> employeeIds,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end) {
        try {
            int sentCount = 0;
            StringBuilder errors = new StringBuilder();

            for (String empIdStr : employeeIds) {
                try {
                    Long empId = Long.parseLong(empIdStr);
                    Employee emp = employeeRepository.findById(empId).orElse(null);
                    
                    if (emp != null) {
                        Payroll payroll = payrollRepository.findByEmployeeIdAndPayPeriodStartAndPayPeriodEnd(emp.getId(), start, end);
                        if (payroll != null) {
                            emailService.sendPayslipEmail(emp, payroll);
                            sentCount++;
                        } else {
                            errors.append("No saved payroll record found for ").append(emp.getFirstName()).append(".\n");
                        }
                    }
                } catch (Exception e) {
                    errors.append("Failed for ID ").append(empIdStr).append(": ").append(e.getMessage()).append("\n");
                }
            }
            
            if (sentCount == 0 && errors.length() == 0) {
                return ResponseEntity.ok("0 sent successfully."); 
            } else if (errors.length() > 0) {
                return ResponseEntity.status(500).body("Sent " + sentCount + " emails, but had errors:\n" + errors.toString());
            }
            
            return ResponseEntity.ok(sentCount + " payslips sent successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error sending emails: " + extractErrorMessage(e));
        }
    }

    public static class DeptPayrollGroup {
        private String departmentName; private List<Payroll> payrolls; private BigDecimal totalNetPay;
        public DeptPayrollGroup(String departmentName, List<Payroll> payrolls) {
            this.departmentName = departmentName; this.payrolls = payrolls;
            this.totalNetPay = payrolls.stream().map(p -> p.getNetPay() != null ? p.getNetPay() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        public String getDepartmentName() { return departmentName; } public List<Payroll> getPayrolls() { return payrolls; } public BigDecimal getTotalNetPay() { return totalNetPay; }
    }
}