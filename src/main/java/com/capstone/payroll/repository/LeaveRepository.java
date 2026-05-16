	package com.capstone.payroll.repository;
	
	import java.time.LocalDate;
	import java.util.List;
	import org.springframework.data.jpa.repository.JpaRepository;
	import org.springframework.data.jpa.repository.Modifying;
	import org.springframework.data.jpa.repository.Query;
	import org.springframework.data.repository.query.Param;
	import org.springframework.stereotype.Repository;
	import org.springframework.transaction.annotation.Transactional;
	import com.capstone.payroll.model.Leave; 
	
	@Repository
	public interface LeaveRepository extends JpaRepository<Leave, Long> { 
	    
	    List<Leave> findByEmployeeId(Long employeeId); 
	    List<Leave> findByEmployeeIdAndStatus(Long employeeId, String status); 
	    List<Leave> findByStatus(String status); 
	    List<Leave> findByLeaveType(String leaveType); 
	    List<Leave> findByStartDateBetween(LocalDate startDate, LocalDate endDate); 
	    List<Leave> findByEmployeeIdAndStartDateBetween(Long employeeId, LocalDate startDate, LocalDate endDate); 
	
	    @Modifying
	    @Transactional
	    @Query(value = "CALL SP_ApproveLeaveTransaction(:leaveId, :empId, :leaveType, :days)", nativeQuery = true)
	    void approveLeaveProc(
	        @Param("leaveId") Long leaveId, @Param("empId") Long empId,
	        @Param("leaveType") String leaveType, @Param("days") int days
	    );
	}