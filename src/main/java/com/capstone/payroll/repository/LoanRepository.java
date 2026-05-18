package com.capstone.payroll.repository;

import com.capstone.payroll.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    List<Loan> findByEmployeeId(Long employeeId);
    
    @Query("SELECT l FROM Loan l WHERE l.employee.id = :employeeId AND l.status = 'ACTIVE'")
    List<Loan> findActiveLoansByEmployeeId(Long employeeId);
}