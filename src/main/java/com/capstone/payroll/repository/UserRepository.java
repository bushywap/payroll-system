package com.capstone.payroll.repository;

import com.capstone.payroll.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    
    User findByVerificationCode(String code);
    
    // NEW: Find by reset token
    User findByResetPasswordToken(String token);
}