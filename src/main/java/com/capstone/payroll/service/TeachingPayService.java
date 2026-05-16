package com.capstone.payroll.service;

import com.capstone.payroll.model.Employee;
import com.capstone.payroll.model.Suspension;
import com.capstone.payroll.model.TeachingLoad;
import com.capstone.payroll.model.Attendance;
import com.capstone.payroll.repository.EmployeeRepository;
import com.capstone.payroll.repository.SuspensionRepository;
import com.capstone.payroll.repository.TeachingLoadRepository;
import com.capstone.payroll.repository.TeachingPayRepository;
import com.capstone.payroll.repository.AttendanceRepository;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TeachingPayService {

    @Autowired private TeachingPayRepository teachingPayRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private SuspensionRepository suspensionRepository;
    @Autowired private TeachingLoadRepository teachingLoadRepository;
    @Autowired private AttendanceRepository attendanceRepository;

    public void importTeachingPayFromExcel(MultipartFile file) throws Exception {
        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0); 
        DataFormatter formatter = new DataFormatter(); 

        System.out.println("--- STARTING EXCEL UPLOAD ---");

        for (Row row : sheet) {
            if (row.getRowNum() == 0) continue; 

            String email = formatter.formatCellValue(row.getCell(0)).trim();
            String startStr = formatter.formatCellValue(row.getCell(1)).trim();
            String endStr = formatter.formatCellValue(row.getCell(2)).trim();

            if (email.isEmpty()) continue; 

            Employee employee = employeeRepository.findByEmail(email);
            
            if (employee != null) {
                LocalDate periodStart = LocalDate.parse(startStr);
                LocalDate periodEnd = LocalDate.parse(endStr);
                
                // Fetch valid Suspensions for this cutoff
                List<Suspension> suspensions = suspensionRepository.findAll().stream()
                        .filter(s -> s.getDate() != null && !s.getDate().isBefore(periodStart) && !s.getDate().isAfter(periodEnd))
                        .collect(Collectors.toList());
                        
                // FIX: Passes the String formatted ID properly to the Repository
                List<TeachingLoad> loads = teachingLoadRepository.findByEmployeeEmployeeId(employee.getEmployeeId());
                
                // Fetch Attendance for this cutoff to verify time-ins
                List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndDateBetween(String.valueOf(employee.getId()), periodStart, periodEnd);

                // 1. Set up maps to track hours PER WEEK
                Map<Integer, Double> weeklyLecHours = new HashMap<>();
                Map<Integer, Double> weeklyLabHours = new HashMap<>();
                
                double totalSuspensionDeduction = 0.0;
                
                // Get the hourly rate for monetary deduction math
                double hourlyRate = employee.getHourlyRate() != null ? employee.getHourlyRate().doubleValue() : 0.0;
                
             // Check if employee is Full-Time
                boolean isFullTime = employee.getDesignation() != null && 
                                     employee.getDesignation().getDesignation().equalsIgnoreCase("Full-Time");

                Set<String> appliedSuspensions = new HashSet<>();
                DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm a");
                
                // ==========================================================
                // THE CALENDAR LOOP (Accumulates hours per calendar week)
                // ==========================================================
                for (LocalDate date = periodStart; !date.isAfter(periodEnd); date = date.plusDays(1)) {
                    String dayName = date.getDayOfWeek().name().substring(0, 3).toUpperCase();
                    
                    // Get the week number so we can group the hours weekly
                    int weekNum = date.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear());
                    weeklyLecHours.putIfAbsent(weekNum, 0.0);
                    weeklyLabHours.putIfAbsent(weekNum, 0.0);
                    
                    final LocalDate currentDate = date;
                    Suspension currentSuspension = suspensions.stream()
                            .filter(s -> s.getDate().equals(currentDate))
                            .findFirst()
                            .orElse(null);

                    boolean isPresent = attendances.stream()
                            .anyMatch(a -> a.getDate().equals(currentDate) && a.getTimeIn() != null);

                    for (TeachingLoad load : loads) {
                        if (load.getDayOfWeek() != null && load.getDayOfWeek().toUpperCase().contains(dayName)) {
                            
                            int sections = (load.getNoOfSections() > 0) ? load.getNoOfSections() : 1;
                            
                            // Divide total weekly units by the number of days the class meets per week
                            String[] meetingDays = load.getDayOfWeek().split(",");
                            int daysPerWeek = (meetingDays.length > 0) ? meetingDays.length : 1;

                            double classLecHours = (load.getLectureUnits() / (double) daysPerWeek) * 1.0 * sections;
                            double classLabHours = (load.getLabUnits() / (double) daysPerWeek) * 3.0 * sections;

                            if (currentSuspension != null) {
                                LocalTime suspTime = currentSuspension.getStartTime();
                                LocalTime classStart = load.getStartTime();
                                LocalTime classEnd = load.getEndTime();
                                
                                double suspendedHoursToDeduct = 0.0;

                                if (suspTime == null) {
                                    suspendedHoursToDeduct = classLecHours + classLabHours; 
                                    appliedSuspensions.add(date.toString() + " (Full Day)");
                                } else if (classEnd != null && classEnd.isAfter(suspTime)) {
                                    LocalTime effectiveStart = (classStart != null && classStart.isAfter(suspTime)) ? classStart : suspTime;
                                    long suspendedMinutes = ChronoUnit.MINUTES.between(effectiveStart, classEnd);
                                    suspendedHoursToDeduct = suspendedMinutes / 60.0;
                                    appliedSuspensions.add(date.toString() + " @" + suspTime.format(timeFormatter));
                                }

                                double applicableRate = (classLabHours > 0) ? (hourlyRate * 0.75) : hourlyRate;
                                totalSuspensionDeduction += (suspendedHoursToDeduct * applicableRate);

                                weeklyLecHours.put(weekNum, weeklyLecHours.get(weekNum) + classLecHours);
                                weeklyLabHours.put(weekNum, weeklyLabHours.get(weekNum) + classLabHours);

                            } else if (!isPresent) {
                                // Absent - do not add hours
                            } else {
                                // Present - add hours to the week's total
                                weeklyLecHours.put(weekNum, weeklyLecHours.get(weekNum) + classLecHours);
                                weeklyLabHours.put(weekNum, weeklyLabHours.get(weekNum) + classLabHours);
                            }
                        }
                    }
                }

                // ==========================================================
                // APPLY THE WEEKLY 15-UNIT RULE & 1:2 LAB DEDUCTION
                // ==========================================================
                double finalPayableLecHours = 0.0;
                double finalPayableLabHours = 0.0;

                for (Integer week : weeklyLecHours.keySet()) {
                    double weekLec = weeklyLecHours.get(week);
                    double weekLab = weeklyLabHours.get(week);

                    if (isFullTime) {
                        if (weekLec < 15.0) {
                            // They are short on their 15 lecture units
                            double deficitUnits = 15.0 - weekLec; 
                            
                            // ISOLATED 1:2 RULE: 1 unit deficit = 2 hours of lab deduction
                            double labHoursToDeduct = deficitUnits * 2.0; 
                            
                            // Subtract those precise hours from this week's lab pool
                            double remainingLab = weekLab - labHoursToDeduct;
                            
                            // Anything left over above 0 is payable excess lab
                            finalPayableLabHours += Math.max(0.0, remainingLab);
                            
                            // Lecture is 0 because the required 15 absorbed it all
                            finalPayableLecHours += 0.0; 
                        } else {
                            // They met or exceeded the 15 units!
                            finalPayableLecHours += (weekLec - 15.0); // Only excess is payable
                            finalPayableLabHours += weekLab;         // All lab is payable
                        }
                    } else {
                        // If Part-Time, everything they worked is payable
                        finalPayableLecHours += weekLec;
                        finalPayableLabHours += weekLab;
                    }
                }

                String suspensionDatesStr = String.join(", ", appliedSuspensions);

                // ==========================================================
                // CALL THE REPOSITORY / STORED PROCEDURE
                // ==========================================================
                teachingPayRepository.callGenerateManualTeachingPayroll(
                    employee.getId(), 
                    periodStart, 
                    periodEnd, 
                    finalPayableLecHours,  // Passed as actual payable excess hours
                    finalPayableLabHours,  // Passed as actual payable excess hours
                    BigDecimal.valueOf(hourlyRate), 
                    BigDecimal.ZERO, // Holiday
                    BigDecimal.valueOf(totalSuspensionDeduction), // The exact cash to deduct
                    BigDecimal.ZERO, // Loans
                    suspensionDatesStr // The formatted string for the UI
                );
            }
        }
        workbook.close();
        System.out.println("--- FINISHED EXCEL UPLOAD ---");
    }
}