package com.capstone.payroll.controller;

import com.capstone.payroll.model.Employee;
import com.capstone.payroll.model.Loan;
import com.capstone.payroll.repository.EmployeeRepository;
import com.capstone.payroll.repository.LoanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class LoanController {

    @Autowired private LoanRepository loanRepository;
    @Autowired private EmployeeRepository employeeRepository;

    @GetMapping("/loans")
    public String viewLoans(Model model) {
        model.addAttribute("loans", loanRepository.findAll());
        model.addAttribute("employees", employeeRepository.findAll());
        return "loans";
    }

    @PostMapping("/loans/save")
    public String saveLoan(@RequestParam String employeeId,
                           @RequestParam String loanType,
                           @RequestParam BigDecimal totalAmount,
                           @RequestParam BigDecimal deductionPerCutoff) {
        
        Employee emp = employeeRepository.findById(employeeId).orElse(null);
        if (emp != null) {
            Loan loan = new Loan();
            loan.setEmployee(emp);
            loan.setLoanType(loanType);
            loan.setTotalAmount(totalAmount);
            loan.setRemainingBalance(totalAmount); 
            loan.setDeductionPerCutoff(deductionPerCutoff);
            loan.setDateGranted(LocalDate.now());
            loan.setStatus("ACTIVE");
            
            loanRepository.save(loan);
        }
        return "redirect:/loans";
    }

    // ✅ NEW: Amortization Schedule / Breakdown Generator
    @GetMapping("/api/loans/{id}/breakdown")
    @ResponseBody
    public ResponseEntity<?> getLoanBreakdown(@PathVariable Long id) {
        Loan loan = loanRepository.findById(id).orElse(null);
        if (loan == null) return ResponseEntity.notFound().build();

        List<Map<String, Object>> schedule = new ArrayList<>();
        BigDecimal balance = loan.getTotalAmount(); // Project from the beginning
        LocalDate date = loan.getDateGranted() != null ? loan.getDateGranted() : LocalDate.now();
        String type = loan.getLoanType().toUpperCase();

        // Jump to the first potential cutoff
        if (date.getDayOfMonth() <= 15) {
            date = date.withDayOfMonth(15);
        } else {
            date = date.withDayOfMonth(date.lengthOfMonth());
        }

        int loopGuard = 0; // Prevent infinite loops
        while (balance.compareTo(BigDecimal.ZERO) > 0 && loopGuard < 120) { 
            boolean isFirstCutoff = date.getDayOfMonth() <= 15;
            boolean isSecondCutoff = date.getDayOfMonth() > 15;

            boolean willDeduct = false;
            
            // 1. SSS only deducts on 1st Cut-off (15th)
            if (type.contains("SSS") && isFirstCutoff) {
                willDeduct = true;
            } 
            // 2. PAG-IBIG only deducts on 2nd Cut-off (End of month)
            else if ((type.contains("PAG-IBIG") || type.contains("HDMF")) && isSecondCutoff) {
                willDeduct = true;
            } 
            // 3. Other Loans (Cash Advances) deduct EVERY Cut-off
            else if (!type.contains("SSS") && !type.contains("PAG-IBIG") && !type.contains("HDMF")) {
                willDeduct = true; 
            }

            if (willDeduct) {
                BigDecimal deduction = loan.getDeductionPerCutoff();
                // If remaining balance is smaller than regular deduction, just deduct the rest
                if (deduction.compareTo(balance) > 0) {
                    deduction = balance;
                }
                balance = balance.subtract(deduction);

                Map<String, Object> row = new HashMap<>();
                row.put("no", schedule.size() + 1);
                row.put("date", date.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
                row.put("deduction", deduction);
                row.put("balance", balance);
                schedule.add(row);
            }

            // Advance to the next cutoff date
            if (isFirstCutoff) {
                date = date.withDayOfMonth(date.lengthOfMonth()); // Move to end of month
            } else {
                date = date.plusMonths(1).withDayOfMonth(15); // Move to 15th of next month
            }
            loopGuard++;
        }

        Map<String, Object> response = new HashMap<>();
        response.put("loanType", loan.getLoanType());
        response.put("totalAmount", loan.getTotalAmount());
        response.put("schedule", schedule);

        return ResponseEntity.ok(response);
    }
}