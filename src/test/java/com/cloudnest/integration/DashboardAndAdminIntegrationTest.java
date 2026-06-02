package com.cloudnest.integration;

import com.cloudnest.entity.FileEntity;
import com.cloudnest.entity.User;
import com.cloudnest.repository.FileRepository;
import com.cloudnest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class DashboardAndAdminIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    private User adminUser;
    private User regularUser;

    @BeforeEach
    void setUp() {
        adminUser = new User();
        adminUser.setUsername("admin");
        adminUser.setEmail("admin@cloudnest.com");
        adminUser.setPassword("password123");
        adminUser.setRole("ROLE_ADMIN");
        userRepository.save(adminUser);

        regularUser = new User();
        regularUser.setUsername("user");
        regularUser.setEmail("user@cloudnest.com");
        regularUser.setPassword("password123");
        regularUser.setRole("ROLE_USER");
        userRepository.save(regularUser);
    }

    // --- SECTION 3: DASHBOARD ---

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void testDashboardLoadsFreshUser() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(model().attribute("dashboard", org.hamcrest.Matchers.hasProperty("totalFiles", org.hamcrest.Matchers.equalTo(0L))))
                .andExpect(model().attribute("dashboard", org.hamcrest.Matchers.hasProperty("totalStorageBytes", org.hamcrest.Matchers.equalTo(0L))));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void testDashboardStatsAfterUpload() throws Exception {
        FileEntity file = FileEntity.builder()
                .originalName("test.pdf")
                .storedName("test.pdf")
                .storageNode("node1")
                .fileType("application/pdf")
                .fileSize(1024L) // 1KB
                .user(regularUser)
                .isDeleted(false)
                .build();
        fileRepository.save(file);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("dashboard", org.hamcrest.Matchers.hasProperty("totalFiles", org.hamcrest.Matchers.equalTo(1L))))
                .andExpect(model().attribute("dashboard", org.hamcrest.Matchers.hasProperty("totalStorageBytes", org.hamcrest.Matchers.equalTo(1024L))));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void testDashboardDoesNotCountDeletedFiles() throws Exception {
        FileEntity active = FileEntity.builder().originalName("active.txt").storedName("a.txt").storageNode("node1").fileSize(100L).user(regularUser).isDeleted(false).build();
        FileEntity deleted = FileEntity.builder().originalName("deleted.txt").storedName("d.txt").storageNode("node2").fileSize(500L).user(regularUser).isDeleted(true).build();
        fileRepository.save(active);
        fileRepository.save(deleted);

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("dashboard", org.hamcrest.Matchers.hasProperty("totalFiles", org.hamcrest.Matchers.equalTo(1L)))) // Only active
                .andExpect(model().attribute("dashboard", org.hamcrest.Matchers.hasProperty("totalStorageBytes", org.hamcrest.Matchers.equalTo(100L)))); // Only active
    }

    // --- SECTION 10: ADMIN PANEL ---

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testAdminDashboardAccess() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(model().attributeExists("totalUsers", "totalFiles", "totalStorage"));
    }

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void testAdminDashboardBlockedForRegularUser() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testPromoteUserToAdmin() throws Exception {
        mockMvc.perform(post("/admin/users/toggle-role/" + regularUser.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("success"));

        User updated = userRepository.findById(regularUser.getId()).orElseThrow();
        assertEquals("ROLE_ADMIN", updated.getRole());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testCannotDemoteYourself() throws Exception {
        mockMvc.perform(post("/admin/users/toggle-role/" + adminUser.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"))
                .andExpect(flash().attributeExists("error"));

        User unchanged = userRepository.findById(adminUser.getId()).orElseThrow();
        assertEquals("ROLE_ADMIN", unchanged.getRole());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void testAdminDeleteAnyFile() throws Exception {
        FileEntity file = FileEntity.builder().originalName("userfile.txt").storedName("u.txt").storageNode("node1").user(regularUser).build();
        file = fileRepository.save(file);

        mockMvc.perform(post("/admin/files/delete/" + file.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));

        // File should be permanently deleted by admin
        assertEquals(0, fileRepository.count());
    }

    // --- SECTION 11: INFRASTRUCTURE PAGES ---

    @Test
    @WithMockUser(username = "user", roles = "USER")
    void testInfrastructurePagesLoad() throws Exception {
        mockMvc.perform(get("/nodes")).andExpect(status().isOk());
        mockMvc.perform(get("/deduplication")).andExpect(status().isOk());
        mockMvc.perform(get("/replication")).andExpect(status().isOk());
        mockMvc.perform(get("/network")).andExpect(status().isOk());
        mockMvc.perform(get("/analytics")).andExpect(status().isOk());
        mockMvc.perform(get("/monitoring")).andExpect(status().isOk());
    }

    @Test
    void testInfrastructurePagesRequireLogin() throws Exception {
        mockMvc.perform(get("/nodes")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
        mockMvc.perform(get("/deduplication")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrlPattern("**/login"));
    }
}
