package com.cloudnest.service;

import com.cloudnest.dto.UserRegistrationDto;
import com.cloudnest.entity.User;
import com.cloudnest.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

/**
 * UserService — Handles user registration and authentication.
 *
 * WHY THIS EXISTS:
 * - Implements UserDetailsService (required by Spring Security for authentication)
 * - Provides registration logic with validation
 * - Encodes passwords with BCrypt before storing
 *
 * SPRING SECURITY INTEGRATION:
 * - When a user logs in, Spring Security calls loadUserByUsername()
 * - This method fetches the user from the database
 * - Spring Security then compares the entered password with the stored hash
 *
 * @Transactional ensures database operations are wrapped in a transaction
 * (if something fails, all changes are rolled back)
 */
@Service
@Transactional
public class UserService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    // Constructor injection — Spring injects the dependencies automatically
    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Register a new user.
     *
     * Steps:
     * 1. Check if username or email already exists
     * 2. Encode the password with BCrypt
     * 3. Build and save the User entity
     *
     * @param dto The registration form data
     * @return The saved User entity
     * @throws IllegalArgumentException if username/email is taken or passwords don't match
     */
    public User registerUser(UserRegistrationDto dto) {
        // Validate: passwords must match
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        // Validate: username must be unique
        if (userRepository.existsByUsername(dto.getUsername())) {
            throw new IllegalArgumentException("Username '" + dto.getUsername() + "' is already taken");
        }

        // Validate: email must be unique
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new IllegalArgumentException("Email '" + dto.getEmail() + "' is already registered");
        }

        // Public registration must never create administrator accounts.
        String finalRole = "ROLE_USER";

        // Build the User entity with encoded password
        User user = User.builder()
                .username(dto.getUsername())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword())) // BCrypt hash!
                .role(finalRole)
                .build();

        // Save to database and return the saved entity (with generated ID)
        return userRepository.save(user);
    }

    /**
     * Load a user by username — REQUIRED by Spring Security.
     *
     * Spring Security calls this during the login process:
     * 1. User submits username + password
     * 2. Spring Security calls this method with the username
     * 3. We return a UserDetails object (Spring's user representation)
     * 4. Spring Security compares the entered password with the stored hash
     *
     * @param username The username entered in the login form
     * @return UserDetails object for Spring Security
     * @throws UsernameNotFoundException if no user found with that username
     */
    @Override
    public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
        // Find the user by username OR email (case-insensitive)
        User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(loginId, loginId)
                .orElseThrow(() -> {
                    log.debug("Login failed: no user found for input '{}'", loginId);
                    return new UsernameNotFoundException("User not found: " + loginId);
                });

        log.debug("Login attempt for user '{}'", user.getUsername());

        // Convert our User entity to Spring Security's UserDetails using their database role
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
        );
    }

    /**
     * Find a user by username (used by controllers to get the current user).
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found: " + username));
    }
}
