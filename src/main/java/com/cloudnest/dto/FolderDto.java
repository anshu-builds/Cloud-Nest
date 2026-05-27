package com.cloudnest.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * FolderDto — Data Transfer Object for folder display.
 *
 * WHY THIS EXISTS:
 * - Carries folder info to the frontend without exposing the entity
 * - Includes counts for quick summary display
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FolderDto {

    private Long id;
    private String name;
    private Long parentId;
    private String parentName;
    private int fileCount;
    private int subFolderCount;
    private LocalDateTime createdAt;
    private List<FileDto> files;
    private List<FolderDto> subFolders;
}
