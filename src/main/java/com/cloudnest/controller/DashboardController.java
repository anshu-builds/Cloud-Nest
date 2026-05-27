package com.cloudnest.controller;

import com.cloudnest.dto.DashboardDto;
import com.cloudnest.dto.FileDto;
import com.cloudnest.entity.User;
import com.cloudnest.repository.FileRepository;
import com.cloudnest.util.FormatUtils;
import com.cloudnest.service.FileStorageService;
import com.cloudnest.service.FolderService;
import com.cloudnest.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * DashboardController — Displays the main dashboard with statistics.
 *
 * The dashboard shows:
 * - Total uploaded files count
 * - Total storage usage (formatted)
 * - Total folders count
 * - 5 most recent uploads
 * - File distribution across storage nodes (for the distributed storage demo)
 * - File type distribution
 */
@Controller
public class DashboardController {

    private final UserService userService;
    private final FileStorageService fileStorageService;
    private final FolderService folderService;
    private final FileRepository fileRepository;

    @org.springframework.beans.factory.annotation.Value("${cloudnest.storage.quota-bytes:1073741824}")
    private long quotaBytes;

    public DashboardController(UserService userService,
                               FileStorageService fileStorageService,
                               FolderService folderService,
                               FileRepository fileRepository) {
        this.userService = userService;
        this.fileStorageService = fileStorageService;
        this.folderService = folderService;
        this.fileRepository = fileRepository;
    }

    /**
     * GET /dashboard — Show the dashboard page with statistics.
     *
     * @param principal Spring Security injects the logged-in user's identity
     */
    @GetMapping("/dashboard")
    public String showDashboard(Principal principal, Model model) {
        // Get the current logged-in user
        User user = userService.findByUsername(principal.getName());

        // Gather statistics
        long totalFiles = fileRepository.countByUserAndIsDeletedFalse(user);
        long totalStorage = fileRepository.sumFileSizeByUser(user);
        long totalFolders = folderService.countByUser(user);
        List<FileDto> recentFiles = fileStorageService.getRecentFiles(user);

        // Build node distribution map (for the distributed storage chart)
        Map<String, Long> nodeDistribution = new LinkedHashMap<>();
        for (Object[] row : fileRepository.countByStorageNode(user)) {
            nodeDistribution.put((String) row[0], (Long) row[1]);
        }

        // Build file type distribution map
        Map<String, Long> typeDistribution = new LinkedHashMap<>();
        for (Object[] row : fileRepository.countByFileType(user)) {
            String type = (String) row[0];
            if (type != null) {
                // Simplify MIME types for display (e.g., "application/pdf" → "PDF")
                type = simplifyFileType(type);
            } else {
                type = "Other";
            }
            typeDistribution.merge(type, (Long) row[1], Long::sum);
        }

        // Calculate Quota Percentage
        int quotaPercentage = (int) ((totalStorage * 100) / quotaBytes);
        if (quotaPercentage > 100) quotaPercentage = 100;

        // Build the dashboard DTO
        DashboardDto dashboard = DashboardDto.builder()
                .totalFiles(totalFiles)
                .totalFolders(totalFolders)
                .totalStorageBytes(totalStorage)
                .formattedStorage(FormatUtils.formatBytes(totalStorage))
                .quotaBytes(quotaBytes)
                .formattedQuota(FormatUtils.formatBytes(quotaBytes))
                .quotaPercentage(quotaPercentage)
                .recentFiles(recentFiles)
                .nodeDistribution(nodeDistribution)
                .fileTypeDistribution(typeDistribution)
                .build();

        model.addAttribute("dashboard", dashboard);
        model.addAttribute("username", user.getUsername());

        return "dashboard"; // Resolves to templates/dashboard.html
    }

    /** Convert MIME types to simple labels. */
    private String simplifyFileType(String mimeType) {
        if (mimeType.startsWith("image/")) return "Images";
        if (mimeType.equals("application/pdf")) return "PDF";
        if (mimeType.startsWith("video/")) return "Videos";
        if (mimeType.startsWith("audio/")) return "Audio";
        if (mimeType.contains("zip") || mimeType.contains("rar")) return "Archives";
        if (mimeType.contains("word") || mimeType.contains("document")) return "Documents";
        if (mimeType.contains("sheet") || mimeType.contains("excel")) return "Spreadsheets";
        if (mimeType.startsWith("text/")) return "Text";
        return "Other";
    }


    // ── New Enterprise Infrastructure Pages ──────────────────────────────

    /** GET /nodes — Interactive storage node topology visualization. */
    @GetMapping("/nodes")
    public String showNodes(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("user", user);
        model.addAttribute("activePage", "nodes");
        
        List<com.cloudnest.entity.FileEntity> allFiles = fileRepository.findAll();
        
        long node1Bytes = 0, node2Bytes = 0, node3Bytes = 0;
        long node1Count = 0, node2Count = 0, node3Count = 0;
        
        for (com.cloudnest.entity.FileEntity f : allFiles) {
            if (f.isDeleted()) continue;
            long size = f.getFileSize() != null ? f.getFileSize() : 0;
            if ("node1".equals(f.getStorageNode())) { node1Bytes += size; node1Count++; }
            else if ("node2".equals(f.getStorageNode())) { node2Bytes += size; node2Count++; }
            else if ("node3".equals(f.getStorageNode())) { node3Bytes += size; node3Count++; }
        }
        
        // Calculate per-node capacity based on total quota
        long nodeCapacity = quotaBytes / 3;
        if (nodeCapacity == 0) nodeCapacity = 1073741824L / 3; // Fallback to ~333MB if quota 0
        
        String capFormatted = FormatUtils.formatBytes(nodeCapacity);
        
        model.addAttribute("node1Used", FormatUtils.formatBytes(node1Bytes));
        model.addAttribute("node2Used", FormatUtils.formatBytes(node2Bytes));
        model.addAttribute("node3Used", FormatUtils.formatBytes(node3Bytes));
        
        model.addAttribute("node1StorageFormatted", FormatUtils.formatBytes(node1Bytes) + " / " + capFormatted);
        model.addAttribute("node2StorageFormatted", FormatUtils.formatBytes(node2Bytes) + " / " + capFormatted);
        model.addAttribute("node3StorageFormatted", FormatUtils.formatBytes(node3Bytes) + " / " + capFormatted);
        
        model.addAttribute("node1Pct", Math.min(100, (int)((node1Bytes * 100.0) / nodeCapacity)) + "%");
        model.addAttribute("node2Pct", Math.min(100, (int)((node2Bytes * 100.0) / nodeCapacity)) + "%");
        model.addAttribute("node3Pct", Math.min(100, (int)((node3Bytes * 100.0) / nodeCapacity)) + "%");
        
        model.addAttribute("node1Count", node1Count + " files");
        model.addAttribute("node2Count", node2Count + " files");
        model.addAttribute("node3Count", node3Count + " files");
        
        return "nodes";
    }

    /** GET /deduplication — SHA-256 deduplication center. */
    @GetMapping("/deduplication")
    public String showDeduplication(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("user", user);
        model.addAttribute("activePage", "deduplication");
        return "deduplication";
    }

    /** GET /replication — Animated cross-node replication view. */
    @GetMapping("/replication")
    public String showReplication(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("user", user);
        model.addAttribute("activePage", "replication");
        return "replication";
    }

    /** GET /network — Real-time network activity dashboard. */
    @GetMapping("/network")
    public String showNetwork(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("user", user);
        model.addAttribute("activePage", "network");
        return "network";
    }

    /** GET /analytics — Enterprise storage analytics. */
    @GetMapping("/analytics")
    public String showAnalytics(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("user", user);
        model.addAttribute("activePage", "analytics");
        return "analytics";
    }

    /** GET /monitoring — System health and monitoring center. */
    @GetMapping("/monitoring")
    public String showMonitoring(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("user", user);
        model.addAttribute("activePage", "monitoring");
        
        // Real JVM Memory Health
        Runtime runtime = Runtime.getRuntime();
        long maxMemory = runtime.maxMemory();
        long allocatedMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = allocatedMemory - freeMemory;
        
        int memoryHealth = 100 - (int)((usedMemory * 100.0) / maxMemory);
        if (memoryHealth < 0) memoryHealth = 0;
        
        model.addAttribute("memoryHealth", memoryHealth);
        model.addAttribute("memoryStrokeOffset", (int) (252 - (252 * memoryHealth / 100.0)));
        
        // Deduplication Stats
        List<com.cloudnest.entity.FileEntity> allFiles = fileRepository.findAll();
        long totalFiles = allFiles.size();
        long uniqueHashes = allFiles.stream()
            .map(f -> f.getFileHash() != null ? f.getFileHash() : "")
            .filter(h -> !h.isEmpty())
            .distinct()
            .count();
        
        long dedupSaved = totalFiles > 0 ? (totalFiles - uniqueHashes) : 0;
        if (dedupSaved < 0) dedupSaved = 0;
        
        model.addAttribute("totalFiles", totalFiles);
        model.addAttribute("dedupSaved", dedupSaved);
        
        return "monitoring";
    }
}
