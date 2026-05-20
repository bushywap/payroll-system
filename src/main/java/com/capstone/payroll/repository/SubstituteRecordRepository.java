package com.capstone.payroll.repository;

import com.capstone.payroll.model.SubstituteRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubstituteRecordRepository extends JpaRepository<SubstituteRecord, Long> {
    
    // EAC employee PK is varchar employee_id (Employee.id)
    List<SubstituteRecord> findByOriginalFaculty_Id(String originalFacultyId);

    List<SubstituteRecord> findBySubstituteFaculty_Id(String substituteFacultyId);
    
    Optional<SubstituteRecord> findByTeachingLoadId(Long loadId);
}
