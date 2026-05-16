package com.capstone.payroll.controller;

import com.capstone.payroll.model.Holidays;
import com.capstone.payroll.model.Suspension;
import com.capstone.payroll.repository.HolidayRepository;
import com.capstone.payroll.repository.SuspensionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalTime;

@Controller
public class CalendarController {

    @Autowired
    private HolidayRepository holidayRepository;

    @Autowired
    private SuspensionRepository suspensionRepository;

    // ==========================================
    // HOLIDAYS MAPPINGS
    // ==========================================
    
    @GetMapping("/holidays")
    public String showHolidays(Model model) {
        model.addAttribute("holidays", holidayRepository.findAll());
        return "holidays";
    }

    @PostMapping("/api/holidays/add")
    @ResponseBody
    public ResponseEntity<?> addHoliday(
            @RequestParam String date, 
            @RequestParam String name, 
            @RequestParam String type) {
        try {
            Holidays holiday = new Holidays();
            holiday.setDate(LocalDate.parse(date));
            holiday.setName(name); 
            holiday.setType(type); // "REGULAR" or "SPECIAL_NON_WORKING"
            holidayRepository.save(holiday);
            return ResponseEntity.ok("Holiday successfully added to the calendar.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to add holiday. Check your data format.");
        }
    }

    @PostMapping("/api/holidays/delete")
    @ResponseBody
    public ResponseEntity<?> deleteHoliday(@RequestParam int id) {
        holidayRepository.deleteById(id);
        return ResponseEntity.ok("Holiday successfully removed.");
    }

    // NEW: BULK DELETE API
    @PostMapping("/api/holidays/delete-multiple")
    @ResponseBody
    public ResponseEntity<?> deleteMultipleHolidays(@RequestBody java.util.List<Integer> ids) {
        try {
            if (ids == null || ids.isEmpty()) {
                return ResponseEntity.badRequest().body(java.util.Map.of("success", false, "message", "No holidays selected."));
            }
            // Spring Data JPA safely deletes all provided IDs in a single query
            holidayRepository.deleteAllById(ids); 
            
            return ResponseEntity.ok(java.util.Map.of("success", true, "message", "Holidays deleted successfully."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of("success", false, "message", "Error deleting holidays: " + e.getMessage()));
        }
    }

    // ==========================================
    // SUSPENSIONS MAPPINGS
    // ==========================================

    @GetMapping("/suspensions")
    public String showSuspensions(Model model) {
        model.addAttribute("suspensions", suspensionRepository.findAll());
        return "suspensions";
    }

    @PostMapping("/api/suspensions/add")
    @ResponseBody
    public ResponseEntity<?> addSuspension(
            @RequestParam String date, 
            @RequestParam String reason, 
            @RequestParam(required = false) String startTime) {
        try {
            Suspension suspension = new Suspension();
            suspension.setDate(LocalDate.parse(date));
            suspension.setReason(reason);
            
            // Handle optional partial-day suspension time
            if (startTime != null && !startTime.trim().isEmpty()) {
                suspension.setStartTime(LocalTime.parse(startTime));
            } else {
                suspension.setStartTime(null); // Full day suspension
            }
            
            suspensionRepository.save(suspension);
            return ResponseEntity.ok("Class/Work suspension successfully recorded.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed to add suspension. Check your data format.");
        }
    }

    @PostMapping("/api/suspensions/delete")
    @ResponseBody
    public ResponseEntity<?> deleteSuspension(@RequestParam Long id) {
        suspensionRepository.deleteById(id);
        return ResponseEntity.ok("Suspension successfully removed.");
    }
}