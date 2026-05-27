package com.cloudnest.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

/**
 * DashboardDto — Data Transfer Object for the dashboard page.
 *
 * WHY THIS EXISTS:
 * - Aggregates statistics for the dashboard view
 * - Shows total files, storage usage, recent uploads, and node distribution
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardDto {

    private long totalFiles;
    private long totalFolders;
    private long totalStorageBytes;
    private String formattedStorage;
    private long quotaBytes;
    private String formattedQuota;
    private int quotaPercentage;
    private List<FileDto> recentFiles;
    private Map<String, Long> nodeDistribution; // e.g., {"node1": 5, "node2": 3, "node3": 4}
    private Map<String, Long> fileTypeDistribution; // e.g., {"PDF": 3, "Image": 5}
}
