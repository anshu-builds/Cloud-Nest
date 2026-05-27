package com.cloudnest.service;

import com.cloudnest.dto.FileDto;
import com.cloudnest.dto.FolderDto;
import com.cloudnest.entity.FileEntity;
import com.cloudnest.entity.Folder;
import com.cloudnest.entity.User;
import com.cloudnest.repository.FileRepository;
import com.cloudnest.repository.FolderRepository;
import com.cloudnest.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FolderServiceTest {

    @Autowired
    private FolderService folderService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FolderRepository folderRepository;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        fileRepository.deleteAll();
        folderRepository.deleteAll();
        userRepository.deleteAll();

        testUser = User.builder()
                .username("foldertester")
                .email("folder@tester.com")
                .password("password")
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testCreateFolder_Success() {
        FolderDto rootFolder = folderService.createFolder("Projects", testUser, null);
        assertNotNull(rootFolder);
        assertEquals("Projects", rootFolder.getName());
        assertNull(rootFolder.getParentId());

        FolderDto nestedFolder = folderService.createFolder("Spring Boot", testUser, rootFolder.getId());
        assertNotNull(nestedFolder);
        assertEquals("Spring Boot", nestedFolder.getName());
        assertEquals(rootFolder.getId(), nestedFolder.getParentId());
    }

    @Test
    void testCreateFolder_DuplicateNameThrowsException() {
        folderService.createFolder("Documents", testUser, null);

        assertThrows(IllegalArgumentException.class, () -> {
            folderService.createFolder("Documents", testUser, null);
        });
    }

    @Test
    void testGetBreadcrumbs() {
        FolderDto f1 = folderService.createFolder("Level1", testUser, null);
        FolderDto f2 = folderService.createFolder("Level2", testUser, f1.getId());
        FolderDto f3 = folderService.createFolder("Level3", testUser, f2.getId());

        List<FolderDto> breadcrumbs = folderService.getBreadcrumbs(f3.getId(), testUser);
        assertNotNull(breadcrumbs);
        assertEquals(3, breadcrumbs.size());
        assertEquals("Level1", breadcrumbs.get(0).getName());
        assertEquals("Level2", breadcrumbs.get(1).getName());
        assertEquals("Level3", breadcrumbs.get(2).getName());
    }

    @Test
    void testPermanentDeleteFolder_RecursivelyDeletesDBAndPhysicalFiles() throws IOException {
        // 1. Create a parent folder
        FolderDto parentFolder = folderService.createFolder("MainFolder", testUser, null);

        // 2. Create a subfolder inside it
        FolderDto subFolder = folderService.createFolder("SubFolder", testUser, parentFolder.getId());

        // 3. Upload a file inside the parent folder
        MockMultipartFile file1 = new MockMultipartFile(
                "files", "parent-file.txt", "text/plain", "Parent Content".getBytes()
        );
        FileDto fileDto1 = fileStorageService.uploadFile(file1, testUser, parentFolder.getId());
        Path path1 = fileStorageService.getFilePath(fileDto1.getId(), testUser);
        assertTrue(Files.exists(path1), "File 1 physical file should exist");

        // 4. Upload a file inside the subfolder
        MockMultipartFile file2 = new MockMultipartFile(
                "files", "sub-file.txt", "text/plain", "Sub Content".getBytes()
        );
        FileDto fileDto2 = fileStorageService.uploadFile(file2, testUser, subFolder.getId());
        Path path2 = fileStorageService.getFilePath(fileDto2.getId(), testUser);
        assertTrue(Files.exists(path2), "File 2 physical file should exist");

        // Verify database counts
        assertEquals(2, folderRepository.count());
        assertEquals(2, fileRepository.count());

        // 5. Permanently delete the parent folder (should recursively delete subfolder, metadata records, and physical files)
        folderService.permanentDeleteFolder(parentFolder.getId(), testUser);

        // Verify database records are cleared
        assertEquals(0, folderRepository.count(), "Folders should be deleted from DB");
        assertEquals(0, fileRepository.count(), "Files should be deleted from DB");

        // Verify physical files are cleaned up from disk
        assertFalse(Files.exists(path1), "File 1 physical file should be deleted from disk");
        assertFalse(Files.exists(path2), "File 2 physical file should be deleted from disk");
    }
}
