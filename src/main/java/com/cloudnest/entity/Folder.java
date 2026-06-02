package com.cloudnest.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Folder Entity — Represents a folder in the user's file system.
 *
 * WHY THIS CLASS EXISTS:
 * - Maps to the "folders" table in PostgreSQL
 * - Allows users to organize their files into folders
 * - Supports nested folders via the self-referencing parent relationship
 *
 * SELF-REFERENCING RELATIONSHIP:
 * - A folder can have a "parent" folder (for nested folder structures)
 * - If parent is null, the folder is at the root level
 */
@Entity
@Table(name = "folders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Folder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    @Column(nullable = false, length = 100)
    private String name;

    // Many folders belong to one user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // Self-referencing: a folder can be inside another folder
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Folder parent;

    // A folder contains many files
    @OneToMany(mappedBy = "folder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FileEntity> files = new ArrayList<>();

    // A folder can contain sub-folders
    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Folder> subFolders = new ArrayList<>();

    // Soft delete flag
    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isDeleted = false;

    // Timestamp when the folder was moved to trash (used by auto-purge scheduler)
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
