package com.cloudnest.service;

import com.cloudnest.dto.FileDto;
import com.cloudnest.entity.FileEntity;
import com.cloudnest.entity.Folder;
import com.cloudnest.entity.User;
import com.cloudnest.exception.FileNotFoundException;
import com.cloudnest.exception.StorageException;
import com.cloudnest.repository.FileRepository;
import com.cloudnest.repository.FolderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * FileStorageService — Core service for file upload, download, and delete operations.
 *
 * WHY THIS EXISTS:
 * - Handles the business logic for all file operations
 * - Coordinates between the database (FileRepository) and the file system
 * - Uses StorageNodeService to simulate distributed storage
 *
 * HOW FILE UPLOAD WORKS:
 * 1. User selects a file in the browser
 * 2. The file is sent as a MultipartFile to the controller
 * 3. This service generates a UUID filename (prevents collisions)
 * 4. StorageNodeService picks a random node
 * 5. The file is saved to: storage/node{X}/uuid-filename
 * 6. Metadata is saved to the database
 */
@Service
@Transactional
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private final FileRepository fileRepository;
    private final FolderRepository folderRepository;
    private final StorageNodeService storageNodeService;
    private final SharedLinkService sharedLinkService;

    private static final Set<String> BLOCKED_EXTENSIONS = Set.of(".exe", ".bat", ".sh", ".ps1", ".cmd");

    @org.springframework.beans.factory.annotation.Value("${cloudnest.storage.quota-bytes:1073741824}")
    private long quotaBytes;

    public FileStorageService(FileRepository fileRepository,
                              FolderRepository folderRepository,
                              StorageNodeService storageNodeService,
                              @org.springframework.context.annotation.Lazy SharedLinkService sharedLinkService) {
        this.fileRepository = fileRepository;
        this.folderRepository = folderRepository;
        this.storageNodeService = storageNodeService;
        this.sharedLinkService = sharedLinkService;
    }

    /**
     * Upload a file to the distributed storage system.
     *
     * @param file     The uploaded file from the form
     * @param user     The currently logged-in user
     * @param folderId The target folder ID (null for root level)
     * @return FileDto with the uploaded file's information
     */
    public FileDto uploadFile(MultipartFile file, User user, Long folderId) {
        // Validate: file must not be empty
        if (file.isEmpty()) {
            throw new StorageException("Cannot upload an empty file");
        }

        // Quota check
        long currentStorage = fileRepository.sumFileSizeByUser(user);
        if (currentStorage + file.getSize() > quotaBytes) {
            throw new StorageException("Storage quota exceeded. You have reached your 1GB limit.");
        }

        // Generate a unique stored name using UUID to prevent filename collisions
        String originalName = file.getOriginalFilename();
        String extension = "";
        if (originalName != null && originalName.contains(".")) {
            extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
            if (BLOCKED_EXTENSIONS.contains(extension)) {
                throw new StorageException("File type not allowed for security reasons: " + extension);
            }
        }

        // --- HASH-BEFORE-WRITE DEDUPLICATION ---
        // Buffer file bytes in memory (bounded by max-file-size config: 50MB)
        // and compute SHA-256 hash BEFORE touching disk — avoids TOCTOU race conditions.
        byte[] fileBytes;
        String fileHash;
        try {
            fileBytes = file.getBytes();
            fileHash = computeSha256(fileBytes);
        } catch (IOException e) {
            throw new StorageException("Failed to read uploaded file: " + originalName, e);
        }

        String storedName;
        String node;

        // Check if a file with the same content already exists (deduplication)
        FileEntity existingFile = fileRepository.findFirstByFileHash(fileHash);
        if (existingFile != null) {
            // Dedup hit — reuse existing physical file, skip disk write entirely
            storedName = existingFile.getStoredName();
            node = existingFile.getStorageNode();
            log.info("Deduplication triggered — reused existing file for hash: {}", fileHash);
        } else {
            // New unique file — write bytes to disk
            storedName = UUID.randomUUID().toString() + extension;
            node = storageNodeService.selectNode();
            String filePath = storageNodeService.getFilePath(node, storedName);
            Path targetPath = Paths.get(filePath);

            try {
                Files.createDirectories(targetPath.getParent());
                Files.write(targetPath, fileBytes);
            } catch (IOException e) {
                throw new StorageException("Failed to store file: " + originalName, e);
            }
        }

        // Resolve the folder (if specified)
        Folder folder = null;
        if (folderId != null) {
            folder = folderRepository.findByIdAndUser(folderId, user)
                    .orElse(null); // If folder not found, store at root level
        }

        // Save file metadata to the database
        FileEntity fileEntity = FileEntity.builder()
                .originalName(originalName)
                .storedName(storedName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .storageNode(node)
                .fileHash(fileHash)
                .user(user)
                .folder(folder)
                .build();

        if (folder != null) {
            folder.getFiles().add(fileEntity);
        }

        FileEntity saved = fileRepository.save(fileEntity);
        return convertToDto(saved);
    }

    /**
     * Compute SHA-256 hash from a byte array.
     *
     * @param data the file content as bytes
     * @return lowercase hex string of the SHA-256 hash
     */
    private String computeSha256(byte[] data) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            StringBuilder hexString = new StringBuilder(64);
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new StorageException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Download a file — returns the Path to the physical file.
     *
     * @param fileId The database ID of the file
     * @param user   The currently logged-in user (for ownership check)
     * @return Path to the physical file on disk
     */
    public Path getFilePath(Long fileId, User user) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found with ID: " + fileId));

        if (fileEntity.isDeleted()) {
            throw new FileNotFoundException("File not found");
        }

        // Security check: ensure the user owns this file
        if (!fileEntity.getUser().getId().equals(user.getId())) {
            throw new FileNotFoundException("File not found");
        }

        String filePath = storageNodeService.getFilePath(
                fileEntity.getStorageNode(), fileEntity.getStoredName());

        Path path = Paths.get(filePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("File not found on disk: " + fileEntity.getOriginalName());
        }

        return path;
    }

    /**
     * Get the FileEntity for download (to access metadata like original filename).
     */
    public FileEntity getFileEntity(Long fileId, User user) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found with ID: " + fileId));

        if (fileEntity.isDeleted()) {
            throw new FileNotFoundException("File not found");
        }

        if (!fileEntity.getUser().getId().equals(user.getId())) {
            throw new FileNotFoundException("File not found");
        }

        return fileEntity;
    }

    /**
     * Delete a file from both the database and the file system.
     */
    public void deleteFile(Long fileId, User user) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found with ID: " + fileId));

        if (!fileEntity.getUser().getId().equals(user.getId())) {
            throw new FileNotFoundException("File not found");
        }

        // Soft delete — record deletion time for trash auto-purge (30-day retention)
        fileEntity.setDeleted(true);
        fileEntity.setDeletedAt(java.time.LocalDateTime.now());
        fileRepository.save(fileEntity);
    }

    public void restoreFile(Long fileId, User user) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!fileEntity.getUser().getId().equals(user.getId())) {
            throw new FileNotFoundException("File not found");
        }

        fileEntity.setDeleted(false);
        fileEntity.setDeletedAt(null); // Clear deletion timestamp on restore
        fileRepository.save(fileEntity);
    }

    public void permanentDeleteFile(Long fileId, User user) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!fileEntity.getUser().getId().equals(user.getId())) {
            throw new FileNotFoundException("File not found");
        }

        // Check if this physical file is used by any other FileEntity (Deduplication check)
        long referenceCount = fileRepository.countByStoredName(fileEntity.getStoredName());
        
        if (referenceCount <= 1) {
            // Delete the physical file from the storage node ONLY if it's the last reference
            String filePath = storageNodeService.getFilePath(
                    fileEntity.getStorageNode(), fileEntity.getStoredName());
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (IOException e) {
                throw new StorageException("Failed to delete file from disk", e);
            }
        }

        sharedLinkService.deleteLinksForFile(fileEntity.getId());
        fileRepository.delete(fileEntity);
    }

    /**
     * Delete the physical file from disk ONLY if it's the last reference in the database.
     * Used recursively by FolderService without deleting database records manually (relying on cascade instead).
     */
    public void deletePhysicalFileIfLastReference(FileEntity fileEntity) {
        long referenceCount = fileRepository.countByStoredName(fileEntity.getStoredName());
        if (referenceCount <= 1) {
            String filePath = storageNodeService.getFilePath(
                    fileEntity.getStorageNode(), fileEntity.getStoredName());
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (IOException e) {
                throw new StorageException("Failed to delete file from disk", e);
            }
        }
    }

    /**
     * Permanent delete of ANY file without ownership checks (Admin role only).
     */
    public void permanentDeleteFileAdmin(Long fileId) {
        FileEntity fileEntity = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found with ID: " + fileId));

        long referenceCount = fileRepository.countByStoredName(fileEntity.getStoredName());
        if (referenceCount <= 1) {
            String filePath = storageNodeService.getFilePath(
                    fileEntity.getStorageNode(), fileEntity.getStoredName());
            try {
                Files.deleteIfExists(Paths.get(filePath));
            } catch (IOException e) {
                throw new StorageException("Failed to delete file from disk", e);
            }
        }
        
        sharedLinkService.deleteLinksForFile(fileEntity.getId());
        fileRepository.delete(fileEntity);
    }

    public void moveFile(Long fileId, Long targetFolderId, User user) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found"));

        if (!file.getUser().getId().equals(user.getId())) {
            throw new FileNotFoundException("File not found");
        }

        Folder targetFolder = null;
        if (targetFolderId != null) {
            targetFolder = folderRepository.findByIdAndUserAndIsDeletedFalse(targetFolderId, user)
                    .orElseThrow(() -> new IllegalArgumentException("Target folder not found"));
        }

        file.setFolder(targetFolder);
        fileRepository.save(file);
    }

    public List<FileDto> getTrashFiles(User user) {
        return fileRepository.findByUserAndIsDeletedTrueOrderByUploadedAtDesc(user)
                .stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * Get all files for a user, optionally filtered by folder.
     */
    public List<FileDto> getUserFiles(User user, Long folderId) {
        List<FileEntity> files;
        if (folderId != null) {
            files = fileRepository.findByUserAndFolderIdAndIsDeletedFalseOrderByUploadedAtDesc(user, folderId);
        } else {
            files = fileRepository.findByUserAndIsDeletedFalseOrderByUploadedAtDesc(user);
        }
        return files.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Get files at root level (no folder).
     */
    public List<FileDto> getRootFiles(User user) {
        return fileRepository.findByUserAndFolderIsNullAndIsDeletedFalseOrderByUploadedAtDesc(user)
                .stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Search files by name or type.
     */
    public List<FileDto> searchFiles(User user, String query) {
        // Search by name first
        List<FileEntity> results = fileRepository.searchByName(user, query);

        // If no results by name, try searching by type
        if (results.isEmpty()) {
            results = fileRepository.searchByType(user, query);
        }

        return results.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Get the 5 most recent uploads for the dashboard.
     */
    public List<FileDto> getRecentFiles(User user) {
        return fileRepository.findTop5ByUserAndIsDeletedFalseOrderByUploadedAtDesc(user)
                .stream().map(this::convertToDto).collect(Collectors.toList());
    }

    /**
     * Get a file by ID (for sharing).
     */
    public FileEntity getFileById(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException("File not found with ID: " + fileId));
    }

    /**
     * Convert a FileEntity to a FileDto.
     *
     * WHY CONVERT?
     * - DTOs are lightweight and safe to pass to templates
     * - Entities have lazy-loaded relationships that can cause errors in templates
     * - DTOs contain only the data needed for display
     */
    private FileDto convertToDto(FileEntity entity) {
        return FileDto.builder()
                .id(entity.getId())
                .originalName(entity.getOriginalName())
                .fileType(entity.getFileType())
                .fileSize(entity.getFileSize())
                .storageNode(entity.getStorageNode())
                .folderName(entity.getFolder() != null ? entity.getFolder().getName() : null)
                .folderId(entity.getFolder() != null ? entity.getFolder().getId() : null)
                .uploadedAt(entity.getUploadedAt())
                .build();
    }
}
