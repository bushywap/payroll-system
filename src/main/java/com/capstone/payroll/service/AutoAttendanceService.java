package com.capstone.payroll.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.capstone.payroll.model.Attendance;
import com.capstone.payroll.model.Employee;

@Service
public class AutoAttendanceService {

    @Autowired
    private AttendanceService attendanceService;
    
    @Autowired
    private EmployeeService employeeService;

    private static final LocalTime DEFAULT_TIME_IN = LocalTime.of(8, 0); 
    private static final LocalTime DEFAULT_TIME_OUT = LocalTime.of(17, 0); 
    private static final LocalTime GRACE_PERIOD_END = LocalTime.of(8, 15); 

    @Scheduled(cron = "0 1 0 * * *") 
    public void createDailyAttendanceRecords() {
        LocalDate today = LocalDate.now();
        List<Employee> activeEmployees = employeeService.findActiveEmployees();
        
        for (Employee employee : activeEmployees) {
            Optional<Attendance> existingAttendance = attendanceService
                .getAttendanceByEmployeeIdAndDate(String.valueOf(employee.getEmployeeId()), today);
            
            if (existingAttendance.isEmpty()) {
                attendanceService.createAttendance(
                    String.valueOf(employee.getEmployeeId()), 
                    today, 
                    DEFAULT_TIME_IN, 
                    DEFAULT_TIME_OUT
                );
            }
        }
    }

    @Scheduled(cron = "0 0 18 * * MON-FRI") 
    public void autoTimeOutEmployees() {
        LocalDate today = LocalDate.now();
        List<Employee> activeEmployees = employeeService.findActiveEmployees();
        
        for (Employee employee : activeEmployees) {
            Optional<Attendance> attendance = attendanceService
                .getAttendanceByEmployeeIdAndDate(String.valueOf(employee.getEmployeeId()), today);
            
            if (attendance.isPresent()) {
                Attendance record = attendance.get();
                // Check if they timed in (have late mins) but never got total hours
                if (record.getMinutesLate() != null && (record.getTotalHours() == null || record.getTotalHours() == 0)) {
                    
                    // Work in integers (minutes) instead of doubles. 8 hours = 480 minutes
                    int lateMins = record.getMinutesLate() != null ? record.getMinutesLate() : 0;
                    int defaultMinutes = 480 - lateMins; 
                    
                    record.setTotalHours(Math.max(0, defaultMinutes)); 
                    record.setUndertimeHours(0); // Safely sets to 0 minutes
                    
                    attendanceService.saveAttendance(record);
                }
            }
        }
    }

    public Attendance recordAutoTimeIn(Long employeeId) {
        LocalTime currentTime = LocalTime.now();
        return attendanceService.recordTimeIn(String.valueOf(employeeId), currentTime);
    }

    public Attendance recordAutoTimeOut(Long employeeId) {
        LocalTime currentTime = LocalTime.now();
        return attendanceService.recordTimeOut(String.valueOf(employeeId), currentTime);
    }

    public boolean isWithinGracePeriod(LocalTime timeIn) {
        return timeIn.isBefore(GRACE_PERIOD_END) || timeIn.equals(GRACE_PERIOD_END);
    }

    public double calculateWorkHours(Attendance attendance) {
        return attendance.getTotalHours() != null ? (attendance.getTotalHours() / 60.0) : 0.0;
    }

    public AttendanceStats getAttendanceStats(Long empId, LocalDate startDate, LocalDate endDate) {
        List<Attendance> attendances = attendanceService
            .getAttendanceByEmpIdAndDateRange(String.valueOf(empId), startDate, endDate);
        
        int totalDays = attendances.size();
        int presentDays = 0;
        int lateDays = 0;
        int absentDays = 0;
        double totalHours = 0.0;
        
        for (Attendance attendance : attendances) {
            boolean isPresent = attendance.getMinutesLate() != null || 
                               (attendance.getTotalHours() != null && attendance.getTotalHours() > 0);

            if (isPresent) {
                presentDays++;
                totalHours += calculateWorkHours(attendance);
                
                if (attendance.getMinutesLate() != null && attendance.getMinutesLate() > 0) {
                    lateDays++;
                }
            } else {
                absentDays++;
            }
        }
        
        return new AttendanceStats(totalDays, presentDays, lateDays, absentDays, totalHours);
    }

    public static class AttendanceStats {
        private int totalDays;
        private int presentDays;
        private int lateDays;
        private int absentDays;
        private double totalHours;
        
        public AttendanceStats(int totalDays, int presentDays, int lateDays, int absentDays, double totalHours) {
            this.totalDays = totalDays;
            this.presentDays = presentDays;
            this.lateDays = lateDays;
            this.absentDays = absentDays;
            this.totalHours = totalHours;
        }
        
        public int getTotalDays() { return totalDays; }
        public int getPresentDays() { return presentDays; }
        public int getLateDays() { return lateDays; }
        public int getAbsentDays() { return absentDays; }
        public double getTotalHours() { return totalHours; }
        public double getAttendancePercentage() { 
            return totalDays > 0 ? (double) presentDays / totalDays * 100 : 0.0; 
        }
    }
}