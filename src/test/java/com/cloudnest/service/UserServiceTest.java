package com.cloudnest.service;

import com.cloudnest.dto.UserRegistrationDto;
import com.cloudnest.entity.User;
import com.cloudnest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = Mockito.mock(UserRepository.class);
        passwordEncoder = Mockito.mock(PasswordEncoder.class);
        userService = new UserService(userRepository, passwordEncoder);
    }

    @Test
    void testRegisterUser_Success() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("testuser");
        dto.setEmail("test@cloudnest.com");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("test@cloudnest.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        
        User savedUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@cloudnest.com")
                .password("hashedPassword")
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.registerUser(dto);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("testuser", result.getUsername());
        assertEquals("hashedPassword", result.getPassword());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testRegisterUser_MismatchedPasswords() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("testuser");
        dto.setEmail("test@cloudnest.com");
        dto.setPassword("password123");
        dto.setConfirmPassword("password456");

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(dto);
        });

        assertEquals("Passwords do not match", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterUser_DuplicateUsername() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("existinguser");
        dto.setEmail("test@cloudnest.com");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(dto);
        });

        assertEquals("Username 'existinguser' is already taken", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterUser_DuplicateEmail() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("testuser");
        dto.setEmail("existing@cloudnest.com");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");

        when(userRepository.existsByUsername("testuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@cloudnest.com")).thenReturn(true);

        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            userService.registerUser(dto);
        });

        assertEquals("Email 'existing@cloudnest.com' is already registered", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegisterUser_AlwaysAssignsUserRole() {
        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("adminuser");
        dto.setEmail("admin@cloudnest.com");
        dto.setPassword("password123");
        dto.setConfirmPassword("password123");

        when(userRepository.existsByUsername("adminuser")).thenReturn(false);
        when(userRepository.existsByEmail("admin@cloudnest.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        
        User savedUser = User.builder()
                .id(2L)
                .username("adminuser")
                .email("admin@cloudnest.com")
                .password("hashedPassword")
                .role("ROLE_USER")
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.registerUser(dto);

        assertNotNull(result);
        assertEquals("ROLE_USER", result.getRole());
        verify(userRepository).save(argThat(user -> "ROLE_USER".equals(user.getRole())));
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void testLoadUserByUsername_Success() {
        User user = User.builder()
                .username("testuser")
                .password("hashedPassword")
                .role("ROLE_USER")
                .build();
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("testuser", "testuser"))
                .thenReturn(Optional.of(user));

        UserDetails userDetails = userService.loadUserByUsername("testuser");

        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("hashedPassword", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void testLoadUserByUsername_NotFound() {
        when(userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("nonexistent", "nonexistent"))
                .thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.loadUserByUsername("nonexistent");
        });
    }

    @Test
    void testFindByUsername_Success() {
        User user = User.builder()
                .username("testuser")
                .build();
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        User result = userService.findByUsername("testuser");

        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    @Test
    void testFindByUsername_NotFound() {
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () -> {
            userService.findByUsername("nonexistent");
        });
    }
}
