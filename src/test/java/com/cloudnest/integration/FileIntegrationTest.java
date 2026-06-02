package com.cloudnest.integration;

import com.cloudnest.entity.FileEntity;
import com.cloudnest.entity.User;
import com.cloudnest.repository.FileRepository;
import com.cloudnest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class FileIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    private User testUser;
    private User otherUser;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser1");
        testUser.setEmail("test1@cloudnest.com");
        testUser.setPassword("password123");
        testUser.setRole("ROLE_USER");
        userRepository.save(testUser);

        otherUser = new User();
        otherUser.setUsername("testuser2");
        otherUser.setEmail("test2@cloudnest.com");
        otherUser.setPassword("password123");
        otherUser.setRole("ROLE_USER");
        userRepository.save(otherUser);
    }

    @Test
    @WithMockUser(username = "testuser1", roles = "USER")
    void testUploadSingleFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "hello.txt",
                "text/plain",
                "Hello, World!".getBytes()
        );

        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files"))
                .andExpect(flash().attributeExists("success"));

        // Verify it was saved to DB
        assertEquals(1, fileRepository.count());
        FileEntity savedFile = fileRepository.findAll().get(0);
        assertEquals("hello.txt", savedFile.getOriginalName());
    }

    @Test
    @WithMockUser(username = "testuser1", roles = "USER")
    void testUploadEmptyFile() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files")); // No file uploaded

        assertEquals(0, fileRepository.count());
    }

    @Test
    @WithMockUser(username = "testuser1", roles = "USER")
    void testUploadBlockedExtension() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "virus.exe",
                "application/x-msdownload",
                "malicious content".getBytes()
        );

        mockMvc.perform(multipart("/files/upload")
                        .file(file)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        assertEquals(0, fileRepository.count());
    }

    @Test
    @WithMockUser(username = "testuser1", roles = "USER")
    void testDataDeduplication() throws Exception {
        MockMultipartFile fileA = new MockMultipartFile(
                "files", "fileA.txt", "text/plain", "Same Content".getBytes()
        );
        MockMultipartFile fileB = new MockMultipartFile(
                "files", "fileB.txt", "text/plain", "Same Content".getBytes()
        );

        mockMvc.perform(multipart("/files/upload").file(fileA).with(csrf()));
        mockMvc.perform(multipart("/files/upload").file(fileB).with(csrf()));

        assertEquals(2, fileRepository.count());
        FileEntity dbFileA = fileRepository.findAll().get(0);
        FileEntity dbFileB = fileRepository.findAll().get(1);

        // They should have the same hash and stored name
        assertEquals(dbFileA.getFileHash(), dbFileB.getFileHash());
        assertEquals(dbFileA.getStoredName(), dbFileB.getStoredName());
    }

    @Test
    @WithMockUser(username = "testuser1", roles = "USER")
    void testDownloadFile() throws Exception {
        // Upload file first
        MockMultipartFile file = new MockMultipartFile("files", "test.txt", "text/plain", "data".getBytes());
        mockMvc.perform(multipart("/files/upload").file(file).with(csrf()));

        FileEntity savedFile = fileRepository.findAll().get(0);

        mockMvc.perform(get("/files/download/" + savedFile.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"test.txt\""))
                .andExpect(content().bytes("data".getBytes()));
    }

    @Test
    @WithMockUser(username = "testuser2", roles = "USER")
    void testDownloadOtherUsersFileIdor() throws Exception {
        // Create file for user 1
        FileEntity file = FileEntity.builder()
                .originalName("secret.txt")
                .storedName("stored.txt")
                .user(testUser)
                .build();
        file = fileRepository.save(file);

        // Try to access it as testuser2
        mockMvc.perform(get("/files/download/" + file.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser1", roles = "USER")
    void testSoftDeleteFile() throws Exception {
        // Create file
        FileEntity file = FileEntity.builder()
                .originalName("test.txt")
                .storedName("test.txt")
                .storageNode("node1")
                .user(testUser)
                .isDeleted(false)
                .build();
        file = fileRepository.save(file);

        mockMvc.perform(post("/files/delete/" + file.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        FileEntity deletedFile = fileRepository.findById(file.getId()).orElseThrow();
        assertTrue(deletedFile.isDeleted());
        assertNotNull(deletedFile.getDeletedAt());
    }

    @Test
    @WithMockUser(username = "testuser1", roles = "USER")
    void testDownloadSoftDeletedFile() throws Exception {
        FileEntity file = FileEntity.builder()
                .originalName("test.txt")
                .storedName("test.txt")
                .storageNode("node1")
                .user(testUser)
                .isDeleted(true)
                .build();
        file = fileRepository.save(file);

        mockMvc.perform(get("/files/download/" + file.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "testuser1", roles = "USER")
    void testSearchFiles() throws Exception {
        FileEntity file1 = FileEntity.builder().originalName("quarterly_report.pdf").storedName("1.pdf").storageNode("node1").user(testUser).build();
        FileEntity file2 = FileEntity.builder().originalName("image.png").storedName("2.png").storageNode("node1").user(testUser).build();
        FileEntity file3 = FileEntity.builder().originalName("quarterly_budget.xlsx").storedName("3.xlsx").storageNode("node1").user(testUser).isDeleted(true).build();
        fileRepository.save(file1);
        fileRepository.save(file2);
        fileRepository.save(file3);

        mockMvc.perform(get("/files/search").param("query", "quarterly"))
                .andExpect(status().isOk())
                .andExpect(view().name("files"))
                .andExpect(model().attributeExists("files"))
                // Model should not include deleted file
                .andExpect(content().string(org.hamcrest.Matchers.containsString("quarterly_report.pdf")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("quarterly_budget.xlsx"))));
    }
}
