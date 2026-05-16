package com.capstone.payroll.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User { 

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String firstName;
    private String lastName;

    private boolean enabled = false; 
    
    private String verificationCode;
    private LocalDateTime verificationExpiry = LocalDateTime.now().plusDays(1);
    private String resetPasswordToken;
    private LocalDateTime resetPasswordExpiry;

    // ✅ REMOVED: The 'employee' relationship is gone. 
    // This table is now only for system authentication.

    private String role = "ROLE_ADMIN"; 

    public User() {}

    public User(String email, String password, String firstName, String lastName, String verificationCode) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.verificationCode = verificationCode;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public boolean isEnabled() { return enabled; }
    public String getVerificationCode() { return verificationCode; }
    public LocalDateTime getVerificationExpiry() { return verificationExpiry; }
    public String getResetPasswordToken() { return resetPasswordToken; }
    public LocalDateTime getResetPasswordExpiry() { return resetPasswordExpiry; }
    public String getRole() { return role; }

    public void setPassword(String password) { this.password = password; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }
    public void setVerificationExpiry(LocalDateTime verificationExpiry) { this.verificationExpiry = verificationExpiry; }
    public void setResetPasswordToken(String resetPasswordToken) { this.resetPasswordToken = resetPasswordToken; }
    public void setResetPasswordExpiry(LocalDateTime resetPasswordExpiry) { this.resetPasswordExpiry = resetPasswordExpiry; }
    public void setRole(String role) { this.role = role; }
}