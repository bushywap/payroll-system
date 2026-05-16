package com.capstone.payroll.repository;

import com.capstone.payroll.model.Holidays;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holidays, Integer> {
    Optional<Holidays> findByDate(LocalDate date);
}