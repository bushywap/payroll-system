package com.capstone.payroll.repository;

import com.capstone.payroll.model.Employee;
import com.capstone.payroll.model.TeachingPay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional; // <-- Added this import

@Repository
public interface TeachingPayRepository extends JpaRepository<TeachingPay, Long> {

    TeachingPay findByEmployeeAndPeriodStartAndPeriodEnd(Employee employee, LocalDate periodStart, LocalDate periodEnd);
    
    List<TeachingPay> findByPeriodStartAndPeriodEnd(LocalDate periodStart, LocalDate periodEnd);

    // Resolves the "undefined" error when fetching the latest teaching pay
    List<TeachingPay> findByEmployeeIdOrderByPeriodEndDesc(Long employeeId);
    
    // =========================================================================
    // NEW: REQUIRED TO PRESERVE MAKE-UP CLASSES DURING PAGE RELOADS
    // =========================================================================
    Optional<TeachingPay> findByEmployeeIdAndPeriodStartAndPeriodEnd(Long employeeId, LocalDate periodStart, LocalDate periodEnd);

    // =========================================================================
    // UPDATED STORED PROCEDURE CALL
    // Connects Java's calculated hours and suspension money directly to MySQL
    // =========================================================================
    @Modifying
    @Transactional
    @Query(value = "CALL SP_GenerateManualTeachingPayroll(:empId, :startDate, :endDate, :lecHours, :labHours, :rate, :holiday, :suspensionDed, :loans, :suspensionDates)", nativeQuery = true)
    void callGenerateManualTeachingPayroll(
        @Param("empId") Long empId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("lecHours") Double lecHours,
        @Param("labHours") Double labHours,
        @Param("rate") BigDecimal rate,
        @Param("holiday") BigDecimal holiday,
        @Param("suspensionDed") BigDecimal suspensionDed,
        @Param("loans") BigDecimal loans,
        @Param("suspensionDates") String suspensionDates 
    );
}