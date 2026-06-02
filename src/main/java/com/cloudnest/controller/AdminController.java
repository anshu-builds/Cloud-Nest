package com.cloudnest.controller;

import com.cloudnest.entity.FileEntity;
import com.cloudnest.entity.User;
import com.cloudnest.repository.FileRepository;
import com.cloudnest.repository.UserRepository;
import com.cloudnest.service.FileStorageService;
import com.cloudnest.util.FormatUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AdminController — Handles all administrative console mappings.
 * Strictly protected via Spring Security (only accessible to ROLE_ADMIN).
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;

    public AdminController(UserRepository userRepository,
                           FileRepository fileRepository,
                           FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.fileRepository = fileRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * GET /admin/dashboard — Render the administrative control panel.
     */
    @GetMapping("/dashboard")
    public String showAdminDashboard(Model model, Principal principal) {
        // System metrics
        long totalUsers = userRepository.count();
        long totalFiles = fileRepository.countActiveFiles();
        long totalStorageBytes = fileRepository.sumTotalFileSize();

        // Node metrics distribution
        long node1Count = 0, node2Count = 0, node3Count = 0;
        for (Object[] row : fileRepository.getNodeStats()) {
            String node = (String) row[0];
            long count = (Long) row[1];
            if ("node1".equals(node)) node1Count = count;
            else if ("node2".equals(node)) node2Count = count;
            else if ("node3".equals(node)) node3Count = count;
        }

        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("totalFiles", totalFiles);
        model.addAttribute("totalStorage", FormatUtils.formatBytes(totalStorageBytes));
        
        model.addAttribute("node1Count", node1Count);
        model.addAttribute("node2Count", node2Count);
        model.addAttribute("node3Count", node3Count);

        // Fetch lists for administration tables
        List<User> usersList = userRepository.findAll();
        List<FileEntity> allFiles = fileRepository.findAll();
        model.addAttribute("users", usersList);
        model.addAttribute("files", allFiles);
        
        // Add active page tag for the sidebar shell
        model.addAttribute("activePage", "admin");

        return "admin"; // Resolves to templates/admin.html
    }

    /**
     * POST /admin/users/toggle-role/{id} — Toggle a user's role between USER and ADMIN.
     */
    @PostMapping("/users/toggle-role/{id}")
    public String toggleUserRole(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User currentUser = userRepository.findByUsername(principal.getName()).orElse(null);
        User targetUser = userRepository.findById(id).orElse(null);

        if (targetUser == null) {
            redirectAttributes.addFlashAttribute("error", "User not found.");
            return "redirect:/admin/dashboard";
        }

        // Prevent self-demotion to preserve at least one admin account
        if (currentUser != null && currentUser.getId().equals(targetUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "You cannot demote your own administrator account.");
            return "redirect:/admin/dashboard";
        }

        if ("ROLE_ADMIN".equals(targetUser.getRole())) {
            targetUser.setRole("ROLE_USER");
            redirectAttributes.addFlashAttribute("success", "User '" + targetUser.getUsername() + "' has been demoted to Standard User.");
        } else {
            targetUser.setRole("ROLE_ADMIN");
            redirectAttributes.addFlashAttribute("success", "User '" + targetUser.getUsername() + "' has been promoted to Administrator.");
        }

        userRepository.save(targetUser);
        return "redirect:/admin/dashboard";
    }

    /**
     * POST /admin/files/delete/{id} — Administrative permanent deletion of any file from disk and DB.
     */
    @PostMapping("/files/delete/{id}")
    public String adminDeleteFile(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            fileStorageService.permanentDeleteFileAdmin(id);
            redirectAttributes.addFlashAttribute("success", "File permanently deleted from the system.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete file: " + e.getMessage());
        }
        return "redirect:/admin/dashboard";
    }
}
