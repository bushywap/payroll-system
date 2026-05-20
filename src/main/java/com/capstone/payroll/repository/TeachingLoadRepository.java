package com.capstone.payroll.repository;

import com.capstone.payroll.model.TeachingLoad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeachingLoadRepository extends JpaRepository<TeachingLoad, Long> {
    
    /** By EAC employee id ({@code employee.employee_id}, e.g. 1-00001). */
    List<TeachingLoad> findByEmployee_Id(String employeeId);
}