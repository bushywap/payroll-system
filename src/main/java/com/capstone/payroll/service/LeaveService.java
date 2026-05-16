package com.capstone.payroll.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.capstone.payroll.model.Leave; 
import com.capstone.payroll.repository.LeaveRepository;

@Service
public class LeaveService {

    @Autowired
    private LeaveRepository leaveRepository;

    public List<String> getLeaveTypes() {
        return List.of(
            "Vacation Leave", "Sick Leave", "Emergency Leave", "Maternity Leave",
            "Paternity Leave", "Personal Leave", "Bereavement Leave",
            "Service Incentive Leave", "Study Leave", "Solo Parent Leave"
        );
    }

    public Leave createLeave(Leave leave) { 
        if (leave.getStartDate() != null && leave.getEndDate() != null) {
            long days = java.time.temporal.ChronoUnit.DAYS.between(leave.getStartDate(), leave.getEndDate()) + 1;
            leave.setTotalDays((int) days);
        }
        return leaveRepository.save(leave);
    }

    public List<Leave> findAllLeaves() { return leaveRepository.findAll(); }

    public Optional<Leave> findLeaveById(Long id) { return leaveRepository.findById(id); }

    public List<Leave> findLeavesByEmployeeId(Long employeeId) { return leaveRepository.findByEmployeeId(employeeId); }

    public List<Leave> findLeavesByStatus(String status) { return leaveRepository.findByStatus(status); }

    public List<Leave> findLeavesByType(String leaveType) { return leaveRepository.findByLeaveType(leaveType); }

    public Leave updateLeave(Leave leave) { return leaveRepository.save(leave); }

    // =========================================================================
    // STORED PROCEDURE: APPROVE LEAVE & DEDUCT BALANCE
    // =========================================================================
    public Leave approveLeave(Long leaveId, String approvedBy, String remarks) { 
        Optional<Leave> leaveOpt = leaveRepository.findById(leaveId);
        if (leaveOpt.isPresent()) {
            Leave leave = leaveOpt.get();
            
            // Execute Database Transaction to safely deduct balances
            leaveRepository.approveLeaveProc(
                leave.getId(), 
                leave.getEmployee().getId(), 
                leave.getLeaveType() != null ? leave.getLeaveType().toUpperCase() : "VACATION", 
                leave.getTotalDays() != null ? leave.getTotalDays() : 0
            );
            
            // Update the entity with the approver name and remarks
            leave.setStatus("Approved");
            leave.setApprovedBy(approvedBy);
            leave.setRemarks(remarks);
            return leaveRepository.save(leave);
        }
        return null;
    }

    public Leave rejectLeave(Long leaveId, String approvedBy, String remarks) { 
        Optional<Leave> leaveOpt = leaveRepository.findById(leaveId);
        if (leaveOpt.isPresent()) {
            Leave leave = leaveOpt.get();
            leave.setStatus("Rejected");
            leave.setApprovedBy(approvedBy);
            leave.setRemarks(remarks);
            return leaveRepository.save(leave);
        }
        return null;
    }

    public void deleteLeave(Long id) { leaveRepository.deleteById(id); }

    public List<Leave> findLeavesByDateRange(LocalDate startDate, LocalDate endDate) { 
        return leaveRepository.findByStartDateBetween(startDate, endDate);
    }

    public List<Leave> findEmployeeLeavesByDateRange(Long employeeId, LocalDate startDate, LocalDate endDate) { 
        return leaveRepository.findByEmployeeIdAndStartDateBetween(employeeId, startDate, endDate);
    }
}