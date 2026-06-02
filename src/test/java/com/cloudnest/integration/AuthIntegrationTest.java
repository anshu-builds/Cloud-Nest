package com.cloudnest.integration;

import com.cloudnest.entity.User;
import com.cloudnest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.logout;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Create a test user before each test
        User user = new User();
        user.setUsername("testuser1");
        user.setEmail("test1@cloudnest.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole("ROLE_USER");
        userRepository.save(user);
    }

    // --- SECTION 1: REGISTRATION ---

    @Test
    void testShowRegistrationPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("user"));
    }

    @Test
    void testSuccessfulRegistration() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "newuser@cloudnest.com")
                        .param("password", "securepass")
                        .param("confirmPassword", "securepass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));
    }

    @Test
    void testRegistrationWithMismatchedPasswords() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "newuser")
                        .param("email", "newuser@cloudnest.com")
                        .param("password", "securepass")
                        .param("confirmPassword", "wrongpass"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void testRegistrationWithDuplicateUsername() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "testuser1") // Already exists
                        .param("email", "different@cloudnest.com")
                        .param("password", "securepass")
                        .param("confirmPassword", "securepass"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void testRegistrationWithDuplicateEmail() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "different")
                        .param("email", "test1@cloudnest.com") // Already exists
                        .param("password", "securepass")
                        .param("confirmPassword", "securepass"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("error"));
    }

    @Test
    void testRegistrationWithInvalidEmail() throws Exception {
        mockMvc.perform(post("/register")
                        .with(csrf())
                        .param("username", "validuser")
                        .param("email", "not-an-email")
                        .param("password", "securepass")
                        .param("confirmPassword", "securepass"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().hasErrors()); // Validation error
    }

    // --- SECTION 2: LOGIN & LOGOUT ---

    @Test
    void testShowLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void testSuccessfulLoginWithUsername() throws Exception {
        mockMvc.perform(formLogin("/login").user("testuser1").password("password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void testSuccessfulLoginWithEmail() throws Exception {
        mockMvc.perform(formLogin("/login").user("test1@cloudnest.com").password("password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void testLoginWithWrongPassword() throws Exception {
        mockMvc.perform(formLogin("/login").user("testuser1").password("wrongpass"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error=true"));
    }

    @Test
    void testCaseInsensitiveLogin() throws Exception {
        mockMvc.perform(formLogin("/login").user("TESTUSER1").password("password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    void testLogout() throws Exception {
        mockMvc.perform(logout("/logout"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?logout=true"));
    }

    @Test
    void testAccessProtectedPageWithoutLogin() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }
}
