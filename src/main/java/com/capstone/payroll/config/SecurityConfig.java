package com.capstone.payroll.config;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository(); 
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserDetailsService userDetailsService) {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, SecurityContextRepository securityContextRepository) throws Exception {
        // FIX: Added "/reset_password" and "/api/verify" to public paths to match the controller safety nets
        String[] publicPaths = {
                "/css/**", "/js/**", "/images/**",
                "/login", "/user_register", "/verify", "/api/verify", "/reset-password", "/reset_password",
                "/api/login", "/api/register", "/api/forgot-password", "/api/reset-password"
        };

        http
        .csrf(csrf -> csrf.disable())
        .authorizeHttpRequests(auth -> auth
            .requestMatchers(publicPaths).permitAll() 
            
            .requestMatchers("/dashboard", "/attendance/**", "/payroll/**", "/api/payroll/**",
                             "/compensation", "/employees", "/api/employees/**",
                             "/payslips", "/payroll-history", "/leave/**", "/overtime", 
                             "/profile", "/notifications")
            .hasAnyAuthority("ROLE_ADMIN", "ADMIN", "Admin", "employee", "EMPLOYEE")
            
            .anyRequest().authenticated()
        )
            .securityContext(context -> context.securityContextRepository(securityContextRepository))
            .formLogin(form -> form.disable())
            .sessionManagement(session -> session.sessionFixation().migrateSession())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
            );

        return http.build();
    }
}