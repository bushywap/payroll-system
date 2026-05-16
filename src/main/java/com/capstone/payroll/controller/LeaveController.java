package com.capstone.payroll.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.capstone.payroll.model.Leave; // Changed from LeaveBalance
import com.capstone.payroll.service.LeaveService;

@Controller
public class LeaveController {

    @Autowired
    private LeaveService leaveService;

    @GetMapping("/leave")
    public String showLeavePage() {
        return "leave";
    }

    @GetMapping("/api/leave/types")
    @ResponseBody
    public List<String> getLeaveTypes() {
        return leaveService.getLeaveTypes();
    }

    @GetMapping("/api/leave")
    @ResponseBody
    public List<Leave> getAllLeaves() { // Changed to Leave
        return leaveService.findAllLeaves();
    }

    @GetMapping("/api/leave/{id}")
    @ResponseBody
    public ResponseEntity<Leave> getLeaveById(@PathVariable Long id) { // Changed to Leave
        Optional<Leave> leave = leaveService.findLeaveById(id);
        return leave.map(ResponseEntity::ok)
                   .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/api/leave/employee/{employeeId}")
    @ResponseBody
    public List<Leave> getLeavesByEmployeeId(@PathVariable Long employeeId) { // Changed to Leave
        return leaveService.findLeavesByEmployeeId(employeeId);
    }

    @GetMapping("/api/leave/status/{status}")
    @ResponseBody
    public List<Leave> getLeavesByStatus(@PathVariable String status) { // Changed to Leave
        return leaveService.findLeavesByStatus(status);
    }

    @GetMapping("/api/leave/type/{leaveType}")
    @ResponseBody
    public List<Leave> getLeavesByType(@PathVariable String leaveType) { // Changed to Leave
        return leaveService.findLeavesByType(leaveType);
    }

    @PostMapping("/api/leave")
    @ResponseBody
    public Map<String, Object> createLeave(@RequestBody Map<String, Object> leaveData) {
        try {
            Leave leave = new Leave(); // Changed to Leave
            leave.setEmployeeDbId(Long.parseLong(leaveData.get("employeeId").toString()));
            leave.setLeaveType(leaveData.get("leaveType").toString());
            leave.setStartDate(LocalDate.parse(leaveData.get("startDate").toString()));
            leave.setEndDate(LocalDate.parse(leaveData.get("endDate").toString()));
            leave.setReason(leaveData.get("reason") != null ? leaveData.get("reason").toString() : "");
            leave.setStatus(leaveData.get("status") != null ? leaveData.get("status").toString() : "Pending");

            Leave savedLeave = leaveService.createLeave(leave);
            return Map.of("success", true, "message", "Leave request created successfully", "leave", savedLeave);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
    }

    @PutMapping("/api/leave/{id}")
    @ResponseBody
    public Map<String, Object> updateLeave(@PathVariable Long id, @RequestBody Map<String, Object> leaveData) {
        try {
            Optional<Leave> leaveOpt = leaveService.findLeaveById(id); // Changed to Leave
            if (leaveOpt.isEmpty()) {
                return Map.of("success", false, "message", "Leave not found");
            }

            Leave leave = leaveOpt.get(); // Changed to Leave
            if (leaveData.containsKey("leaveType")) {
                leave.setLeaveType(leaveData.get("leaveType").toString());
            }
            if (leaveData.containsKey("startDate")) {
                leave.setStartDate(LocalDate.parse(leaveData.get("startDate").toString()));
            }
            if (leaveData.containsKey("endDate")) {
                leave.setEndDate(LocalDate.parse(leaveData.get("endDate").toString()));
            }
            if (leaveData.containsKey("reason")) {
                leave.setReason(leaveData.get("reason").toString());
            }
            if (leaveData.containsKey("status")) {
                leave.setStatus(leaveData.get("status").toString());
            }

            if (leave.getStartDate() != null && leave.getEndDate() != null) {
                long days = java.time.temporal.ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
                leave.setTotalDays((int) days);
            }

            Leave updatedLeave = leaveService.updateLeave(leave);
            return Map.of("success", true, "message", "Leave updated successfully", "leave", updatedLeave);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
    }

    @PostMapping("/api/leave/{id}/approve")
    @ResponseBody
    public Map<String, Object> approveLeave(@PathVariable Long id, @RequestBody Map<String, String> data) {
        try {
            Leave leave = leaveService.approveLeave(id, data.get("approvedBy"), data.get("remarks")); // Changed to Leave
            if (leave != null) {
                return Map.of("success", true, "message", "Leave approved successfully", "leave", leave);
            } else {
                return Map.of("success", false, "message", "Leave not found");
            }
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
    }

    @PostMapping("/api/leave/{id}/reject")
    @ResponseBody
    public Map<String, Object> rejectLeave(@PathVariable Long id, @RequestBody Map<String, String> data) {
        try {
            Leave leave = leaveService.rejectLeave(id, data.get("approvedBy"), data.get("remarks")); // Changed to Leave
            if (leave != null) {
                return Map.of("success", true, "message", "Leave rejected", "leave", leave);
            } else {
                return Map.of("success", false, "message", "Leave not found");
            }
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/api/leave/{id}")
    @ResponseBody
    public Map<String, Object> deleteLeave(@PathVariable Long id) {
        try {
            leaveService.deleteLeave(id);
            return Map.of("success", true, "message", "Leave deleted successfully");
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
    }
}