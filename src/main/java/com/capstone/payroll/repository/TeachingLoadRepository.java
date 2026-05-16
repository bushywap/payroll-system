package com.capstone.payroll.repository;

import com.capstone.payroll.model.TeachingLoad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeachingLoadRepository extends JpaRepository<TeachingLoad, Long> {
    
    // Original method (searches by Database ID: 1, 2, 3)
    List<TeachingLoad> findByEmployeeId(Long employeeId);
    
    // NEW METHOD: Searches by the String School ID (1-00056)
    List<TeachingLoad> findByEmployeeEmployeeId(String employeeId);
}