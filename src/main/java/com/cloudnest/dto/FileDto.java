package com.cloudnest.dto;

import lombok.*;

import java.time.LocalDateTime;

/**
 * FileDto — Data Transfer Object for file information displayed on the frontend.
 *
 * WHY THIS EXISTS:
 * - Carries file metadata to the Thymeleaf templates
 * - Includes formatted file size for human-readable display
 * - Avoids exposing the full FileEntity (which has lazy-loaded relationships)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileDto {

    private Long id;
    private String originalName;
    private String fileType;
    private Long fileSize;
    private String storageNode;
    private String folderName;
    private Long folderId;
    private LocalDateTime uploadedAt;
    private String shareLink;

    /**
     * Returns a human-readable file size (e.g., "2.5 MB" instead of "2621440").
     */
    public String getFormattedSize() {
        if (fileSize == null) return "Unknown";
        if (fileSize < 1024) return fileSize + " B";
        if (fileSize < 1024 * 1024) return String.format("%.1f KB", fileSize / 1024.0);
        if (fileSize < 1024 * 1024 * 1024) return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        return String.format("%.1f GB", fileSize / (1024.0 * 1024 * 1024));
    }

    /**
     * Returns a CSS class based on file type for icon display.
     */
    public String getFileIconClass() {
        if (fileType == null) return "bi-file-earmark";
        if (fileType.startsWith("image/")) return "bi-file-earmark-image";
        if (fileType.equals("application/pdf")) return "bi-file-earmark-pdf";
        if (fileType.startsWith("video/")) return "bi-file-earmark-play";
        if (fileType.startsWith("audio/")) return "bi-file-earmark-music";
        if (fileType.contains("zip") || fileType.contains("rar") || fileType.contains("tar"))
            return "bi-file-earmark-zip";
        if (fileType.contains("word") || fileType.contains("document"))
            return "bi-file-earmark-word";
        if (fileType.contains("sheet") || fileType.contains("excel"))
            return "bi-file-earmark-excel";
        if (fileType.contains("presentation") || fileType.contains("powerpoint"))
            return "bi-file-earmark-slides";
        if (fileType.startsWith("text/")) return "bi-file-earmark-text";
        return "bi-file-earmark";
    }
}
