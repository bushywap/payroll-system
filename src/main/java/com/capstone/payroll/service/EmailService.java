package com.capstone.payroll.service;

import com.capstone.payroll.model.Employee;
import com.capstone.payroll.model.Payroll;
import com.itextpdf.html2pdf.HtmlConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private TemplateEngine templateEngine;

    // Made more robust to handle any nulls gracefully
    private BigDecimal addSafely(BigDecimal... amounts) {
        BigDecimal total = BigDecimal.ZERO;
        if (amounts == null) return total;
        for (BigDecimal amt : amounts) {
            if (amt != null) {
                total = total.add(amt);
            }
        }
        return total;
    }

    public void sendPayslipEmail(Employee employee, Payroll payroll) {
        if (employee.getEmail() == null || employee.getEmail().trim().isEmpty()) {
            System.out.println("Skipped email: No email address for " + employee.getFirstName());
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(employee.getEmail());
            helper.setSubject("Official Payslip: " + payroll.getPayPeriodStart() + " to " + payroll.getPayPeriodEnd());
            
            String emailBody = "<p>Dear " + employee.getFirstName() + ",</p>"
                             + "<p>Please find attached your official payslip for the period of <strong>" 
                             + payroll.getPayPeriodStart() + " to " + payroll.getPayPeriodEnd() + "</strong>.</p>"
                             + "<p>If you have any questions or discrepancies, please contact the HR department.</p>"
                             + "<br><p>Best regards,<br><strong>EAC Payroll Department</strong></p>";
            helper.setText(emailBody, true);

            Context context = new Context();
            context.setVariable("emp", employee);
            context.setVariable("p", payroll);

            // 1. TAXABLE INCOME 
            BigDecimal teachingPay = (payroll.getTeachingPayRecord() != null && payroll.getTeachingPayRecord().getTotalTeachingPay() != null) 
                    ? payroll.getTeachingPayRecord().getTotalTeachingPay() : BigDecimal.ZERO;
            
            BigDecimal totalTaxable = addSafely(
                payroll.getBasicSalary(), 
                teachingPay, 
                payroll.getHonorarium(),
                payroll.getAdjustment(),
                payroll.getOvertimePay(), 
                payroll.getHolidayPay(), 
                payroll.getLeavePay(),
                payroll.getAllowance(),
                payroll.getRelocationPay()
            );
            
            // 2. NON-TAXABLE INCOME 
            BigDecimal totalNonTaxable = addSafely(
                payroll.getDeMinimis(), 
                payroll.getLongevity(), 
                payroll.getCashGift(),
                payroll.getIncentive()
            );
            
            // 3. STATUTORY DEDUCTIONS
            BigDecimal totalStatutory = addSafely(
                payroll.getWithholdingTax(), 
                payroll.getSssDeduction(), 
                payroll.getPhilhealthDeduction(), 
                payroll.getPagibigDeduction()
            );
            
            // 4. ATTENDANCE PENALTIES
            BigDecimal totalPenalties = addSafely(
                payroll.getAbsentDeduction(), 
                payroll.getLateDeduction(), 
                payroll.getUndertimeDeduction()
            );
            
            // 5. LOANS SUMMARY
            BigDecimal totalLoans = addSafely(
                payroll.getSssLoan(), 
                payroll.getHdmfLoan(), 
                payroll.getLoanDeductions()
            );
            
            // 6. GRAND TOTAL DEDUCTIONS
            BigDecimal grandTotalDeductions = addSafely(
                totalStatutory, 
                totalPenalties, 
                totalLoans,
                payroll.getLeaveWithoutPay() 
            );

            context.setVariable("totalTaxable", totalTaxable);
            context.setVariable("totalNonTaxable", totalNonTaxable);
            context.setVariable("totalStatutory", totalStatutory);
            context.setVariable("totalPenalties", totalPenalties);
            context.setVariable("totalLoans", totalLoans);
            context.setVariable("grandTotalDeductions", grandTotalDeductions);

            String htmlContent = templateEngine.process("payslip_pdf", context);
            ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream();
            HtmlConverter.convertToPdf(htmlContent, pdfOutputStream);
            byte[] pdfBytes = pdfOutputStream.toByteArray();

            String fileName = "Payslip_" + employee.getLastName() + "_" + payroll.getPayPeriodEnd() + ".pdf";
            helper.addAttachment(fileName, new ByteArrayResource(pdfBytes));

            mailSender.send(message);
            System.out.println("PDF Payslip sent successfully to: " + employee.getEmail());

        } catch (Exception e) { // Catch Exception broadly so Thymeleaf template errors are caught
            System.err.println("Failed to generate or send PDF email to " + employee.getEmail());
            e.printStackTrace();
        }
    }
}