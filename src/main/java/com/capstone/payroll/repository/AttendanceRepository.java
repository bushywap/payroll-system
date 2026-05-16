package com.capstone.payroll.repository;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.capstone.payroll.model.Attendance;

@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    
    @Query("SELECT a FROM Attendance a JOIN Employee e ON a.employeeId = e.employeeId WHERE e.department.departmentCode = :deptCode")
    List<Attendance> findByEmployeeDepartmentCode(@Param("deptCode") String deptCode);
    
    List<Attendance> findByEmployeeId(String employeeId);
    List<Attendance> findByEmployeeIdAndDate(String employeeId, LocalDate date);
    List<Attendance> findByEmployeeIdAndDateBetween(String employeeId, LocalDate startDate, LocalDate endDate);
    List<Attendance> findByEmployeeIdAndDateBetweenOrderByDateAsc(String employeeId, LocalDate startDate, LocalDate endDate);

    @Query("SELECT a FROM Attendance a JOIN Employee e ON a.employeeId = e.employeeId WHERE a.employeeId = :empId AND e.designation.teaching = 1")
    List<Attendance> findTeachingTypeByEmployeeId(@Param("empId") String employeeId);

    @Query("SELECT a FROM Attendance a JOIN Employee e ON a.employeeId = e.employeeId WHERE a.employeeId = :empId AND e.designation.employee = 1")
    List<Attendance> findEmployeeTypeByEmployeeId(@Param("empId") String employeeId);

    // --- STORED PROCEDURE CALLS ---
    @Modifying
    @Transactional
    @Query(value = "CALL SP_RecordTimeIn(:empId, :timeIn, :date)", nativeQuery = true)
    void recordTimeInProc(@Param("empId") String empId, @Param("timeIn") java.time.LocalTime timeIn, @Param("date") LocalDate date);

    @Modifying
    @Transactional
    @Query(value = "CALL SP_RecordTimeOut(:empId, :timeOut, :date)", nativeQuery = true)
    void recordTimeOutProc(@Param("empId") String empId, @Param("timeOut") java.time.LocalTime timeOut, @Param("date") LocalDate date);
}