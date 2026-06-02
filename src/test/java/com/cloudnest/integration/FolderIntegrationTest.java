package com.cloudnest.integration;

import com.cloudnest.entity.FileEntity;
import com.cloudnest.entity.Folder;
import com.cloudnest.entity.User;
import com.cloudnest.repository.FileRepository;
import com.cloudnest.repository.FolderRepository;
import com.cloudnest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class FolderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private FolderRepository folderRepository;
    
    @Autowired
    private FileRepository fileRepository;

    private User testUser;
    private User otherUser;

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
    void testCreateRootFolder() throws Exception {
        mockMvc.perform(post("/folders/create")
                        .with(csrf())
                        .param("name", "Projects"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files"))
                .andExpect(flash().attributeExists("success"));

        assertEquals(1, folderRepository.count());
        Folder folder = folderRepository.findAll().get(0);
        assertEquals("Projects", folder.getName());
        assertNull(folder.getParent());
    }

    @Test
    @WithMockUser(username = "testuser1", roles = "USER")
    void testCreateNestedFolder() throws Exception {
        Folder root = new Folder();
        root.setName("Projects");
        root.setUser(testUser);
        root = folderRepository.save(root);

        mockMvc.perform(post("/folders/create")
                        .with(csrf())
                        .param("name", "Spring Boot")
                        .param("parentId", root.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files?folderId=" + root.getId()));

        assertEquals(2, folderRepository.count());
        Folder child = folderRepository.findByUserAndParentIdAndIsDeletedFalseOrderByNameAsc(testUser, root.getId())
                .stream().filter(f -> f.getName().equals("Spring Boot")).findFirst().orElseThrow();
        assertEquals(root.getId(), child.getParent().getId());
    }

    @Test
    @WithMockUser(username = "testuser1", roles = "USER")
    void testCreateDuplicateFolderAtSameLevel() throws Exception {
        Folder root = new Folder();
        root.setName("Projects");
        root.setUser(testUser);
        folderRepository.save(root);

        mockMvc.perform(post("/folders/create")
                        .with(csrf())
                        .param("name", "Projects"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        // Still only 1 folder
        assertEquals(1, folderRepository.count());
    }
    
    @Test
    @WithMockUser(username = "testuser1", roles = "USER")
    void testCreateDuplicateFolderAtDifferentLevel() throws Exception {
        Folder root = new Folder();
        root.setName("Projects");
        root.setUser(testUser);
        root = folderRepository.save(root);

        // Creating "Projects" again, but this time INSIDE the root "Projects"
        mockMvc.perform(post("/folders/create")
                        .with(csrf())
                        .param("name", "Projects")
                        .param("parentId", root.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        assertEquals(2, folderRepository.count());
    }

    @Test
    @WithMockUser(username = "testuser1", roles = "USER")
    void testCreateFolderWithInvalidName() throws Exception {
        mockMvc.perform(post("/folders/create")
                        .with(csrf())
                        .param("name", "../etc/passwd"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));

        assertEquals(0, folderRepository.count());
    }

    @Test
    @WithMockUser(username = "testuser1", roles = "USER")
    void testSoftDeleteFolderCascade() throws Exception {
        // Create root folder
        Folder root = new Folder();
        root.setName("Projects");
        root.setUser(testUser);
        root = folderRepository.save(root);
        
        // Create file inside folder
        FileEntity file = FileEntity.builder()
                .originalName("test.txt")
                .storedName("test.txt")
                .storageNode("node1")
                .user(testUser)
                .folder(root)
                .isDeleted(false)
                .build();
        file = fileRepository.save(file);
        
        root.getFiles().add(file);
        folderRepository.save(root);

        // Soft delete the folder
        mockMvc.perform(post("/folders/delete/" + root.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("success"));

        // Verify folder is deleted
        Folder deletedFolder = folderRepository.findById(root.getId()).orElseThrow();
        assertTrue(deletedFolder.isDeleted());
        
        // Verify file is also soft-deleted (cascade)
        FileEntity deletedFile = fileRepository.findAll().get(0);
        assertTrue(deletedFile.isDeleted());
    }

    @Test
    @WithMockUser(username = "testuser1", roles = "USER")
    void testDownloadFolderAsZip() throws Exception {
        Folder root = new Folder();
        root.setName("Downloads");
        root.setUser(testUser);
        root = folderRepository.save(root);
        
        // Add a dummy file
        FileEntity file = FileEntity.builder()
                .originalName("test.txt")
                .storedName("test.txt") // Note: mock storage won't actually have this file on disk, which might cause IOException in service.
                .storageNode("node1")
                .user(testUser)
                .folder(root)
                .isDeleted(false)
                .build();
        fileRepository.save(file);
        
        // Since we don't have the actual file on disk in integration test, 
        // downloading as ZIP will throw a FileNotFoundException internally, 
        // which the global exception handler catches. 
        // We will just verify it hits the endpoint and either returns zip OR handles the missing file gracefully.
        mockMvc.perform(get("/folders/download/" + root.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/zip"));
    }

    @Test
    @WithMockUser(username = "testuser1", roles = "USER")
    void testMoveFolderCyclePrevention() throws Exception {
        // Create A -> B
        Folder folderA = new Folder();
        folderA.setName("A");
        folderA.setUser(testUser);
        folderA = folderRepository.save(folderA);
        
        Folder folderB = new Folder();
        folderB.setName("B");
        folderB.setParent(folderA);
        folderB.setUser(testUser);
        folderB = folderRepository.save(folderB);

        // Try to move A into B (cycle)
        mockMvc.perform(post("/folders/move/" + folderA.getId())
                        .with(csrf())
                        .param("targetFolderId", folderB.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error")); // "Cannot move a folder into its own descendant"
                
        // Verify it wasn't moved
        Folder checkA = folderRepository.findById(folderA.getId()).orElseThrow();
        assertNull(checkA.getParent());
    }
    
    @Test
    @WithMockUser(username = "testuser2", roles = "USER")
    void testIDORDeleteOtherUsersFolder() throws Exception {
        Folder folderA = new Folder();
        folderA.setName("A");
        folderA.setUser(testUser);
        folderA = folderRepository.save(folderA);
        
        // testuser2 trying to delete testuser1's folder
        mockMvc.perform(post("/folders/delete/" + folderA.getId()).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }
}
