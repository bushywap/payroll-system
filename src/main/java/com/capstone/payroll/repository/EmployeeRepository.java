package com.capstone.payroll.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import com.capstone.payroll.model.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    List<Employee> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(String firstName, String lastName);

    Employee findByEmail(String email);
    
    List<Employee> findByEmployeeStatus(String employeeStatus);
    
    // Optional: Fetch directly by the school's string ID
    Optional<Employee> findByEmployeeId(String employeeId);

    // Unified Search updated to use the new String field without casting
    @Query("SELECT e FROM Employee e WHERE " +
           "e.employeeId LIKE CONCAT('%', :query, '%') OR " +
           "LOWER(e.firstName) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.lastName) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<Employee> searchByIdOrName(@Param("query") String query);
    
    @Query("SELECT e FROM Employee e WHERE e.designation.teaching = 1")
    List<Employee> findTeaching();
    
    @Query("SELECT e FROM Employee e WHERE e.designation.employee = 1")
    List<Employee> findEmployee();
}