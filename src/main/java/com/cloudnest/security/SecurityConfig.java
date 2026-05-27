package com.cloudnest.security;

import com.cloudnest.service.UserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SecurityConfig — Configures Spring Security for the application.
 *
 * WHY SESSION-BASED AUTH (not JWT)?
 * - Simpler to implement with Thymeleaf (server-side rendering)
 * - Spring Security handles sessions automatically
 * - Perfect for a traditional web application (not a SPA/API)
 * - More beginner-friendly for learning
 *
 * WHAT THIS CONFIGURES:
 * 1. Which pages require login and which are public
 * 2. The login page URL and success redirect
 * 3. The logout behavior
 * 4. Password encoding (BCrypt)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * BCrypt Password Encoder — securely hashes passwords.
     * BCrypt automatically handles salting (adds random data to prevent
     * rainbow table attacks).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Authentication Provider — connects Spring Security to our UserService.
     * Uses UserService.loadUserByUsername() to find users in the database.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserService userService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Authentication Manager — needed for programmatic authentication.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * Security Filter Chain — the main security configuration.
     *
     * This defines:
     * - Public pages (login, register, shared links, static resources)
     * - Protected pages (everything else — requires login)
     * - Login page configuration
     * - Logout behavior
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // Configure URL-based authorization
            .authorizeHttpRequests(auth -> auth
                // These pages are accessible WITHOUT logging in
                .requestMatchers(
                    "/login", "/register",   // Auth pages
                    "/share/**",             // Shared file links (public access)
                    "/css/**", "/js/**",     // Static resources
                    "/images/**",
                    "/webjars/**",
                    "/error",                 // Error page
                    "/actuator/health", "/actuator/info" // Health checks
                ).permitAll()
                // Only users with ADMIN role can access administrative pages
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // ALL other pages require authentication
                .anyRequest().authenticated()
            )
            // Configure the login page
            .formLogin(form -> form
                .loginPage("/login")                    // Custom login page URL
                .loginProcessingUrl("/login")           // Form POST URL
                .defaultSuccessUrl("/dashboard", true)  // After login, go to dashboard
                .failureUrl("/login?error=true")        // On failure, show error
                .usernameParameter("username")          // Form field names
                .passwordParameter("password")
                .permitAll()
            )
            // Configure logout
            .logout(logout -> logout
                .logoutUrl("/logout")                   // Logout URL
                .logoutSuccessUrl("/login?logout=true")  // After logout, go to login
                .invalidateHttpSession(true)            // Destroy the session
                .deleteCookies("JSESSIONID")            // Delete session cookie
                .permitAll()
            );

        return http.build();
    }
}
