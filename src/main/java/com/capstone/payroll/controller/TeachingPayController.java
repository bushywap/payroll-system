package com.capstone.payroll.controller;

import com.capstone.payroll.model.Department;
import com.capstone.payroll.model.Employee;
import com.capstone.payroll.model.TeachingPay;
import com.capstone.payroll.model.TeachingLoad;
import com.capstone.payroll.model.SubstituteRecord;
import com.capstone.payroll.model.Payroll;
import com.capstone.payroll.model.Attendance;
import com.capstone.payroll.repository.DepartmentRepository;
import com.capstone.payroll.repository.EmployeeRepository;
import com.capstone.payroll.repository.TeachingPayRepository;
import com.capstone.payroll.repository.TeachingLoadRepository;
import com.capstone.payroll.repository.SubstituteRecordRepository;
import com.capstone.payroll.repository.AttendanceRepository;
import com.capstone.payroll.service.ExcelHelper;
import com.capstone.payroll.service.TeachingLoadService;
import com.capstone.payroll.service.PayrollService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class TeachingPayController {

    @Autowired private TeachingLoadService teachingLoadService;
    @Autowired private TeachingLoadRepository teachingLoadRepository;
    @Autowired private SubstituteRecordRepository substituteRecordRepository;
    @Autowired private DepartmentRepository departmentRepository;
    @Autowired private TeachingPayRepository teachingPayRepository;
    @Autowired private EmployeeRepository employeeRepository;
    @Autowired private AttendanceRepository attendanceRepository; // <--- ADDED ATTENDANCE REPO
    @Autowired private PayrollService payrollService; 

    private void syncTeachingPaysForPeriod(LocalDate start, LocalDate end) {
        List<TeachingLoad> allLoads = teachingLoadRepository.findAll();
        java.util.Set<Employee> facultySet = new java.util.HashSet<>();
        for (TeachingLoad tl : allLoads) {
            if (tl.getEmployee() != null) facultySet.add(tl.getEmployee());
        }

        for (Employee emp : facultySet) {
            try {
                BigDecimal hourly = emp.getHourlyRate() != null ? emp.getHourlyRate() : BigDecimal.ZERO;
                
                // ====================================================================
                // NEW: AUTOMATIC ABSENCE DETECTION LOGIC
                // ====================================================================
                double totalAbsentHours = 0.0;
                List<TeachingLoad> loads = teachingLoadRepository.findByEmployeeId(emp.getId());
                List<Attendance> attendances = attendanceRepository.findByEmployeeIdAndDateBetween(String.valueOf(emp.getId()), start, end);
                
                for (LocalDate date = start; !date.isAfter(end); date = date.plusDays(1)) {
                    String dayName = date.getDayOfWeek().name().substring(0, 3).toUpperCase();
                    
                    boolean hasClass = false;
                    double hoursForDay = 0.0;
                    
                    // Check if faculty has a class on this specific day
                    for (TeachingLoad load : loads) {
                        if (load.getDayOfWeek() != null && load.getDayOfWeek().toUpperCase().contains(dayName)) {
                            hasClass = true;
                            int sections = load.getNoOfSections() > 0 ? load.getNoOfSections() : 1;
                            String[] meetingDays = load.getDayOfWeek().split(",");
                            int daysPerWeek = meetingDays.length > 0 ? meetingDays.length : 1;
                            
                            double classLec = (load.getLectureUnits() / daysPerWeek) * 1.0 * sections;
                            double classLab = (load.getLabUnits() / daysPerWeek) * 3.0 * sections;
                            hoursForDay += (classLec + classLab);
                        }
                    }
                    
                    // If they had a class, verify they timed in
                    if (hasClass) {
                        final LocalDate currentDate = date;
                        boolean isPresent = attendances.stream().anyMatch(a -> a.getDate().equals(currentDate) && a.getTimeIn() != null);
                        
                        if (!isPresent) {
                            totalAbsentHours += hoursForDay; // Add missed hours to penalty
                        }
                    }
                }
                // ====================================================================
                
                Payroll preview = payrollService.calculateTeachingPayrollPreview(emp, start, end, 0.0, 0.0, hourly, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
                
                if (preview != null && preview.getTeachingPayRecord() != null) {
                    TeachingPay generatedPay = preview.getTeachingPayRecord();
                    
                    // Apply the mathematically calculated absent hours to the entity
                    generatedPay.setAbsentDeductionHours(totalAbsentHours);

                    TeachingPay existingPay = teachingPayRepository
                            .findByEmployeeIdAndPeriodStartAndPeriodEnd(emp.getId(), start, end)
                            .orElse(null);

                    if (existingPay != null) {
                        generatedPay.setId(existingPay.getId());
                        
                        generatedPay.setMakeUpLecHours(existingPay.getMakeUpLecHours());
                        generatedPay.setMakeUpLabHours(existingPay.getMakeUpLabHours());
                        
                        generatedPay.setAdjustmentHours(existingPay.getAdjustmentHours());
                        generatedPay.setAdjustmentPay(existingPay.getAdjustmentPay());
                        generatedPay.setAdjustmentRemarks(existingPay.getAdjustmentRemarks());
                        
                        generatedPay.setDeductionHours(existingPay.getDeductionHours());
                        
                        // We do NOT pull absentDeductionHours from existing, because we want it live-synced!
                        
                        generatedPay.setSgdHours(existingPay.getSgdHours());
                        generatedPay.setTutorialLecHours(existingPay.getTutorialLecHours());
                        generatedPay.setTutorialLabHours(existingPay.getTutorialLabHours());
                        generatedPay.setHonorarium(existingPay.getHonorarium());
                        generatedPay.setAdminPay(existingPay.getAdminPay());
                        generatedPay.setSupplementalPay(existingPay.getSupplementalPay());

                        generatedPay.calculatePay();
                    }

                    teachingPayRepository.save(generatedPay);
                }
            } catch (Exception e) {
                System.err.println("Error verifying attendance for Teaching Pay: " + emp.getId() + " - " + e.getMessage());
            }
        }
    }

    private void syncAllTeachingPays() {
        LocalDate today = LocalDate.now();
        LocalDate start, end;
        
        if (today.getDayOfMonth() <= 15) {
            start = today.withDayOfMonth(1);
            end = today.withDayOfMonth(15);
        } else {
            start = today.withDayOfMonth(16);
            end = today.withDayOfMonth(today.lengthOfMonth());
        }
        syncTeachingPaysForPeriod(start, end);
    }

    @GetMapping("/teaching_payroll")
    public String viewTeachingPayrollProcess(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            Model model) {
        
        if (start != null && end != null && !start.isEmpty() && !end.isEmpty()) {
            syncTeachingPaysForPeriod(LocalDate.parse(start), LocalDate.parse(end));
        } else {
            syncAllTeachingPays(); 
        }
        
        List<TeachingPay> allRecords = teachingPayRepository.findAll();
        List<TeachingPay> payrollRecords = new ArrayList<>();
        String latestCutOff = "N/A";
        
        if (!allRecords.isEmpty()) {
            TeachingPay tp = allRecords.get(allRecords.size() - 1);
            if(tp.getPeriodStart() != null && tp.getPeriodEnd() != null) {
                 latestCutOff = tp.getPeriodStart().toString() + " to " + tp.getPeriodEnd().toString();
            }
        }

        for (TeachingPay tp : allRecords) {
            if (tp.getTotalTeachingPay() != null) {
                payrollRecords.add(tp);
            }
        }
        
        model.addAttribute("cutOffDate", latestCutOff);
        model.addAttribute("payrolls", payrollRecords);
        return "teaching_payroll"; 
    }

    @GetMapping("/teaching_load")
    public String viewTeachingPayPage(Model model) {
        List<Department> departments = departmentRepository.findAll();
        model.addAttribute("departments", departments);

        List<TeachingLoad> allLoads = teachingLoadRepository.findAll();
        Map<Long, TeachingSummaryDTO> summaryMap = new HashMap<>();

        for (TeachingLoad load : allLoads) {
            if (load.getEmployee() == null) continue;
            
            Long ownerId = load.getEmployee().getId();
            TeachingSummaryDTO ownerDto = getOrCreateDto(summaryMap, load.getEmployee());

            int sections = load.getNoOfSections() > 0 ? load.getNoOfSections() : 1;
            
            ownerDto.setLectureUnits(ownerDto.getLectureUnits() + (load.getLectureUnits() * sections));
            ownerDto.setLabUnits(ownerDto.getLabUnits() + (load.getLabUnits() * sections));
            ownerDto.setTotalRleHours(ownerDto.getTotalRleHours() + (load.getRleHours() != null ? load.getRleHours() : 0));
            
            summaryMap.put(ownerId, ownerDto);
        }
        
        for (SubstituteRecord sub : substituteRecordRepository.findAll()) {
            double hrs = sub.getHoursRendered() != null ? sub.getHoursRendered() : 0.0;
            
            if (sub.getOriginalFaculty() != null) {
                TeachingSummaryDTO ownerDto = getOrCreateDto(summaryMap, sub.getOriginalFaculty());
                ownerDto.setAbsentDeductionHours(ownerDto.getAbsentDeductionHours() + hrs);
                summaryMap.put(sub.getOriginalFaculty().getId(), ownerDto);
            }
            
            if (sub.getSubstituteFaculty() != null) {
                TeachingSummaryDTO subDto = getOrCreateDto(summaryMap, sub.getSubstituteFaculty());
                subDto.setSubstituteHours(subDto.getSubstituteHours() + hrs);
                summaryMap.put(sub.getSubstituteFaculty().getId(), subDto);
            }
        }

        for (TeachingSummaryDTO dto : summaryMap.values()) {
            boolean isPartTime = dto.getEmploymentStatus() != null && dto.getEmploymentStatus().toLowerCase().contains("part");
            double currentTotalUnits = dto.getLectureUnits() + dto.getLabUnits();
            
            double reqUnits = isPartTime ? 0.0 : 15.0; 
            double flexiThreshold = 20.0;

            dto.setWorkloadClassification(isPartTime ? "Part-Time" : (currentTotalUnits <= flexiThreshold ? "Full-Time Flexi" : "Full-Time"));

            if (currentTotalUnits <= reqUnits && !isPartTime) {
                dto.setExcessLecUnits(0); dto.setExcessLabUnits(0); 
                dto.setExcessLecHours(0); dto.setExcessLabHours(0);
                
                dto.setTotalLecHours(0); 
                dto.setTotalLabHours(0); 
                dto.setTotalExcessHours(dto.getSubstituteHours() - dto.getAbsentDeductionHours()); 
            } else {
                double deductedLec = Math.min(dto.getLectureUnits(), reqUnits);
                dto.setExcessLecUnits(dto.getLectureUnits() - deductedLec);
                double remainingReq = reqUnits - deductedLec;

                double labHoursNeeded = remainingReq * 2.0; 
                double totalLabHours = dto.getLabUnits() * 3.0; 
                
                double deductedLabHours = Math.min(totalLabHours, labHoursNeeded);
                dto.setExcessLabHours(totalLabHours - deductedLabHours);
                
                dto.setExcessLabUnits(dto.getExcessLabHours() / 3.0);
                dto.setExcessLecHours(dto.getExcessLecUnits() * 1.0); 
                
                dto.setTotalLecHours(dto.getExcessLecHours());
                dto.setTotalLabHours(dto.getExcessLabHours());
                
                dto.setTotalExcessHours(dto.getExcessLecHours() + dto.getExcessLabHours() + dto.getSubstituteHours() - dto.getAbsentDeductionHours());
            }
        }

        model.addAttribute("teachingPaySummaries", new ArrayList<>(summaryMap.values()));
        return "teaching_load"; 
    }

    private TeachingSummaryDTO getOrCreateDto(Map<Long, TeachingSummaryDTO> map, Employee emp) {
        TeachingSummaryDTO dto = map.getOrDefault(emp.getId(), new TeachingSummaryDTO());
        if (dto.getEmployeeId() == null) {
            dto.setEmployeeId(emp.getId());
            dto.setName(emp.getFirstName() + " " + emp.getLastName());
            dto.setDepartment(emp.getDepartment() != null ? emp.getDepartment().getDepartmentName() : "N/A");
            dto.setEmploymentStatus(emp.getEmployeeStatus() != null ? emp.getEmployeeStatus() : "N/A");
            dto.setDateStart(LocalDate.now().withDayOfMonth(1).toString()); 
            dto.setDateEnd(LocalDate.now().withDayOfMonth(15).toString());
        }
        return dto;
    }

    @GetMapping("/teaching-pay/api/get-load")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getEmployeeLoad(@RequestParam String query) {
        List<Employee> emps = employeeRepository.searchByIdOrName(query);
        if (emps.isEmpty()) return ResponseEntity.notFound().build();
        Employee emp = emps.get(0);
        List<TeachingLoad> loads = teachingLoadRepository.findByEmployeeId(emp.getId());
        
        double totalLec = 0.0; 
        double totalLab = 0.0;
        
        for (TeachingLoad load : loads) {
            int sections = load.getNoOfSections() > 0 ? load.getNoOfSections() : 1;
            totalLec += (load.getLectureUnits() * sections); 
            totalLab += (load.getLabUnits() * sections);
        }
        
        Map<String, Object> data = new HashMap<>();
        data.put("totalLec", totalLec); data.put("totalLab", totalLab);
        data.put("rate", emp.getHourlyRate() != null ? emp.getHourlyRate() : BigDecimal.ZERO); 
        data.put("name", emp.getFirstName() + " " + emp.getLastName());
        return ResponseEntity.ok(data);
    }

    @PostMapping("/teaching-pay/upload-teaching-load")
    public String uploadFile(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        if (ExcelHelper.hasExcelFormat(file)) {
            try {
                teachingLoadService.saveFromExcel(file);
                redirectAttributes.addFlashAttribute("message", "Uploaded successfully.");
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("error", "Upload Error: " + e.getMessage());
            }
        } else {
            redirectAttributes.addFlashAttribute("error", "Upload a valid Excel file.");
        }
        return "redirect:/teaching_load"; 
    }

    @GetMapping("/teaching-pay/api/payslip/{empId}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTeachingPayslip(@PathVariable Long empId) {
        syncAllTeachingPays(); 
        
        List<TeachingPay> records = teachingPayRepository.findByEmployeeIdOrderByPeriodEndDesc(empId);
        if (records.isEmpty()) return ResponseEntity.notFound().build();
        
        TeachingPay tp = records.get(0);
        Map<String, Object> data = new HashMap<>();
        
        data.put("employeeName", tp.getEmployee().getFirstName() + " " + tp.getEmployee().getLastName());
        data.put("periodStart", tp.getPeriodStart()); data.put("periodEnd", tp.getPeriodEnd());
        data.put("totalLecUnits", tp.getTotalLecUnits()); data.put("totalLabUnits", tp.getTotalLabUnits());
        data.put("excessLecUnits", tp.getExcessLecUnits()); data.put("excessLabUnits", tp.getExcessLabUnits());
        data.put("excessLecHours", tp.getExcessLecHours()); data.put("excessLabHours", tp.getExcessLabHours());
        data.put("totalExcessHours", tp.getTotalExcessHours());
        data.put("workloadClassification", tp.getWorkloadClassification());
        data.put("totalRleHours", tp.getTotalRleHours()); data.put("rlePay", tp.getRlePay());
        data.put("substituteHours", tp.getSubstituteHours()); data.put("substitutePay", tp.getSubstitutePay());
        data.put("absentDeductionHours", tp.getAbsentDeductionHours()); 
        
        try { data.put("absentDeductionPay", tp.getClass().getMethod("getAbsentDeductionPay").invoke(tp)); } catch(Exception e) { data.put("absentDeductionPay", 0.0); }
        
        data.put("hourlyRate", tp.getHourlyRate()); data.put("labRate", tp.getLabRate());
        data.put("lecPay", tp.getLecPay()); data.put("labPay", tp.getLabPay());
        data.put("totalTeachingPay", tp.getTotalTeachingPay());
        
        data.put("suspensionDeduction", tp.getSuspensionDeduction() != null ? tp.getSuspensionDeduction() : BigDecimal.ZERO);
        data.put("appliedSuspensionDates", tp.getAppliedSuspensionDates());
        
        return ResponseEntity.ok(data);
    }

    @GetMapping("/teaching-pay/api/get-process/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTeachingPayById(@PathVariable Long id) {
        return teachingPayRepository.findById(id).map(tp -> {
            Map<String, Object> data = new HashMap<>();
            
            data.put("id", tp.getId());
            data.put("adjustmentHours", tp.getAdjustmentHours());
            data.put("adjustmentPay", tp.getAdjustmentPay());
            data.put("adjustmentRemarks", tp.getAdjustmentRemarks());
            data.put("deductionHours", tp.getDeductionHours());
            data.put("absentDeductionHours", tp.getAbsentDeductionHours());
            data.put("totalDeductionHours", tp.getTotalDeductionHours());
            
            data.put("sgdHours", tp.getSgdHours());
            data.put("tutorialLecHours", tp.getTutorialLecHours());
            data.put("tutorialLabHours", tp.getTutorialLabHours());
            data.put("honorarium", tp.getHonorarium());
            data.put("adminPay", tp.getAdminPay());
            data.put("supplementalPay", tp.getSupplementalPay());
            
            data.put("makeUpLecHours", tp.getMakeUpLecHours());
            data.put("makeUpLabHours", tp.getMakeUpLabHours());
            data.put("makeUpPay", tp.getMakeUpPay());
            
            data.put("hourlyRate", tp.getHourlyRate());
            data.put("labRate", tp.getLabRate());
            data.put("totalLecHours", tp.getTotalLecHours());
            data.put("totalLabHours", tp.getTotalLabHours());
            data.put("totalRleHours", tp.getTotalRleHours());
            data.put("workloadClassification", tp.getWorkloadClassification());

            data.put("suspensionDeduction", tp.getSuspensionDeduction() != null ? tp.getSuspensionDeduction() : BigDecimal.ZERO);
            data.put("appliedSuspensionDates", tp.getAppliedSuspensionDates());

            try { data.put("rleRate", tp.getClass().getMethod("getRleRate").invoke(tp)); } catch(Exception e) { data.put("rleRate", 0.0); }
            try { data.put("schedule", tp.getClass().getMethod("getSchedule").invoke(tp)); } catch(Exception e) { data.put("schedule", "Load matrix schedule"); }
            
            Employee emp = tp.getEmployee();
            List<Map<String, Object>> asRelieverList = new ArrayList<>();
            List<Map<String, Object>> whenAbsentList = new ArrayList<>();

            if (emp != null) {
                LocalDate tpStart = tp.getPeriodStart();
                LocalDate tpEnd = tp.getPeriodEnd();
                
                List<SubstituteRecord> allSubs = substituteRecordRepository.findAll();

                for (SubstituteRecord sub : allSubs) {
                    LocalDate subDate = sub.getDateSubstituted();
                    if (subDate != null && tpStart != null && tpEnd != null) {
                        if (subDate.isBefore(tpStart) || subDate.isAfter(tpEnd)) continue;
                    }

                    if (sub.getOriginalFaculty() != null && sub.getOriginalFaculty().getId().equals(emp.getId())) {
                        Map<String, Object> subRecord = new HashMap<>();
                        subRecord.put("relieverFacultyName", sub.getSubstituteFaculty().getFirstName() + " " + sub.getSubstituteFaculty().getLastName());
                        subRecord.put("hoursRendered", sub.getHoursRendered());
                        subRecord.put("actualDate", subDate != null ? subDate.toString() : "N/A");
                        subRecord.put("startTime", sub.getTeachingLoad().getStartTime() != null ? sub.getTeachingLoad().getStartTime().toString() : "");
                        subRecord.put("endTime", sub.getTeachingLoad().getEndTime() != null ? sub.getTeachingLoad().getEndTime().toString() : "");
                        subRecord.put("loadType", sub.getSubjectType() != null ? sub.getSubjectType() : "LEC");
                        whenAbsentList.add(subRecord);
                    }

                    if (sub.getSubstituteFaculty() != null && sub.getSubstituteFaculty().getId().equals(emp.getId())) {
                        Map<String, Object> subRecord = new HashMap<>();
                        subRecord.put("absentFacultyName", sub.getOriginalFaculty().getFirstName() + " " + sub.getOriginalFaculty().getLastName());
                        subRecord.put("hoursRendered", sub.getHoursRendered());
                        subRecord.put("actualDate", subDate != null ? subDate.toString() : "N/A");
                        subRecord.put("startTime", sub.getTeachingLoad().getStartTime() != null ? sub.getTeachingLoad().getStartTime().toString() : "");
                        subRecord.put("endTime", sub.getTeachingLoad().getEndTime() != null ? sub.getTeachingLoad().getEndTime().toString() : "");
                        subRecord.put("loadType", sub.getSubjectType() != null ? sub.getSubjectType() : "LEC");
                        asRelieverList.add(subRecord);
                    }
                }
            }

            data.put("substitutionsAsReliever", asRelieverList);
            data.put("substitutionsWhenAbsent", whenAbsentList);

            Map<String, Object> empData = new HashMap<>();
            if (emp != null) {
                empData.put("id", emp.getId());
                empData.put("employeeId", emp.getId()); 
                try {
                    Object customId = emp.getClass().getMethod("getEmployeeId").invoke(emp);
                    if (customId != null) empData.put("employeeId", customId);
                } catch(Exception e) { }
                
                empData.put("firstName", emp.getFirstName());
                empData.put("lastName", emp.getLastName());
            } else {
                empData.put("id", 0);
                empData.put("employeeId", "N/A");
                empData.put("firstName", "Unknown");
                empData.put("lastName", "Faculty");
            }
            data.put("employee", empData);

            return ResponseEntity.ok(data);
        }).orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/teaching-pay/api/update-process/{id}")
    @ResponseBody
    public ResponseEntity<?> updateTeachingPayProcess(@PathVariable Long id, @RequestBody TeachingPay updatedData) {
        return teachingPayRepository.findById(id).map(tp -> {
            tp.setAdjustmentHours(updatedData.getAdjustmentHours());
            tp.setAdjustmentPay(updatedData.getAdjustmentPay());
            tp.setAdjustmentRemarks(updatedData.getAdjustmentRemarks());
            tp.setDeductionHours(updatedData.getDeductionHours()); 
            tp.setSgdHours(updatedData.getSgdHours());
            tp.setTutorialLecHours(updatedData.getTutorialLecHours());
            tp.setTutorialLabHours(updatedData.getTutorialLabHours());
            tp.setHonorarium(updatedData.getHonorarium());
            tp.setAdminPay(updatedData.getAdminPay());
            tp.setSupplementalPay(updatedData.getSupplementalPay());
            
            tp.calculatePay();
            teachingPayRepository.save(tp);
            return ResponseEntity.ok().body("{\"message\": \"Updated successfully\"}");
        }).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/api/teaching-pay/{id}/makeup")
    @ResponseBody
    public ResponseEntity<?> updateMakeUpHours(
            @PathVariable Long id, 
            @RequestParam(defaultValue = "0.0") Double lecHours, 
            @RequestParam(defaultValue = "0.0") Double labHours) {

        return teachingPayRepository.findById(id).map(tp -> {
            tp.setMakeUpLecHours(lecHours);
            tp.setMakeUpLabHours(labHours);
            
            tp.calculatePay();
            
            teachingPayRepository.save(tp);
            
            return ResponseEntity.ok().body("{\"message\": \"Make-up classes encoded successfully\"}");
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/teaching-pay/export-report")
    public ResponseEntity<org.springframework.core.io.Resource> exportTeachingPayroll() {
        List<TeachingPay> teachingPays = teachingPayRepository.findAll();
        java.io.ByteArrayInputStream in = ExcelHelper.teachingPaysToExcel(teachingPays);
        org.springframework.core.io.InputStreamResource file = new org.springframework.core.io.InputStreamResource(in);
        
        return ResponseEntity.ok()
            .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Teaching_Payroll_Report.xlsx")
            .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(file); 
    }

    public static class TeachingSummaryDTO {
        // [Existing DTO Logic Remains the Same]
        private Long employeeId;
        private String name;
        private String department;
        private String employmentStatus;
        private String dateStart;
        private String dateEnd;
        private double lectureUnits;
        private double labUnits;
        private double totalLecHours; 
        private double totalLabHours; 
        private double excessLecUnits;
        private double excessLabUnits;
        private double excessLecHours;
        private double excessLabHours;
        private double totalExcessHours;
        private String workloadClassification;
        private double totalRleHours;
        private double substituteHours;
        private double absentDeductionHours; 

        public Long getEmployeeId() { return employeeId; }
        public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }
        public String getEmploymentStatus() { return employmentStatus; }
        public void setEmploymentStatus(String employmentStatus) { this.employmentStatus = employmentStatus; }
        public String getDateStart() { return dateStart; }
        public void setDateStart(String dateStart) { this.dateStart = dateStart; }
        public String getDateEnd() { return dateEnd; }
        public void setDateEnd(String dateEnd) { this.dateEnd = dateEnd; }
        public double getLectureUnits() { return lectureUnits; }
        public void setLectureUnits(double lectureUnits) { this.lectureUnits = lectureUnits; }
        public double getLabUnits() { return labUnits; }
        public void setLabUnits(double labUnits) { this.labUnits = labUnits; }
        public double getTotalLecHours() { return totalLecHours; }
        public void setTotalLecHours(double totalLecHours) { this.totalLecHours = totalLecHours; }
        public double getTotalLabHours() { return totalLabHours; }
        public void setTotalLabHours(double totalLabHours) { this.totalLabHours = totalLabHours; }
        public double getExcessLecUnits() { return excessLecUnits; }
        public void setExcessLecUnits(double excessLecUnits) { this.excessLecUnits = excessLecUnits; }
        public double getExcessLabUnits() { return excessLabUnits; }
        public void setExcessLabUnits(double excessLabUnits) { this.excessLabUnits = excessLabUnits; }
        public double getExcessLecHours() { return excessLecHours; }
        public void setExcessLecHours(double excessLecHours) { this.excessLecHours = excessLecHours; }
        public double getExcessLabHours() { return excessLabHours; }
        public void setExcessLabHours(double excessLabHours) { this.excessLabHours = excessLabHours; }
        public double getTotalExcessHours() { return totalExcessHours; }
        public void setTotalExcessHours(double totalExcessHours) { this.totalExcessHours = totalExcessHours; }
        public String getWorkloadClassification() { return workloadClassification; }
        public void setWorkloadClassification(String workloadClassification) { this.workloadClassification = workloadClassification; }
        public double getTotalRleHours() { return totalRleHours; }
        public void setTotalRleHours(double totalRleHours) { this.totalRleHours = totalRleHours; }
        public double getSubstituteHours() { return substituteHours; }
        public void setSubstituteHours(double substituteHours) { this.substituteHours = substituteHours; }
        public double getAbsentDeductionHours() { return absentDeductionHours; }
        public void setAbsentDeductionHours(double absentDeductionHours) { this.absentDeductionHours = absentDeductionHours; }
    }
}