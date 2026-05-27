package com.cloudnest.controller;

import com.cloudnest.dto.FileDto;
import com.cloudnest.dto.FolderDto;
import com.cloudnest.entity.User;
import com.cloudnest.service.FileStorageService;
import com.cloudnest.service.FolderService;
import com.cloudnest.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/trash")
public class TrashController {

    private final FileStorageService fileStorageService;
    private final FolderService folderService;
    private final UserService userService;

    public TrashController(FileStorageService fileStorageService,
                           FolderService folderService,
                           UserService userService) {
        this.fileStorageService = fileStorageService;
        this.folderService = folderService;
        this.userService = userService;
    }

    @GetMapping
    public String viewTrash(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());

        List<FileDto> deletedFiles = fileStorageService.getTrashFiles(user);
        List<FolderDto> deletedFolders = folderService.getTrashFolders(user);

        model.addAttribute("files", deletedFiles);
        model.addAttribute("folders", deletedFolders);

        return "trash";
    }

    @PostMapping("/restore/file/{id}")
    public String restoreFile(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(principal.getName());
        try {
            fileStorageService.restoreFile(id, user);
            redirectAttributes.addFlashAttribute("success", "File restored successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error restoring file: " + e.getMessage());
        }
        return "redirect:/trash";
    }

    @PostMapping("/delete/file/{id}")
    public String permanentDeleteFile(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(principal.getName());
        try {
            fileStorageService.permanentDeleteFile(id, user);
            redirectAttributes.addFlashAttribute("success", "File permanently deleted!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting file: " + e.getMessage());
        }
        return "redirect:/trash";
    }

    @PostMapping("/restore/folder/{id}")
    public String restoreFolder(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(principal.getName());
        try {
            folderService.restoreFolder(id, user);
            redirectAttributes.addFlashAttribute("success", "Folder restored successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error restoring folder: " + e.getMessage());
        }
        return "redirect:/trash";
    }

    @PostMapping("/delete/folder/{id}")
    public String permanentDeleteFolder(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.findByUsername(principal.getName());
        try {
            folderService.permanentDeleteFolder(id, user);
            redirectAttributes.addFlashAttribute("success", "Folder permanently deleted!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error deleting folder: " + e.getMessage());
        }
        return "redirect:/trash";
    }
}
