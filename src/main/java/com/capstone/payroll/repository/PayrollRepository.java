package com.capstone.payroll.repository;

import com.capstone.payroll.model.Payroll;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PayrollRepository extends JpaRepository<Payroll, Long> {
    
    List<Payroll> findByPayPeriodStartAndPayPeriodEndAndStatus(LocalDate start, LocalDate end, String status);
    Payroll findByEmployee_IdAndPayPeriodStartAndPayPeriodEnd(String employeeId, LocalDate start, LocalDate end);

    // ✅ ADDED THIS LINE FOR THE 13TH MONTH PAY!
    List<Payroll> findByEmployee_IdAndPayPeriodStartBetween(String employeeId, LocalDate start, LocalDate end);

    // --- STORED PROCEDURE CALLS ---
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "CALL SP_Proces2sRegularPayroll(:empId, :start, :end, :sssLoan, :hdmfLoan, :adj, :honorarium, :longevity, :lateMins, :utMins, :absentDays, :otHours, :holidayPay, :leaveWp, :leaveWop, :teachingPay, :partTimeHours)", nativeQuery = true)
    void processRegularPayrollProc(
        @Param("empId") Long empId, @Param("start") LocalDate start, @Param("end") LocalDate end,
        @Param("sssLoan") BigDecimal sssLoan, @Param("hdmfLoan") BigDecimal hdmfLoan,
        @Param("adj") BigDecimal adj, @Param("honorarium") BigDecimal honorarium, @Param("longevity") BigDecimal longevity,
        @Param("lateMins") int lateMins, @Param("utMins") int utMins, @Param("absentDays") int absentDays,
        @Param("otHours") double otHours, @Param("holidayPay") BigDecimal holidayPay,
        @Param("leaveWp") int leaveWp, @Param("leaveWop") int leaveWop,
        @Param("teachingPay") BigDecimal teachingPay, @Param("partTimeHours") double partTimeHours
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "CALL SP_ProcessBatchTeachingPayroll(:empId, :start, :end, :loans, :lateMins, :utMins, :absentDays, :teachingPay)", nativeQuery = true)
    void processBatchTeachingPayrollProc(
        @Param("empId") Long empId, @Param("start") LocalDate start, @Param("end") LocalDate end,
        @Param("loans") BigDecimal loans, @Param("lateMins") int lateMins, @Param("utMins") int utMins, @Param("absentDays") int absentDays,
        @Param("teachingPay") BigDecimal teachingPay
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = "CALL SP_GenerateManualTeachingPayroll(:empId, :startDate, :endDate, :lec, :lab, :rate, :holiday, :suspension, :loans)", nativeQuery = true)
    void generateManualTeachingPayrollProc(
        @Param("empId") Long empId, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, 
        @Param("lec") Double lec, @Param("lab") Double lab, @Param("rate") BigDecimal rate,
        @Param("holiday") BigDecimal holiday, @Param("suspension") BigDecimal suspension, @Param("loans") BigDecimal loans
    ); 
}