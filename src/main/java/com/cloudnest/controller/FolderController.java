package com.cloudnest.controller;

import com.cloudnest.entity.User;
import com.cloudnest.service.FolderService;
import com.cloudnest.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import com.cloudnest.dto.FolderDto;

/**
 * FolderController — Handles folder creation and deletion.
 */
@Controller
@RequestMapping("/folders")
public class FolderController {

    private final FolderService folderService;
    private final UserService userService;

    public FolderController(FolderService folderService, UserService userService) {
        this.folderService = folderService;
        this.userService = userService;
    }

    /**
     * POST /folders/create — Create a new folder.
     */
    @PostMapping("/create")
    public String createFolder(@RequestParam("name") String name,
                               @RequestParam(value = "parentId", required = false) Long parentId,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(principal.getName());

        try {
            folderService.createFolder(name, user, parentId);
            redirectAttributes.addFlashAttribute("success",
                    "Folder '" + name + "' created successfully!");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        if (parentId != null) {
            return "redirect:/files?folderId=" + parentId;
        }
        return "redirect:/files";
    }

    /**
     * POST /folders/delete/{id} — Delete a folder and all its contents.
     */
    @PostMapping("/delete/{id}")
    public String deleteFolder(@PathVariable Long id,
                               @RequestParam(value = "parentId", required = false) Long parentId,
                               Principal principal,
                               RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(principal.getName());

        try {
            folderService.deleteFolder(id, user);
            redirectAttributes.addFlashAttribute("success", "Folder deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        if (parentId != null) {
            return "redirect:/files?folderId=" + parentId;
        }
        return "redirect:/files";
    }

    /**
     * GET /folders/download/{id} — Download a folder and its contents as a ZIP.
     */
    @GetMapping("/download/{id}")
    public void downloadFolder(@PathVariable Long id, Principal principal, HttpServletResponse response) {
        User user = userService.findByUsername(principal.getName());
        FolderDto folder = folderService.getFolder(id, user);
        
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + folder.getName() + ".zip\"");
        
        try {
            folderService.downloadFolderAsZip(id, user, response.getOutputStream());
        } catch (Exception e) {
            // In a real app, you might want to redirect with an error message
            throw new RuntimeException("Failed to download folder", e);
        }
    }

    /**
     * POST /folders/move/{id} — Move a folder to another folder.
     */
    @PostMapping("/move/{id}")
    public String moveFolder(@PathVariable Long id,
                             @RequestParam(value = "targetFolderId", required = false) Long targetFolderId,
                             @RequestParam(value = "currentFolderId", required = false) Long currentFolderId,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(principal.getName());
        try {
            folderService.moveFolder(id, targetFolderId, user);
            redirectAttributes.addFlashAttribute("success", "Folder moved successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        if (currentFolderId != null) {
            return "redirect:/files?folderId=" + currentFolderId;
        }
        return "redirect:/files";
    }
}
