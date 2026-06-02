package com.cloudnest.service;

import com.cloudnest.dto.FileDto;
import com.cloudnest.dto.FolderDto;
import com.cloudnest.entity.Folder;
import com.cloudnest.entity.User;
import com.cloudnest.repository.FileRepository;
import com.cloudnest.repository.FolderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import com.cloudnest.entity.FileEntity;

/**
 * FolderService — Handles folder creation, listing, and navigation.
 */
@Service
@Transactional
public class FolderService {

    private final FolderRepository folderRepository;
    private final FileRepository fileRepository;
    private final StorageNodeService storageNodeService;
    private final FileStorageService fileStorageService;

    public FolderService(FolderRepository folderRepository,
                         FileRepository fileRepository,
                         StorageNodeService storageNodeService,
                         @org.springframework.context.annotation.Lazy FileStorageService fileStorageService) {
        this.folderRepository = folderRepository;
        this.fileRepository = fileRepository;
        this.storageNodeService = storageNodeService;
        this.fileStorageService = fileStorageService;
    }

    public FolderDto createFolder(String name, User user, Long parentId) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Folder name cannot be empty");
        }
        // Security (Violation 11): prevent path traversal characters in folder names
        if (name.contains("..") || name.contains("/") || name.contains("\\")
                || name.contains("\0") || name.length() > 255) {
            throw new IllegalArgumentException("Folder name contains invalid characters");
        }
        String trimmedName = name.trim();

        boolean exists;
        if (parentId == null) {
            exists = folderRepository.existsByUserAndNameAndParentIsNullAndIsDeletedFalse(user, trimmedName);
        } else {
            exists = folderRepository.existsByUserAndNameAndParentIdAndIsDeletedFalse(user, trimmedName, parentId);
        }
        if (exists) {
            throw new IllegalArgumentException("A folder named '" + trimmedName + "' already exists here");
        }

        Folder parent = null;
        if (parentId != null) {
            parent = folderRepository.findByIdAndUserAndIsDeletedFalse(parentId, user)
                    .orElseThrow(() -> new IllegalArgumentException("Parent folder not found"));
        }

        Folder folder = Folder.builder().name(trimmedName).user(user).parent(parent).build();
        if (parent != null) {
            parent.getSubFolders().add(folder);
        }
        Folder saved = folderRepository.save(folder);
        return convertToDto(saved);
    }

    public List<FolderDto> getRootFolders(User user) {
        return folderRepository.findByUserAndParentIsNullAndIsDeletedFalseOrderByNameAsc(user)
                .stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public List<FolderDto> getSubFolders(User user, Long parentId) {
        return folderRepository.findByUserAndParentIdAndIsDeletedFalseOrderByNameAsc(user, parentId)
                .stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public FolderDto getFolder(Long folderId, User user) {
        Folder folder = folderRepository.findByIdAndUserAndIsDeletedFalse(folderId, user)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        return convertToDto(folder);
    }

    public void deleteFolder(Long folderId, User user) {
        Folder folder = folderRepository.findByIdAndUserAndIsDeletedFalse(folderId, user)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        // BUG-05 FIX: Cascade soft-delete to all files and subfolders recursively
        // so they don't leak into search results or quota calculations.
        softDeleteRecursively(folder);
    }

    /**
     * Recursively soft-deletes a folder tree: marks the folder, its files,
     * and all descendant subfolders + their files as deleted.
     */
    private void softDeleteRecursively(Folder folder) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        folder.setDeleted(true);
        folder.setDeletedAt(now);
        folderRepository.save(folder);

        // Soft-delete all files in this folder
        if (folder.getFiles() != null) {
            for (FileEntity file : folder.getFiles()) {
                file.setDeleted(true);
                file.setDeletedAt(now);
            }
            fileRepository.saveAll(folder.getFiles());
        }

        // Recurse into subfolders
        List<Folder> subFolders = folderRepository.findByParentAndUserAndIsDeletedFalse(folder, folder.getUser());
        for (Folder sub : subFolders) {
            softDeleteRecursively(sub);
        }
    }

    public void restoreFolder(Long folderId, User user) {
        Folder folder = folderRepository.findByIdAndUser(folderId, user)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        // BUG-05 FIX: Cascade restore to all files and subfolders recursively
        restoreRecursively(folder);
    }

    /**
     * Recursively restores a folder tree: unmarks the folder, its files,
     * and all descendant subfolders + their files.
     */
    private void restoreRecursively(Folder folder) {
        folder.setDeleted(false);
        folder.setDeletedAt(null);
        folderRepository.save(folder);

        // Restore all files in this folder
        if (folder.getFiles() != null) {
            for (FileEntity file : folder.getFiles()) {
                file.setDeleted(false);
                file.setDeletedAt(null);
            }
            fileRepository.saveAll(folder.getFiles());
        }

        // Recurse into subfolders (find deleted ones since they were cascade-deleted)
        List<Folder> subFolders = folderRepository.findByParentAndUser(folder, folder.getUser());
        for (Folder sub : subFolders) {
            restoreRecursively(sub);
        }
    }

    public void permanentDeleteFolder(Long folderId, User user) {
        Folder folder = folderRepository.findByIdAndUser(folderId, user)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
        
        // Actually physically delete all files within
        deleteFolderRecursively(folder, user);
        
        folderRepository.delete(folder);
    }

    public void moveFolder(Long folderId, Long targetFolderId, User user) {
        Folder folder = folderRepository.findByIdAndUserAndIsDeletedFalse(folderId, user)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        if (targetFolderId != null && folderId.equals(targetFolderId)) {
            throw new IllegalArgumentException("Cannot move a folder into itself");
        }

        Folder targetFolder = null;
        if (targetFolderId != null) {
            targetFolder = folderRepository.findByIdAndUserAndIsDeletedFalse(targetFolderId, user)
                    .orElseThrow(() -> new IllegalArgumentException("Target folder not found"));

            // BUG-10 FIX: Walk up from target to root, checking for cycles.
            // Moving folder A into a descendant of A would create an infinite loop.
            Folder ancestor = targetFolder;
            while (ancestor != null) {
                if (ancestor.getId().equals(folderId)) {
                    throw new IllegalArgumentException("Cannot move a folder into its own descendant");
                }
                ancestor = ancestor.getParent();
            }
        }

        folder.setParent(targetFolder);
        folderRepository.save(folder);
    }

    private void deleteFolderRecursively(Folder folder, User user) {
        // Delete all physical files in the current folder from disk
        if (folder.getFiles() != null) {
            for (FileEntity file : folder.getFiles()) {
                fileStorageService.deletePhysicalFileIfLastReference(file);
            }
        }

        // Recursively clean up physical files in subfolders
        if (folder.getSubFolders() != null) {
            for (Folder subFolder : folder.getSubFolders()) {
                deleteFolderRecursively(subFolder, user);
            }
        }
    }

    public void downloadFolderAsZip(Long folderId, User user, OutputStream outputStream) {
        Folder folder = folderRepository.findByIdAndUser(folderId, user)
                .orElseThrow(() -> new IllegalArgumentException("Folder not found"));

        try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
            zipFolder(folder, "", zos);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate zip file", e);
        }
    }

    private void zipFolder(Folder folder, String currentPath, ZipOutputStream zos) throws Exception {
        String newPath = currentPath + folder.getName() + "/";

        // Add empty folder entry
        zos.putNextEntry(new ZipEntry(newPath));
        zos.closeEntry();

        // Add files
        for (FileEntity fileEntity : folder.getFiles()) {
            if (fileEntity.isDeleted()) continue;
            String filePath = storageNodeService.getFilePath(fileEntity.getStorageNode(), fileEntity.getStoredName());
            Path path = Paths.get(filePath);

            if (Files.exists(path)) {
                ZipEntry zipEntry = new ZipEntry(newPath + fileEntity.getOriginalName());
                zos.putNextEntry(zipEntry);
                try (InputStream is = Files.newInputStream(path)) {
                    is.transferTo(zos);
                }
                zos.closeEntry();
            }
        }

        // Recursively add sub-folders
        for (Folder subFolder : folder.getSubFolders()) {
            if (subFolder.isDeleted()) continue;
            zipFolder(subFolder, newPath, zos);
        }
    }

    public List<FolderDto> getBreadcrumbs(Long folderId, User user) {
        List<FolderDto> breadcrumbs = new ArrayList<>();
        Folder current = folderRepository.findByIdAndUser(folderId, user).orElse(null);
        while (current != null) {
            breadcrumbs.add(convertToDto(current));
            current = current.getParent();
        }
        Collections.reverse(breadcrumbs);
        return breadcrumbs;
    }

    public List<FolderDto> getAllFolders(User user) {
        return folderRepository.findByUserAndIsDeletedFalseOrderByNameAsc(user)
                .stream().map(this::convertToDto).collect(Collectors.toList());
    }

    public long countByUser(User user) {
        return folderRepository.countByUserAndIsDeletedFalse(user);
    }

    public List<FolderDto> getTrashFolders(User user) {
        return folderRepository.findByUserAndIsDeletedTrueOrderByNameAsc(user)
                .stream().map(this::convertToDto).collect(Collectors.toList());
    }

    private FolderDto convertToDto(Folder folder) {
        return FolderDto.builder()
                .id(folder.getId())
                .name(folder.getName())
                .parentId(folder.getParent() != null ? folder.getParent().getId() : null)
                .parentName(folder.getParent() != null ? folder.getParent().getName() : null)
                .fileCount(folder.getFiles() != null ? (int) folder.getFiles().stream().filter(f -> !f.isDeleted()).count() : 0)
                .subFolderCount(folder.getSubFolders() != null ? (int) folder.getSubFolders().stream().filter(f -> !f.isDeleted()).count() : 0)
                .createdAt(folder.getCreatedAt())
                .build();
    }
}
