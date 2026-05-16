package com.capstone.payroll.service;

import com.capstone.payroll.model.User;
import com.capstone.payroll.model.Employee;
import com.capstone.payroll.repository.UserRepository;
import com.capstone.payroll.repository.EmployeeRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy; 
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserService implements UserDetailsService {

    @Autowired private UserRepository userRepository;
    @Autowired private EmployeeRepository employeeRepository; 
    
    @Autowired 
    @Lazy 
    private PasswordEncoder passwordEncoder;
    
    @Autowired private JavaMailSender mailSender;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        String role = user.getRole();
        if (role == null || role.trim().isEmpty()) {
            role = "ROLE_ADMIN"; 
        }
        
        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .disabled(!user.isEnabled()) 
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .authorities(List.of(new SimpleGrantedAuthority(role)))
                .build();
    }

    public String registerUser(String email, String rawPassword, String firstName, String lastName, String siteURL)
            throws UnsupportedEncodingException, MessagingException {
        
        if (userRepository.findByEmail(email).isPresent()) return "Email is already registered.";
        
        // 1. Create the Employee record (The Payroll Profile)
        Employee newEmployee = new Employee();
        newEmployee.setEmail(email);
        newEmployee.setFirstName(firstName);
        newEmployee.setLastName(lastName);
        newEmployee.setEmployeeStatus("Active"); 
        
        // FIX: Generate a unique alphanumeric Employee ID to satisfy the NOT NULL database constraint
        // You can change this to a specific format like "EAC-001" later in the Employee Masterlist
        String generatedBusinessId = "EMP-" + System.currentTimeMillis();
        newEmployee.setEmployeeId(generatedBusinessId);

        employeeRepository.save(newEmployee);

        // 2. Create the User record (The Login Account)
        User newUser = new User(email, passwordEncoder.encode(rawPassword), firstName, lastName, UUID.randomUUID().toString());
        
        newUser.setEnabled(false); 
        newUser.setVerificationExpiry(LocalDateTime.now().plusHours(24)); 
        newUser.setRole("ROLE_ADMIN"); 

        // ✅ REMOVED: newUser.setEmployee(newEmployee); 
        // This line is gone because the User table no longer has an employee_id column.
        
        userRepository.save(newUser);
        
        try {
            sendEmail(newUser, siteURL, "verify");
        } catch(Exception e) {
            System.out.println("Email sending skipped or failed: " + e.getMessage());
        }
        
        return "success";
    }

    public boolean verify(String verificationCode) {
        User user = userRepository.findByVerificationCode(verificationCode);
        
        if (user == null || user.isEnabled()) return false;
        
        if (user.getVerificationExpiry() != null && user.getVerificationExpiry().isBefore(LocalDateTime.now())) {
            return false;
        }

        user.setVerificationCode(null);
        user.setEnabled(true);
        userRepository.save(user);
        return true;
    }

    public String initiatePasswordReset(String email, String siteURL) {
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isEmpty()) return "Email not found.";
        
        User user = userOptional.get();
        String token = UUID.randomUUID().toString();
        user.setResetPasswordToken(token);
        user.setResetPasswordExpiry(LocalDateTime.now().plusHours(1));
        userRepository.save(user);
        
        try {
            sendEmail(user, siteURL, "reset");
            return "success";
        } catch (Exception e) {
            return "Error sending email.";
        }
    }

    public String resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetPasswordToken(token);
        if (user == null || user.getResetPasswordExpiry() == null || user.getResetPasswordExpiry().isBefore(LocalDateTime.now())) {
            return "Invalid or expired token.";
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetPasswordToken(null);
        user.setResetPasswordExpiry(null);
        userRepository.save(user);
        return "success";
    }

    private void sendEmail(User user, String siteURL, String type)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message);
        
        String cleanSiteURL = siteURL.endsWith("/") ? siteURL.substring(0, siteURL.length() - 1) : siteURL;
        
        String link = cleanSiteURL + (type.equals("verify") ? "/verify?code=" : "/reset-password?token=") 
                     + (type.equals("verify") ? user.getVerificationCode() : user.getResetPasswordToken());
        
        helper.setFrom("ralphaveno@gmail.com", "Payroll Support");
        helper.setTo(user.getEmail());
        helper.setSubject(type.equals("verify") ? "Verify Registration" : "Reset Password");
        
        String content = "Dear " + user.getFirstName() + ",<br>Please click the link to " 
                       + (type.equals("verify") ? "verify your account" : "reset your password") + ":<br>"
                       + "<h3><a href=\"" + link + "\">CLICK HERE</a></h3>";
        
        helper.setText(content, true);
        mailSender.send(message);
    }
}