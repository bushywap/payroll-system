package com.capstone.payroll.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.capstone.payroll.model.Attendance;
import com.capstone.payroll.model.TeachingLoad;
import com.capstone.payroll.model.Department;
import com.capstone.payroll.model.Holidays;
import com.capstone.payroll.model.Suspension;
import com.capstone.payroll.dto.AttendanceSummaryDTO;
import com.capstone.payroll.service.AttendanceService;
import com.capstone.payroll.repository.DepartmentRepository;
import com.capstone.payroll.repository.HolidayRepository;
import com.capstone.payroll.repository.SuspensionRepository;
import com.capstone.payroll.repository.TeachingLoadRepository; 

@Controller
public class AttendanceController {

    @Autowired private AttendanceService attendanceService;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private TeachingLoadRepository teachingLoadRepository; 
    
    @Autowired private HolidayRepository holidayRepository; 
    @Autowired private SuspensionRepository suspensionRepository; 

    @GetMapping("/attendance")
    public String viewAttendancePage(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "attendanceType",required = false) String type, 
            Model model) {

        List<Department> departments = departmentRepository.findAll();
        model.addAttribute("departments", departments);

        String activeType = (type != null && !type.trim().isEmpty()) ? type : "EMPLOYEE";

        List<AttendanceSummaryDTO> summaries = attendanceService.getAttendanceSummariesByType(startDate, endDate, activeType);
        model.addAttribute("summaries", summaries);

        model.addAttribute("startDate", startDate);
        model.addAttribute("endDate", endDate);
        model.addAttribute("currentType", activeType); 

        return "attendance";
    }
    
    @GetMapping("/api/attendance/summaries")
    @ResponseBody
    public List<AttendanceSummaryDTO> getAttendanceSummariesApi(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(name = "attendanceType", defaultValue = "EMPLOYEE") String type) { 
        
        return attendanceService.getAttendanceSummariesByType(startDate, endDate, type);
    }

    @GetMapping("/api/attendance/filter")
    @ResponseBody
    public List<Attendance> filterAttendance(@RequestParam(value = "department", required = false) String departmentCode) {
        return attendanceService.getAttendanceByDepartment(departmentCode);
    }

    @GetMapping("/api/attendance/daily/{employeeId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDailyAttendanceForEmployee(
            @PathVariable String employeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        
        List<Attendance> records;
        
        List<Holidays> holidays = holidayRepository.findAll(); 
        List<Suspension> suspensions = suspensionRepository.findAll(); 
        
        if (startDate != null && endDate != null) {
            records = attendanceService.getAttendanceByEmpIdAndDateRange(employeeId, startDate, endDate);
        } else {
            records = attendanceService.getAttendanceByEmployeeId(employeeId);
        }

        List<String> scheduledDays = new ArrayList<>();
        try {
            teachingLoadsForEmployee(employeeId).forEach(load -> { 
                if (load.getDayOfWeek() != null) {
                    String sched = load.getDayOfWeek().toUpperCase();
                    if (sched.contains("MON")) scheduledDays.add("MONDAY");
                    if (sched.contains("TUE")) scheduledDays.add("TUESDAY");
                    if (sched.contains("WED")) scheduledDays.add("WEDNESDAY");
                    if (sched.contains("THU")) scheduledDays.add("THURSDAY");
                    if (sched.contains("FRI")) scheduledDays.add("FRIDAY");
                    if (sched.contains("SAT")) scheduledDays.add("SATURDAY");
                    if (sched.contains("SUN")) scheduledDays.add("SUNDAY");
                }
            });
        } catch (Exception e) {
            System.err.println("Error fetching teaching load schedule for employee: " + employeeId);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("attendance", records);
        response.put("scheduledDays", scheduledDays);
        response.put("holidays", holidays);
        response.put("suspensions", suspensions);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/attendance/update")
    @ResponseBody
    public String updateDailyAttendance(
            @RequestParam Long id, 
            @RequestParam String timeIn, 
            @RequestParam String timeOut) {
        
        Optional<Attendance> recordOpt = attendanceService.getAttendanceById(id);
        if(recordOpt.isPresent()) {
            Attendance record = recordOpt.get();
            LocalTime tIn = LocalTime.parse(timeIn);
            LocalTime tOut = LocalTime.parse(timeOut);
            
            record.setTimeIn(tIn);
            record.setTimeOut(tOut);
            
            LocalTime SHIFT_START = LocalTime.of(8, 0);
            LocalTime SHIFT_END = LocalTime.of(17, 0);
            int STANDARD_WORK_MINS = 480; 
            
            if (tIn.isAfter(LocalTime.of(8, 15))) {
                long minutes = java.time.Duration.between(SHIFT_START, tIn).toMinutes();
                record.setMinutesLate((int) minutes);
            } else {
                record.setMinutesLate(0);
            }
            
            long elapsedMinutes = java.time.Duration.between(tIn, tOut).toMinutes();
            long breakMinutes = (elapsedMinutes > 300) ? 60 : 0; 
            int workMinutes = (int) Math.max(0, elapsedMinutes - breakMinutes);
            record.setTotalHours(workMinutes);
            
            if (workMinutes > STANDARD_WORK_MINS) {
                record.setOvertimeHours(workMinutes - STANDARD_WORK_MINS);
            } else {
                record.setOvertimeHours(0);
            }
            
            if (tOut.isBefore(SHIFT_END)) {
                int earlyMinutes = (int) java.time.Duration.between(tOut, SHIFT_END).toMinutes();
                record.setUndertimeHours(earlyMinutes);
            } else {
                record.setUndertimeHours(0);
            }
            
            attendanceService.saveAttendance(record);
            return "Success";
        }
        return "Record not found";
    }

    // =====================================================================
    // UI HELPER ENDPOINTS (Safely added using String employeeId)
    // =====================================================================

    @PostMapping("/api/attendance/save")
    @ResponseBody
    public ResponseEntity<?> saveAttendance(
            @RequestParam String employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime timeIn,
            @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime timeOut,
            @RequestParam(required = false) String remarks) {
        try {
            Attendance saved = attendanceService.saveOrUpdateAttendance(employeeId, date, timeIn, timeOut, remarks);
            return ResponseEntity.ok(saved);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error saving attendance: " + e.getMessage());
        }
    }

    @PostMapping("/api/attendance/delete")
    @ResponseBody
    public ResponseEntity<?> deleteAttendance(
            @RequestParam String employeeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        try {
            attendanceService.deleteAttendance(employeeId, date);
            return ResponseEntity.ok("Attendance deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error deleting attendance: " + e.getMessage());
        }
    }

    private List<TeachingLoad> teachingLoadsForEmployee(String employeeId) {
        return teachingLoadRepository.findByEmployee_Id(employeeId);
    }
}