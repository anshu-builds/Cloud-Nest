package com.cloudnest.service;

import com.cloudnest.dto.FileDto;
import com.cloudnest.entity.FileEntity;
import com.cloudnest.entity.User;
import com.cloudnest.exception.StorageException;
import com.cloudnest.repository.FileRepository;
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
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FileStorageServiceTest {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private FileRepository fileRepository;

    @Autowired
    private UserRepository userRepository;

    private User testUser;

    @BeforeEach
    void setUp() {
        // Clear repository to isolate tests
        fileRepository.deleteAll();
        userRepository.deleteAll();

        // Setup a test user
        testUser = User.builder()
                .username("storagetester")
                .email("storage@tester.com")
                .password("password")
                .build();
        testUser = userRepository.save(testUser);
    }

    @Test
    void testUploadFile_Success() throws IOException {
        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                "test-document.txt",
                "text/plain",
                "Hello, CloudNest storage testing!".getBytes()
        );

        FileDto dto = fileStorageService.uploadFile(mockFile, testUser, null);

        assertNotNull(dto);
        assertEquals("test-document.txt", dto.getOriginalName());
        assertEquals("text/plain", dto.getFileType());
        assertEquals(mockFile.getSize(), dto.getFileSize());
        assertNotNull(dto.getStorageNode());

        // Verify the database has the record
        FileEntity entity = fileRepository.findById(dto.getId()).orElse(null);
        assertNotNull(entity);
        assertEquals(testUser.getId(), entity.getUser().getId());

        // Verify physical file exists in isolated test-storage folder
        Path physicalPath = fileStorageService.getFilePath(dto.getId(), testUser);
        assertTrue(Files.exists(physicalPath));
        assertEquals("Hello, CloudNest storage testing!", Files.readString(physicalPath));

        // Clean up physical file
        Files.deleteIfExists(physicalPath);
    }

    @Test
    void testUploadFile_EmptyFileThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "files",
                "empty.txt",
                "text/plain",
                new byte[0]
        );

        assertThrows(StorageException.class, () -> {
            fileStorageService.uploadFile(emptyFile, testUser, null);
        });
    }

    @Test
    void testUploadFile_QuotaExceeded() {
        // Quota is 1GB. Let's mock a file that is too large, or we can mock active usage.
        // Let's create an entity manually in the database that consumes almost all quota
        FileEntity hugeFile = FileEntity.builder()
                .originalName("fake-huge.zip")
                .storedName("fake-huge-uuid")
                .fileType("application/zip")
                .fileSize(1073741820L) // 1GB - 4 bytes
                .storageNode("node1")
                .fileHash("hash123")
                .user(testUser)
                .build();
        fileRepository.save(hugeFile);

        MockMultipartFile overQuotaFile = new MockMultipartFile(
                "files",
                "normal.txt",
                "text/plain",
                "More than 4 bytes of content".getBytes()
        );

        StorageException exception = assertThrows(StorageException.class, () -> {
            fileStorageService.uploadFile(overQuotaFile, testUser, null);
        });

        assertTrue(exception.getMessage().contains("quota exceeded"));
    }

    @Test
    void testDataDeduplication() throws IOException {
        String fileContent = "Deduplication Magic Content";
        MockMultipartFile file1 = new MockMultipartFile(
                "files",
                "original.txt",
                "text/plain",
                fileContent.getBytes()
        );
        MockMultipartFile file2 = new MockMultipartFile(
                "files",
                "duplicate.txt",
                "text/plain",
                fileContent.getBytes()
        );

        // Upload first file
        FileDto dto1 = fileStorageService.uploadFile(file1, testUser, null);
        FileEntity entity1 = fileRepository.findById(dto1.getId()).orElse(null);
        assertNotNull(entity1);

        // Upload second identical file
        FileDto dto2 = fileStorageService.uploadFile(file2, testUser, null);
        FileEntity entity2 = fileRepository.findById(dto2.getId()).orElse(null);
        assertNotNull(entity2);

        // Verify that BOTH records exist in DB but point to the SAME stored filename and node
        assertEquals(entity1.getStoredName(), entity2.getStoredName());
        assertEquals(entity1.getStorageNode(), entity2.getStorageNode());
        assertEquals(entity1.getFileHash(), entity2.getFileHash());

        // Verify only ONE physical file exists on disk
        Path physicalPath = fileStorageService.getFilePath(dto1.getId(), testUser);
        assertTrue(Files.exists(physicalPath));

        // Delete first file metadata (should NOT delete the physical file since entity2 is still referencing it)
        fileStorageService.permanentDeleteFile(dto1.getId(), testUser);
        assertTrue(Files.exists(physicalPath), "Physical file should still exist as it is referenced by another entity");

        // Delete second file metadata (should delete the physical file as it's the last reference)
        fileStorageService.permanentDeleteFile(dto2.getId(), testUser);
        assertFalse(Files.exists(physicalPath), "Physical file should be deleted now that no entity references it");
    }

    @Test
    void testSoftDeleteAndRestore() {
        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                "doc.txt",
                "text/plain",
                "Soft Delete Test".getBytes()
        );
        FileDto dto = fileStorageService.uploadFile(mockFile, testUser, null);

        // Active files check
        List<FileDto> activeFiles = fileStorageService.getUserFiles(testUser, null);
        assertTrue(activeFiles.stream().anyMatch(f -> f.getId().equals(dto.getId())));

        // Soft delete
        fileStorageService.deleteFile(dto.getId(), testUser);

        // Should not be in active files
        activeFiles = fileStorageService.getUserFiles(testUser, null);
        assertFalse(activeFiles.stream().anyMatch(f -> f.getId().equals(dto.getId())));

        // Should be in trash
        List<FileDto> trashFiles = fileStorageService.getTrashFiles(testUser);
        assertTrue(trashFiles.stream().anyMatch(f -> f.getId().equals(dto.getId())));

        // Restore
        fileStorageService.restoreFile(dto.getId(), testUser);

        // Should be back in active files
        activeFiles = fileStorageService.getUserFiles(testUser, null);
        assertTrue(activeFiles.stream().anyMatch(f -> f.getId().equals(dto.getId())));

        // Clean up
        fileStorageService.permanentDeleteFile(dto.getId(), testUser);
    }
}
