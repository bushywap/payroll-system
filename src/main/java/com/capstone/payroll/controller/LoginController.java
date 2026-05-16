package com.capstone.payroll.controller;

import com.capstone.payroll.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.Map;

@Controller
public class LoginController {

    @Autowired 
    private UserService userService;

    @Autowired 
    private AuthenticationManager authenticationManager;

    @Autowired 
    private SecurityContextRepository securityContextRepository;

    @GetMapping("/login") 
    public String showLoginPage() { 
        return "user_login"; 
    }

    @GetMapping("/user_register") 
    public String showRegisterPage() { 
        return "user_register"; 
    }

    @PostMapping("/api/login")
    @ResponseBody
    public Map<String, Object> handleLogin(@RequestBody Map<String, String> payload, 
                                           HttpServletRequest request, 
                                           HttpServletResponse response) {
        String email = payload.get("email");
        String password = payload.get("password");
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
            );
            
            SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
            securityContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(securityContext);
            
            securityContextRepository.saveContext(securityContext, request, response);
            
            return Map.of("success", true, "message", "Login successful!");
            
        } catch (AuthenticationException e) {
            System.out.println("Login Failed for " + email + ": " + e.getMessage());
            return Map.of("success", false, "message", "Login failed: " + e.getMessage());
        }
    }

    @PostMapping("/api/register")
    @ResponseBody
    public Map<String, Object> handleRegister(@RequestBody Map<String, String> payload) {
        // FIX: Replaced string manipulation with Spring's official bulletproof URL builder
        String url = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        
        try {
            String res = userService.registerUser(payload.get("email"), payload.get("password"), 
                         payload.get("firstName"), payload.get("lastName"), url);
            return Map.of("success", "success".equals(res), "message", res);
        } catch (Exception e) {
            return Map.of("success", false, "message", "Error: " + e.getMessage());
        }
    }

    @GetMapping({"/verify", "/api/verify"})
    public String verifyUser(@RequestParam(value = "code", required = false) String code,
                             @RequestParam(value = "token", required = false) String token) {
        
        String finalCode = (code != null && !code.isEmpty()) ? code : token;
        
        if (finalCode == null || finalCode.isEmpty()) {
            return "verify_fail";
        }
        
        return userService.verify(finalCode) ? "verify_success" : "verify_fail";
    }

    @PostMapping("/api/forgot-password")
    @ResponseBody
    public Map<String, Object> handleForgot(@RequestBody Map<String, String> payload) {
        // FIX: Replaced string manipulation with Spring's official bulletproof URL builder
        String url = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        
        String res = userService.initiatePasswordReset(payload.get("email"), url);
        return Map.of("success", "success".equals(res), "message", 
                      "success".equals(res) ? "Reset link sent to your email." : res);
    }

    @GetMapping({"/reset-password", "/reset_password", "/api/reset-password"})
    public String showResetPage(@RequestParam(value = "token", required = false) String token, 
                                @RequestParam(value = "code", required = false) String code, 
                                Model model) {
        
        String finalToken = (token != null && !token.isEmpty()) ? token : code;
        
        model.addAttribute("token", finalToken);
        return "reset_password";
    }
    
    @PostMapping("/api/reset-password")
    @ResponseBody
    public Map<String, Object> handleReset(@RequestBody Map<String, String> payload) {
        String token = payload.get("token");
        String password = payload.get("password");
        
        if (token == null || password == null) {
            return Map.of("success", false, "message", "Missing token or password.");
        }

        String res = userService.resetPassword(token, password);
        boolean isSuccess = "success".equals(res);
        
        return Map.of(
            "success", isSuccess, 
            "message", isSuccess ? "Password updated successfully!" : res
        );
    }
}