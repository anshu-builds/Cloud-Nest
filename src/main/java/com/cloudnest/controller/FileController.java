package com.cloudnest.controller;

import com.cloudnest.dto.FileDto;
import com.cloudnest.dto.FolderDto;
import com.cloudnest.entity.FileEntity;
import com.cloudnest.entity.User;
import com.cloudnest.service.FileStorageService;
import com.cloudnest.service.FolderService;
import com.cloudnest.service.UserService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.security.Principal;
import java.util.List;

/**
 * FileController — Handles file upload, download, delete, and search.
 */
@Controller
@RequestMapping("/files")
public class FileController {

    private final FileStorageService fileStorageService;
    private final FolderService folderService;
    private final UserService userService;

    public FileController(FileStorageService fileStorageService,
                          FolderService folderService,
                          UserService userService) {
        this.fileStorageService = fileStorageService;
        this.folderService = folderService;
        this.userService = userService;
    }

    /**
     * GET /files — List all user files (optionally filtered by folder).
     */
    @GetMapping
    public String listFiles(@RequestParam(required = false) Long folderId,
                            Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());

        List<FileDto> files;
        List<FolderDto> folders;
        List<FolderDto> breadcrumbs = null;
        FolderDto currentFolder = null;

        if (folderId != null) {
            // Show files and sub-folders inside a specific folder
            files = fileStorageService.getUserFiles(user, folderId);
            folders = folderService.getSubFolders(user, folderId);
            breadcrumbs = folderService.getBreadcrumbs(folderId, user);
            currentFolder = folderService.getFolder(folderId, user);
        } else {
            // Show root-level files and folders
            files = fileStorageService.getRootFiles(user);
            folders = folderService.getRootFolders(user);
        }

        // Get all folders for the "Move to folder" dropdown
        List<FolderDto> allFolders = folderService.getAllFolders(user);

        model.addAttribute("files", files);
        model.addAttribute("folders", folders);
        model.addAttribute("allFolders", allFolders);
        model.addAttribute("breadcrumbs", breadcrumbs);
        model.addAttribute("currentFolder", currentFolder);
        model.addAttribute("currentFolderId", folderId);

        return "files";
    }

    /**
     * POST /files/upload — Upload a file.
     */
    @PostMapping("/upload")
    public String uploadFiles(@RequestParam("files") List<MultipartFile> files,
                              @RequestParam(value = "folderId", required = false) Long folderId,
                              Principal principal,
                              RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(principal.getName());

        int successCount = 0;
        int errorCount = 0;
        StringBuilder errorMessages = new StringBuilder();

        for (MultipartFile file : files) {
            if (file.isEmpty()) continue;
            try {
                fileStorageService.uploadFile(file, user, folderId);
                successCount++;
            } catch (Exception e) {
                errorCount++;
                errorMessages.append("Failed to upload ").append(file.getOriginalFilename()).append(": ").append(e.getMessage()).append("<br>");
            }
        }

        if (successCount > 0) {
            redirectAttributes.addFlashAttribute("success", successCount + " file(s) uploaded successfully!");
        }
        if (errorCount > 0) {
            redirectAttributes.addFlashAttribute("error", "Error uploading " + errorCount + " file(s).");
        }

        // Redirect back to the same folder
        if (folderId != null) {
            return "redirect:/files?folderId=" + folderId;
        }
        return "redirect:/files";
    }

    /**
     * GET /files/download/{id} — Download a file.
     */
    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id,
                                                  Principal principal) {
        User user = userService.findByUsername(principal.getName());
        FileEntity fileEntity = fileStorageService.getFileEntity(id, user);
        Path filePath = fileStorageService.getFilePath(id, user);

        try {
            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + fileEntity.getOriginalName() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new com.cloudnest.exception.FileNotFoundException("File not found");
        }
    }

    /**
     * GET /files/preview/{id} — Preview a file directly in the browser.
     */
    @GetMapping("/preview/{id}")
    public ResponseEntity<Resource> previewFile(@PathVariable Long id, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        FileEntity fileEntity = fileStorageService.getFileEntity(id, user);
        Path filePath = fileStorageService.getFilePath(id, user);

        try {
            Resource resource = new UrlResource(filePath.toUri());
            String contentType = fileEntity.getFileType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + fileEntity.getOriginalName() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new com.cloudnest.exception.FileNotFoundException("File not found");
        }
    }
    /**
     * POST /files/delete/{id} — Delete a file.
     */
    @PostMapping("/delete/{id}")
    public String deleteFile(@PathVariable Long id,
                             @RequestParam(value = "folderId", required = false) Long folderId,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(principal.getName());

        try {
            fileStorageService.deleteFile(id, user);
            redirectAttributes.addFlashAttribute("success", "File deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        if (folderId != null) {
            return "redirect:/files?folderId=" + folderId;
        }
        return "redirect:/files";
    }

    /**
     * POST /files/move/{id} — Move a file to another folder.
     */
    @PostMapping("/move/{id}")
    public String moveFile(@PathVariable Long id,
                           @RequestParam(value = "targetFolderId", required = false) Long targetFolderId,
                           @RequestParam(value = "currentFolderId", required = false) Long currentFolderId,
                           Principal principal,
                           RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(principal.getName());
        try {
            fileStorageService.moveFile(id, targetFolderId, user);
            redirectAttributes.addFlashAttribute("success", "File moved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        if (currentFolderId != null) {
            return "redirect:/files?folderId=" + currentFolderId;
        }
        return "redirect:/files";
    }

    /**
     * GET /files/search — Search files by name or type.
     */
    @GetMapping("/search")
    public String searchFiles(@RequestParam("query") String query,
                              Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());
        List<FileDto> files = fileStorageService.searchFiles(user, query);
        List<FolderDto> allFolders = folderService.getAllFolders(user);

        model.addAttribute("files", files);
        model.addAttribute("folders", List.of()); // No folders in search results
        model.addAttribute("allFolders", allFolders);
        model.addAttribute("searchQuery", query);
        model.addAttribute("searchMode", true);

        return "files";
    }
}
