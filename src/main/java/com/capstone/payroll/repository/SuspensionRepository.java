package com.capstone.payroll.repository;

import com.capstone.payroll.model.Suspension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SuspensionRepository extends JpaRepository<Suspension, Long> {
    List<Suspension> findByDate(LocalDate date);
}