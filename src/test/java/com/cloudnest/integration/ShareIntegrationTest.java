package com.cloudnest.integration;

import com.cloudnest.entity.FileEntity;
import com.cloudnest.entity.SharedLink;
import com.cloudnest.entity.User;
import com.cloudnest.repository.FileRepository;
import com.cloudnest.repository.SharedLinkRepository;
import com.cloudnest.repository.UserRepository;
import com.cloudnest.service.StorageNodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.containsString;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class ShareIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private SharedLinkRepository sharedLinkRepository;

    @Autowired
    private StorageNodeService storageNodeService;

    private User testUser;
    private FileEntity testFile;

    @BeforeEach
    void setUp() throws Exception {
        testUser = new User();
        testUser.setUsername("testuser1");
        testUser.setEmail("test1@cloudnest.com");
        testUser.setPassword("password123");
        testUser.setRole("ROLE_USER");
        userRepository.save(testUser);

        testFile = FileEntity.builder()
                .originalName("document.pdf")
                .storedName("share_test_doc.pdf")
                .storageNode("node1")
                .fileType("application/pdf")
                .fileSize(1024L)
                .user(testUser)
                .isDeleted(false)
                .build();
        fileRepository.save(testFile);

        // Ensure physical file exists for download test
        String filePath = storageNodeService.getFilePath("node1", testFile.getStoredName());
        Path path = Paths.get(filePath);
        File dir = path.getParent().toFile();
        if (!dir.exists()) dir.mkdirs();
        if (!Files.exists(path)) {
            Files.writeString(path, "Dummy PDF content");
        }
    }

    @Test
    @WithMockUser(username = "testuser1", roles = "USER")
    void testGenerateShareLink() throws Exception {
        mockMvc.perform(post("/share/generate/" + testFile.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files"))
                .andExpect(flash().attributeExists("shareLink", "success"));

        assertEquals(1, sharedLinkRepository.count());
        SharedLink link = sharedLinkRepository.findAll().get(0);
        assertNotNull(link.getToken());
        assertEquals(testFile.getId(), link.getFile().getId());
    }

    @Test
    void testAccessSharedFileUnauthenticated() throws Exception {
        // Generate link manually
        SharedLink link = new SharedLink();
        link.setToken("abcdef123456");
        link.setFile(testFile);
        link.setCreatedBy(testUser);
        link.setExpiresAt(LocalDateTime.now().plusDays(7));
        sharedLinkRepository.save(link);

        // Access without @WithMockUser (unauthenticated)
        mockMvc.perform(get("/share/" + link.getToken()))
                .andExpect(status().isOk())
                .andExpect(view().name("shared"))
                .andExpect(model().attribute("fileName", "document.pdf"))
                .andExpect(model().attribute("sharedBy", "testuser1"));
    }

    @Test
    void testAccessExpiredShareLink() throws Exception {
        SharedLink link = new SharedLink();
        link.setToken("expired123");
        link.setFile(testFile);
        link.setCreatedBy(testUser);
        link.setExpiresAt(LocalDateTime.now().minusDays(1)); // Expired yesterday
        sharedLinkRepository.save(link);

        mockMvc.perform(get("/share/" + link.getToken()))
                .andExpect(status().isOk())
                .andExpect(view().name("shared"))
                .andExpect(model().attribute("error", "This share link has expired"));
    }

    @Test
    void testAccessDeletedFileViaShareLink() throws Exception {
        // Soft delete the file
        testFile.setDeleted(true);
        fileRepository.save(testFile);

        SharedLink link = new SharedLink();
        link.setToken("deleted123");
        link.setFile(testFile);
        link.setCreatedBy(testUser);
        link.setExpiresAt(LocalDateTime.now().plusDays(7));
        sharedLinkRepository.save(link);

        mockMvc.perform(get("/share/" + link.getToken()))
                .andExpect(status().isOk())
                .andExpect(view().name("shared"))
                .andExpect(model().attribute("error", "This shared file has been deleted by its owner."));
    }

    @Test
    void testDownloadSharedFile() throws Exception {
        SharedLink link = new SharedLink();
        link.setToken("download123");
        link.setFile(testFile);
        link.setCreatedBy(testUser);
        link.setExpiresAt(LocalDateTime.now().plusDays(7));
        sharedLinkRepository.save(link);

        mockMvc.perform(get("/share/download/" + link.getToken()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", containsString("attachment; filename=\"document.pdf\"")))
                .andExpect(content().string("Dummy PDF content"));
    }
}
