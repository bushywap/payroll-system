package com.capstone.payroll.controller;

import com.capstone.payroll.model.Employee;
import com.capstone.payroll.model.SubstituteRecord;
import com.capstone.payroll.model.TeachingLoad;
import com.capstone.payroll.service.TeachingLoadService;
import com.capstone.payroll.repository.EmployeeRepository;
import com.capstone.payroll.repository.SubstituteRecordRepository;
import com.capstone.payroll.repository.TeachingLoadRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/teaching-load")
public class TeachingLoadRESTController {

    @Autowired private TeachingLoadService teachingLoadService;
    @Autowired private TeachingLoadRepository teachingLoadRepository;
    @Autowired private SubstituteRecordRepository substituteRecordRepository;
    @Autowired private EmployeeRepository employeeRepository;

    @GetMapping("/{employeeId}")
    public ResponseEntity<List<TeachingLoad>> getTeachingLoadByEmployeeId(@PathVariable String employeeId) {
        List<TeachingLoad> load = teachingLoadService.findByEmployeeId(employeeId);
        return ResponseEntity.ok(load);
    }

    @GetMapping("/all")
    public ResponseEntity<List<TeachingLoad>> getAllTeachingLoads() {
        List<TeachingLoad> loads = teachingLoadService.getAllTeachingLoads();
        List<SubstituteRecord> allSubs = substituteRecordRepository.findAll();

        // ✅ LINK: Populates transient fields so the UI shows current substitute info
        for (TeachingLoad load : loads) {
            allSubs.stream()
                .filter(sub -> sub.getTeachingLoad() != null && sub.getTeachingLoad().getId().equals(load.getId()))
                .findFirst()
                .ifPresent(sub -> {
                    load.setSubstituteFacultyName(sub.getSubstituteFaculty().getFirstName() + " " + sub.getSubstituteFaculty().getLastName());
                    load.setSubstituteRenderedHours(sub.getHoursRendered());
                    load.setSubstituteDate(sub.getDateSubstituted().toString());
                    load.setSubjectType(sub.getSubjectType());
                });
        }
        return ResponseEntity.ok(loads);
    }
    
    @GetMapping("/schedule/{empId}")
    @ResponseBody
    public ResponseEntity<List<TeachingLoad>> getEmployeeSchedule(@PathVariable String empId) { 
        List<TeachingLoad> schedule = teachingLoadRepository.findByEmployee_Id(empId);
        if (schedule.isEmpty()) { return ResponseEntity.notFound().build(); }
        return ResponseEntity.ok(schedule);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateTeachingLoad(@PathVariable Long id, @RequestBody Map<String, Object> updatedData) {
        Optional<TeachingLoad> optionalLoad = teachingLoadRepository.findById(id);
        
        if (optionalLoad.isPresent()) {
            TeachingLoad load = optionalLoad.get();
            
            // 1. ✅ ENABLE UNIT EDITING: Allows changing units from the UI modal
            if (updatedData.containsKey("lectureUnits")) {
                load.setLectureUnits(Integer.parseInt(updatedData.get("lectureUnits").toString()));
            }
            if (updatedData.containsKey("labUnits")) {
                load.setLabUnits(Integer.parseInt(updatedData.get("labUnits").toString()));
            }

            int sections = updatedData.get("noOfSections") != null ? Integer.parseInt(updatedData.get("noOfSections").toString()) : load.getNoOfSections();
            double rleHours = updatedData.get("rleHours") != null ? Double.parseDouble(updatedData.get("rleHours").toString()) : load.getRleHours();
            
            load.setNoOfSections(sections);
            load.setRleHours(rleHours);
            
            // Recalculate base hours
            load.setLecHours((load.getLectureUnits() * 1.0) * sections);
            load.setLabHours((load.getLabUnits() * 3.0) * sections);
            load.setTotalHours(load.getLecHours() + load.getLabHours() + rleHours);
            
            teachingLoadRepository.save(load);

            // 2. ✅ HANDLE SUBSTITUTION: Finds existing record to prevent duplicates
            String subName = updatedData.get("substituteFacultyName") != null ? updatedData.get("substituteFacultyName").toString().trim() : "";
            
            if (!subName.isEmpty()) {
                double subHours = updatedData.get("substituteRenderedHours") != null ? Double.parseDouble(updatedData.get("substituteRenderedHours").toString()) : 0.0;
                String subDateStr = updatedData.get("substituteDate") != null ? updatedData.get("substituteDate").toString() : "";
                String subType = updatedData.get("subjectType") != null ? updatedData.get("subjectType").toString() : "LEC";
                
                // Fuzzy Search for the Substitute Employee
                Employee substituteEmp = null;
                String cleanInput = subName.replaceAll("\\s+", " ").toLowerCase();
                for (Employee e : employeeRepository.findAll()) {
                    String fullName = (e.getFirstName() + " " + e.getLastName()).replaceAll("\\s+", " ").toLowerCase();
                    if (fullName.equals(cleanInput)) {
                        substituteEmp = e;
                        break;
                    }
                }
                
                if (substituteEmp != null && !subDateStr.isEmpty()) {
                    // ✅ CHECK FOR EXISTING: Prevent multiple rows for the same class
                    SubstituteRecord record = allSubsRepositoryMatch(load.getId());
                    if (record == null) {
                        record = new SubstituteRecord();
                    }
                    
                    record.setTeachingLoad(load);
                    record.setOriginalFaculty(load.getEmployee());
                    record.setSubstituteFaculty(substituteEmp);
                    record.setDateSubstituted(LocalDate.parse(subDateStr));
                    record.setHoursRendered(subHours);
                    record.setSubjectType(subType);
                    substituteRecordRepository.save(record);
                }
            }
            
            return ResponseEntity.ok().body("{\"message\": \"Teaching load and substitution updated successfully.\"}");
        }
        return ResponseEntity.notFound().build();
    }

    // ✅ HELPER: Used to update existing records instead of creating duplicates
    private SubstituteRecord allSubsRepositoryMatch(Long loadId) {
        return substituteRecordRepository.findAll().stream()
            .filter(sub -> sub.getTeachingLoad() != null && sub.getTeachingLoad().getId().equals(loadId))
            .findFirst()
            .orElse(null);
    }
}