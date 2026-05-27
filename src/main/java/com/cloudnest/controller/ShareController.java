package com.cloudnest.controller;

import com.cloudnest.entity.FileEntity;
import com.cloudnest.entity.SharedLink;
import com.cloudnest.entity.User;
import com.cloudnest.exception.FileNotFoundException;
import com.cloudnest.service.FileStorageService;
import com.cloudnest.service.SharedLinkService;
import com.cloudnest.service.StorageNodeService;
import com.cloudnest.service.UserService;
import com.cloudnest.util.FormatUtils;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;

/**
 * ShareController — Handles file sharing via public links.
 *
 * The share flow:
 * 1. Authenticated user generates a share link (POST /share/generate/{fileId})
 * 2. System returns a URL with a unique token
 * 3. Anyone (even unauthenticated) can access GET /share/{token}
 * 4. The shared file page shows file info and a download button
 */
@Controller
@RequestMapping("/share")
public class ShareController {

    private final SharedLinkService sharedLinkService;
    private final FileStorageService fileStorageService;
    private final StorageNodeService storageNodeService;
    private final UserService userService;

    public ShareController(SharedLinkService sharedLinkService,
                           FileStorageService fileStorageService,
                           StorageNodeService storageNodeService,
                           UserService userService) {
        this.sharedLinkService = sharedLinkService;
        this.fileStorageService = fileStorageService;
        this.storageNodeService = storageNodeService;
        this.userService = userService;
    }

    /**
     * POST /share/generate/{fileId} — Generate a shareable link.
     */
    @PostMapping("/generate/{fileId}")
    public String generateShareLink(@PathVariable Long fileId,
                                    Principal principal,
                                    RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(principal.getName());
        FileEntity file = fileStorageService.getFileEntity(fileId, user);

        String token = sharedLinkService.generateShareLink(file, user);
        redirectAttributes.addFlashAttribute("shareLink", "/share/" + token);
        redirectAttributes.addFlashAttribute("success",
                "Share link generated! It will expire in 7 days.");

        return "redirect:/files";
    }

    /**
     * GET /share/{token} — View a shared file (public — no login required).
     */
    @GetMapping("/{token}")
    public String viewSharedFile(@PathVariable String token, Model model) {
        try {
            SharedLink link = sharedLinkService.resolveShareLink(token);
            FileEntity file = link.getFile();

            // Security fix (BUG-07): reject if file was soft-deleted
            if (file.isDeleted()) {
                throw new FileNotFoundException("This shared file has been deleted by its owner.");
            }

            model.addAttribute("fileName", file.getOriginalName());
            model.addAttribute("fileSize", FormatUtils.formatBytes(file.getFileSize()));
            model.addAttribute("fileType", file.getFileType());
            model.addAttribute("uploadedAt", file.getUploadedAt());
            model.addAttribute("sharedBy", link.getCreatedBy().getUsername());
            model.addAttribute("expiresAt", link.getExpiresAt());
            model.addAttribute("token", token);

            return "shared";
        } catch (FileNotFoundException e) {
            model.addAttribute("error", e.getMessage());
            return "shared";
        }
    }

    /**
     * GET /share/download/{token} — Download a shared file.
     */
    @GetMapping("/download/{token}")
    public ResponseEntity<Resource> downloadSharedFile(@PathVariable String token) {
        SharedLink link = sharedLinkService.resolveShareLink(token);
        FileEntity file = link.getFile();

        // Security fix (BUG-07): reject if file was soft-deleted
        if (file.isDeleted()) {
            throw new FileNotFoundException("This shared file has been deleted.");
        }

        String filePath = storageNodeService.getFilePath(
                file.getStorageNode(), file.getStoredName());
        Path path = Paths.get(filePath);

        // Safety: check physical file exists on disk
        if (!java.nio.file.Files.exists(path)) {
            throw new FileNotFoundException("Shared file not found on disk.");
        }

        try {
            Resource resource = new UrlResource(path.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + file.getOriginalName() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new FileNotFoundException("Shared file not found on disk");
        }
    }
}
