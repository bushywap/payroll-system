package com.capstone.payroll.service;

import com.capstone.payroll.model.Employee;
import com.capstone.payroll.model.Payroll;
import com.capstone.payroll.model.TeachingLoad;
import com.capstone.payroll.model.TeachingPay;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExcelHelper {
    public static String TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    public static boolean hasExcelFormat(MultipartFile file) {
        return TYPE.equals(file.getContentType());
    }

    // ==========================================
    // PBCOM EXPORT LOGIC
    // ==========================================
    public static ByteArrayInputStream pbcomExport(List<Payroll> payrolls) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("PBCOM Export");

            // --- THE FIX: Create a strict numeric style aligned to the LEFT ---
            CellStyle netPayStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            netPayStyle.setDataFormat(format.getFormat("#,##0.00")); // Forces format: 1,234.50
            netPayStyle.setAlignment(HorizontalAlignment.LEFT); // Keeps it pinned to the left

            // Optional: Make headers bold to look clean
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            String[] headers = {"Account Number", "Account Name", "Net Pay", "Description"};
            
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < headers.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(headers[col]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (Payroll payroll : payrolls) {
                Row row = sheet.createRow(rowIdx++);
                Employee emp = payroll.getEmployee();
                
                // 1. Account Number
                String accNum = (emp != null && emp.getBankAccountNumber() != null) ? emp.getBankAccountNumber() : "";
                row.createCell(0).setCellValue(accNum);
                
                // 2. Name
                String accountName = emp != null ? emp.getLastName() + ", " + emp.getFirstName() : "";
                row.createCell(1).setCellValue(accountName.toUpperCase());
                
                // 3. Net Pay
                double netPay = (payroll.getNetPay() != null) ? payroll.getNetPay().doubleValue() : 0.0;
                Cell netPayCell = row.createCell(2);
                netPayCell.setCellValue(netPay);
                netPayCell.setCellStyle(netPayStyle); // Applies the left-aligned format
                
                // 4. Description
                String period = (payroll.getPayPeriodStart() != null && payroll.getPayPeriodEnd() != null) ? 
                        payroll.getPayPeriodStart() + " to " + payroll.getPayPeriodEnd() : "Salary";
                row.createCell(3).setCellValue("Salary: " + period);
            }

            // --- Lock the column widths so it never gets squished ---
            sheet.setColumnWidth(0, 25 * 256); // Account Number
            sheet.setColumnWidth(1, 40 * 256); // Name
            sheet.setColumnWidth(2, 18 * 256); // Net Pay
            sheet.setColumnWidth(3, 35 * 256); // Description

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
            
        } catch (IOException e) {
            // Using a standard exception for better error handling
            throw new RuntimeException("Failed to generate PBCOM Excel file: " + e.getMessage());
        }
    }

    // ==========================================
    // PAYROLL REGISTER EXPORT LOGIC (NEW)
    // ==========================================
    public static ByteArrayInputStream payrollsToExcel(List<Payroll> payrolls) {
        try (Workbook workbook = new XSSFWorkbook(); 
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
             
            Sheet sheet = workbook.createSheet("Payroll Register");
            
            // Includes correct institutional mapping separating Admin Pay and basic pay 
            String[] headers = { "Employee ID", "Name", "Department", "Basic Salary", "Admin Pay", "Gross Income", "SSS (5%)", "PhilHealth", "Pag-IBIG", "Net Pay" };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            int rowIdx = 1;
            for (Payroll p : payrolls) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(p.getEmployee() != null ? p.getEmployee().getId().toString() : "");
                row.createCell(1).setCellValue(p.getEmployee() != null ? p.getEmployee().getFirstName() + " " + p.getEmployee().getLastName() : "");
                row.createCell(2).setCellValue(p.getEmployee() != null && p.getEmployee().getDepartment() != null ? p.getEmployee().getDepartment().getDepartmentName() : "");
                row.createCell(3).setCellValue(p.getBasicSalary() != null ? p.getBasicSalary().doubleValue() : 0.0);
                row.createCell(4).setCellValue(p.getEmployee() != null && p.getEmployee().getAdminPay() != null ? p.getEmployee().getAdminPay().doubleValue() : 0.0);
                row.createCell(5).setCellValue(p.getGrossIncome() != null ? p.getGrossIncome().doubleValue() : 0.0);
                row.createCell(6).setCellValue(p.getSssDeduction() != null ? p.getSssDeduction().doubleValue() : 0.0);
                row.createCell(7).setCellValue(p.getPhilhealthDeduction() != null ? p.getPhilhealthDeduction().doubleValue() : 0.0);
                row.createCell(8).setCellValue(p.getPagibigDeduction() != null ? p.getPagibigDeduction().doubleValue() : 0.0);
                row.createCell(9).setCellValue(p.getNetPay() != null ? p.getNetPay().doubleValue() : 0.0);
            }
            
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel file: " + e.getMessage());
        }
    }

    // ==========================================
    // TEACHING PAYROLL EXPORT LOGIC (NEW)
    // ==========================================
    public static ByteArrayInputStream teachingPaysToExcel(List<TeachingPay> teachingPays) {
        try (Workbook workbook = new XSSFWorkbook(); 
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
             
            Sheet sheet = workbook.createSheet("Teaching Payroll");
            
            String[] headers = { "Employee", "Period", "Total Lec Units", "Total Lab Units", "Calculated Lab Hours", "Hourly Rate", "Total Teaching Pay" };
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            int rowIdx = 1;
            for (TeachingPay tp : teachingPays) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(tp.getEmployee() != null ? tp.getEmployee().getFirstName() + " " + tp.getEmployee().getLastName() : "");
                row.createCell(1).setCellValue(tp.getPeriodStart() + " to " + tp.getPeriodEnd());
                row.createCell(2).setCellValue(tp.getTotalLecUnits());
                row.createCell(3).setCellValue(tp.getTotalLabUnits());
                // Computes the column properly setting 1 Lab Unit to 3 hours
                row.createCell(4).setCellValue(tp.getTotalLabUnits() * 3.0); 
                row.createCell(5).setCellValue(tp.getHourlyRate() != null ? tp.getHourlyRate().doubleValue() : 0.0);
                row.createCell(6).setCellValue(tp.getTotalTeachingPay() != null ? tp.getTotalTeachingPay().doubleValue() : 0.0);
            }
            
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate Excel file: " + e.getMessage());
        }
    }

    // ==========================================
    // EXCEL TO TEACHING LOAD IMPORT LOGIC
    // ==========================================
    public static List<TeachingLoad> excelToTeachingLoad(InputStream is) { 
        try {
            Workbook workbook = new XSSFWorkbook(is);
            Sheet sheet = workbook.getSheetAt(0);
            Iterator<Row> rows = sheet.iterator();

            List<TeachingLoad> teachingLoads = new ArrayList<>(); 
            int rowNumber = 0;

            while (rows.hasNext()) {
                Row currentRow = rows.next();
                
                if (rowNumber == 0) {
                    rowNumber++;
                    continue;
                }

                if (currentRow.getCell(0) == null || currentRow.getCell(0).getCellType() == CellType.BLANK) {
                    break; 
                }

                TeachingLoad load = new TeachingLoad();

                String empId = getStringValue(currentRow.getCell(0));
                if (empId == null || empId.isBlank()) {
                    long legacyNum = (long) getNumericValue(currentRow.getCell(0));
                    empId = String.format("1-%05d", legacyNum);
                } else {
                    empId = empId.trim();
                    if (empId.matches("\\d{1,3}")) {
                        empId = String.format("1-%05d", Long.parseLong(empId));
                    }
                }
                Employee employee = new Employee();
                employee.setId(empId);
                load.setEmployee(employee);

                load.setSubject(getStringValue(currentRow.getCell(1)));
                load.setSubjectCode(getStringValue(currentRow.getCell(2)));

                int sections = (int) getNumericValue(currentRow.getCell(3));
                load.setNoOfSections(sections > 0 ? sections : 1);

                load.setDayOfWeek(getStringValue(currentRow.getCell(4)));
                load.setTimeSchedule(getStringValue(currentRow.getCell(5)));
                load.setRoom(getStringValue(currentRow.getCell(6)));

                int lecUnits = (int) getNumericValue(currentRow.getCell(8));
                load.setLectureUnits(lecUnits);

                int labUnits = (int) getNumericValue(currentRow.getCell(9));
                load.setLabUnits(labUnits);

                // EXPLICIT CALCULATION FOR DATABASE COLUMNS
                // Rule: 1 Lab Unit = 3 Hours
                double computedLecHours = lecUnits * 1.0; 
                double computedLabHours = labUnits * 3.0;
                
                load.setLecHours(computedLecHours);
                load.setLabHours(computedLabHours);
                load.setTotalHours(computedLecHours + computedLabHours);

                teachingLoads.add(load); 
            }

            workbook.close();
            return teachingLoads; 
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Excel file. Please check column formats: " + e.getMessage());
        }
    }

    private static double getNumericValue(Cell cell) {
        if (cell == null) return 0.0;
        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private static String getStringValue(Cell cell) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            long val = (long) cell.getNumericCellValue();
            return String.valueOf(val);
        }
        return "";
    }
}