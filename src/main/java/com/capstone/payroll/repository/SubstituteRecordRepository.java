package com.capstone.payroll.repository;

import com.capstone.payroll.model.SubstituteRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubstituteRecordRepository extends JpaRepository<SubstituteRecord, Long> {
    
    // Custom query to find all substitute records for a specific absent faculty
    List<SubstituteRecord> findByOriginalFacultyId(Long originalFacultyId);

    // Custom query to find all classes a specific faculty substituted for (to pay them)
    List<SubstituteRecord> findBySubstituteFacultyId(Long substituteFacultyId);
    
    Optional<SubstituteRecord> findByTeachingLoadId(Long loadId);
}
