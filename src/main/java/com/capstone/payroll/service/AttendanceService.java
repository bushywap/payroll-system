package com.capstone.payroll.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.capstone.payroll.dto.AttendanceSummaryDTO;
import com.capstone.payroll.model.Attendance;
import com.capstone.payroll.model.Employee;
import com.capstone.payroll.model.Holidays;
import com.capstone.payroll.model.Suspension;
import com.capstone.payroll.model.Leave;
import com.capstone.payroll.model.TeachingLoad;
import com.capstone.payroll.repository.AttendanceRepository;
import com.capstone.payroll.repository.EmployeeRepository;
import com.capstone.payroll.repository.HolidayRepository;
import com.capstone.payroll.repository.SuspensionRepository;
import com.capstone.payroll.repository.LeaveRepository;
import com.capstone.payroll.repository.TeachingLoadRepository; 

@Service
public class AttendanceService {

    @Autowired private AttendanceRepository attendanceRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private LeaveRepository leaveRepository;
    @Autowired private HolidayRepository holidayRepository;
    @Autowired private SuspensionRepository suspensionRepository;
    @Autowired private TeachingLoadRepository teachingLoadRepository; 

    public List<Attendance> getAllAttendance() { 
        List<Attendance> records = attendanceRepository.findAll();
        populateRemarks(records);
        return records;
    }
    
    public List<Attendance> getAttendanceByEmployeeId(String employeeId) { 
        List<Attendance> records = attendanceRepository.findByEmployeeId(employeeId);
        populateRemarks(records);
        return records;
    }
    
    public List<Attendance> getAttendanceByEmpIdAndDateRange(String employeeId, LocalDate startDate, LocalDate endDate) {
        List<Attendance> records = attendanceRepository.findByEmployeeIdAndDateBetween(employeeId, startDate, endDate);
        populateRemarks(records);
        return records;
    }
    
    public List<Attendance> getAttendanceByDepartment(String departmentCode) {
        List<Attendance> records = attendanceRepository.findByEmployeeDepartmentCode(departmentCode);
        populateRemarks(records);
        return records;
    }

    private void populateRemarks(List<Attendance> records) {
        List<Holidays> allHolidays = holidayRepository.findAll();
        List<Suspension> allSuspensions = suspensionRepository.findAll();

        for (Attendance a : records) {
            if (a.getDate() != null) {
                LocalDate recordDate = a.getDate();
                
                boolean isHoliday = allHolidays.stream()
                    .anyMatch(h -> h.getDate() != null && h.getDate().equals(recordDate));
                
                Suspension activeSuspension = allSuspensions.stream()
                    .filter(s -> s.getDate() != null && s.getDate().equals(recordDate))
                    .findFirst().orElse(null);

                if (isHoliday) {
                    a.setRemark("Holiday");
                } else if (activeSuspension != null) {
                    String timeLabel = "";
                    if (activeSuspension.getStartTime() != null) {
                        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("h:mm a");
                        timeLabel = " (" + activeSuspension.getStartTime().format(formatter) + " Onwards)";
                    }

                    if (activeSuspension.getStartTime() != null && a.getTimeIn() == null) {
                        a.setRemark("Partial Suspension" + timeLabel);
                    } else {
                        a.setRemark("Suspension" + timeLabel);
                    }
                } else if ((a.getTotalHours() == null || a.getTotalHours() <= 0) && a.getTimeIn() == null) {
                    a.setRemark("Absent");
                } else if ((a.getTotalHours() == null || a.getTotalHours() <= 0) && a.getTimeIn() != null && a.getTimeOut() == null) {
                    a.setRemark("Absent"); 
                } else {
                    a.setRemark("Present");
                }
            }
        }
    }

    public Attendance recordTimeIn(String employeeId, LocalTime timeIn) {
        LocalDate today = LocalDate.now();
        attendanceRepository.recordTimeInProc(employeeId, timeIn, today);
        List<Attendance> records = attendanceRepository.findByEmployeeIdAndDate(employeeId, today);
        return records.isEmpty() ? null : records.get(0);
    }

    public Attendance recordTimeOut(String employeeId, LocalTime timeOut) {
        LocalDate today = LocalDate.now();
        attendanceRepository.recordTimeOutProc(employeeId, timeOut, today);
        List<Attendance> records = attendanceRepository.findByEmployeeIdAndDate(employeeId, today);
        return records.isEmpty() ? null : records.get(0);
    }

    public List<AttendanceSummaryDTO> getAttendanceSummariesByType(LocalDate start, LocalDate end, String type) {
        List<AttendanceSummaryDTO> summaries = new ArrayList<>();
        String safeType = (type != null && !type.trim().isEmpty()) ? type : "EMPLOYEE";

        List<Employee> employeesToProcess = "TEACHING".equalsIgnoreCase(safeType)
            ? employeeRepository.findTeaching() : employeeRepository.findEmployee();
        employeesToProcess = mergeEacRosterMissingFromDesignationFilter(employeesToProcess);

        List<Holidays> allHolidays = holidayRepository.findAll();
        List<Suspension> allSuspensions = suspensionRepository.findAll();

        for (Employee emp : employeesToProcess) {
            List<String> scheduledDays = new ArrayList<>();
            List<TeachingLoad> teachingLoads = new ArrayList<>();

            if ("TEACHING".equalsIgnoreCase(safeType)) {
                teachingLoads = teachingLoadRepository.findByEmployee_Id(emp.getId());
                
                teachingLoads.forEach(load -> { 
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
            }

            String empIdStr = emp.getAttendanceKey();
            List<Attendance> records = (start != null && end != null) 
                ? attendanceRepository.findByEmployeeIdAndDateBetween(empIdStr, start, end)
                : attendanceRepository.findByEmployeeId(empIdStr);

            int totalMinutesLate = 0, totalUndertimeMins = 0, totalOvertimeMins = 0;
            int absentCount = 0, totalHolidays = 0, totalSuspensions = 0;
            int totalLeavesWithPay = 0, totalLeavesWithoutPay = 0;
            
            List<Leave> approvedLeaves = new ArrayList<>();

            if (start != null && end != null) {
                List<Leave> leaves = leaveRepository.findByEmployee_IdAndStartDateBetween(emp.getId(), start, end);
                for (Leave l : leaves) {
                    if ("Approved".equalsIgnoreCase(l.getStatus())) {
                        approvedLeaves.add(l);
                        if (l.getTotalDays() != null) {
                            String leaveType = l.getLeaveType() != null ? l.getLeaveType().toUpperCase() : "";
                            if (leaveType.contains("WITHOUT PAY") || leaveType.contains("UNPAID") || leaveType.equals("LWOP")) {
                                totalLeavesWithoutPay += l.getTotalDays();
                            } else {
                                totalLeavesWithPay += l.getTotalDays();
                            }
                        }
                    }
                }

                for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                    DayOfWeek day = date.getDayOfWeek();
                    String dayName = day.toString();
                    
                    boolean isRestDay = (day == DayOfWeek.SUNDAY);
                    if ("TEACHING".equalsIgnoreCase(safeType)) {
                        isRestDay = !scheduledDays.contains(dayName);
                    } else if (!scheduledDays.isEmpty() && !scheduledDays.contains(dayName)) {
                        isRestDay = true;
                    }

                    final LocalDate currentDate = date;
                    
                    Holidays activeHoliday = allHolidays.stream()
                        .filter(h -> h.getDate() != null && h.getDate().equals(currentDate))
                        .findFirst().orElse(null);

                    Suspension activeSuspension = allSuspensions.stream()
                        .filter(s -> s.getDate() != null && s.getDate().equals(currentDate))
                        .findFirst().orElse(null);

                    if (activeHoliday != null) {
                        if (!isRestDay) totalHolidays++;
                        continue; 
                    }

                    if (activeSuspension != null) {
                        if (!isRestDay) totalSuspensions++;
                        if (activeSuspension.getStartTime() == null) continue;

                        if ("TEACHING".equalsIgnoreCase(safeType)) {
                            boolean hasClassBeforeSuspension = teachingLoads.stream()
                                .anyMatch(load -> load.getDayOfWeek() != null && 
                                    load.getDayOfWeek().toUpperCase().contains(dayName.substring(0, 3)) && 
                                    load.getStartTime() != null && 
                                    load.getStartTime().isBefore(activeSuspension.getStartTime()));
                            
                            if (!hasClassBeforeSuspension) continue; 
                        } else {
                            LocalTime standardStart = LocalTime.of(8, 0);
                            if (!standardStart.isBefore(activeSuspension.getStartTime())) {
                                continue; 
                            }
                        }
                    }

                    if (isRestDay) continue; 

                    java.util.Optional<Attendance> recordOpt = records.stream()
                        .filter(r -> r.getDate().equals(currentDate)).findFirst();
                        
                    boolean hasWorked = recordOpt.isPresent() && recordOpt.get().getTotalHours() != null && recordOpt.get().getTotalHours() > 0;
                    boolean isOnLeave = approvedLeaves.stream().anyMatch(l -> 
                        !currentDate.isBefore(l.getStartDate()) && !currentDate.isAfter(l.getEndDate())
                    );

                    if (!hasWorked && !isOnLeave) {
                        absentCount++;
                    } else if (hasWorked) {
                        Attendance a = recordOpt.get();
                        if (a.getMinutesLate() != null) totalMinutesLate += a.getMinutesLate();
                        if (a.getUndertimeHours() != null) totalUndertimeMins += a.getUndertimeHours();
                        if (a.getOvertimeHours() != null) totalOvertimeMins += a.getOvertimeHours();
                    }
                }
            }

            AttendanceSummaryDTO dto = new AttendanceSummaryDTO();
            dto.setEmployeeId(emp.getId());
            dto.setName(emp.getFirstName() + " " + emp.getLastName());
            dto.setDepartment(emp.getDepartment() != null ? emp.getDepartment().getDepartmentName() : "N/A");
            dto.setDesignationName(emp.getDesignation() != null ? emp.getDesignation().getDesignation() : "N/A");
            dto.setEmployeeStatus(emp.getEmployeeStatus());
            dto.setPeriodNo(1);
            dto.setTotalLate(String.valueOf(totalMinutesLate));
            dto.setTotalUndertime(String.valueOf(totalUndertimeMins));
            dto.setTotalOT(String.format("%.2f", totalOvertimeMins / 60.0));
            dto.setTotalAbsent(String.valueOf(absentCount));
            dto.setTotalLeaveWithPay(String.valueOf(totalLeavesWithPay));       
            dto.setTotalLeaveWithoutPay(String.valueOf(totalLeavesWithoutPay)); 
            dto.setTotalHoliday(String.valueOf(totalHolidays));     
            dto.setTotalSuspension(String.valueOf(totalSuspensions));  

            summaries.add(dto);
        }
        return summaries;
    }

    public void saveAttendance(Attendance attendance) { attendanceRepository.save(attendance); }
    public Optional<Attendance> getAttendanceById(Long id) { return attendanceRepository.findById(id); }

    public Optional<Attendance> getAttendanceByEmployeeIdAndDate(String employeeId, LocalDate date) {
        List<Attendance> records = attendanceRepository.findByEmployeeIdAndDate(employeeId, date);
        return records.isEmpty() ? Optional.empty() : Optional.of(records.get(0));
    }

    public void createAttendance(String employeeId, LocalDate date, LocalTime timeIn, LocalTime timeOut) {
        Attendance a = new Attendance();
        a.setEmployeeId(employeeId);
        a.setDate(date);
        a.setTimeIn(timeIn);
        a.setTimeOut(timeOut);
        attendanceRepository.save(a);
    }
 // =========================================================================
    // RESTORED METHODS (Updated to use String) FOR UI CONTROLLER COMPATIBILITY
    // =========================================================================
    public Attendance saveOrUpdateAttendance(String employeeId, LocalDate date, LocalTime timeIn, LocalTime timeOut, String remarks) {
        List<Attendance> existingRecords = attendanceRepository.findByEmployeeIdAndDate(employeeId, date);
        Attendance attendance = existingRecords.isEmpty() ? new Attendance() : existingRecords.get(0);
        
        attendance.setEmployeeId(employeeId);
        attendance.setDate(date);
        attendance.setTimeIn(timeIn);
        attendance.setTimeOut(timeOut);
        
        // FIX 1: Changed setRemarks to setRemark (Singular) to match your model
        attendance.setRemark(remarks); 

        // FIX 2: Converted the double logic to standard Integer logic
        if (timeIn != null && timeOut != null) {
            Duration duration = Duration.between(timeIn, timeOut);
            long elapsedMinutes = duration.toMinutes();
            
            // Subtract 60 minutes for lunch break if shift is longer than 5 hours (300 mins)
            long breakMinutes = (elapsedMinutes > 300) ? 60 : 0; 
            int workMinutes = (int) Math.max(0, elapsedMinutes - breakMinutes);
            
            attendance.setTotalHours(workMinutes);
        } else {
            attendance.setTotalHours(0);
        }

        return attendanceRepository.save(attendance);
    }

    public void deleteAttendance(String employeeId, LocalDate date) {
        List<Attendance> existingRecords = attendanceRepository.findByEmployeeIdAndDate(employeeId, date);
        if (!existingRecords.isEmpty()) {
            attendanceRepository.delete(existingRecords.get(0));
        }
    }

    /** Include HR EAC ids (1-00001..) when designation column is not set yet. */
    private List<Employee> mergeEacRosterMissingFromDesignationFilter(List<Employee> fromDesignation) {
        List<Employee> merged = new ArrayList<>(fromDesignation);
        Set<String> seen = new HashSet<>();
        for (Employee e : merged) {
            if (e.getId() != null) seen.add(e.getId());
        }
        for (Employee e : employeeRepository.findActiveEacRoster()) {
            if (e.getId() != null && !seen.contains(e.getId())) {
                merged.add(e);
                seen.add(e.getId());
            }
        }
        return merged;
    }
}