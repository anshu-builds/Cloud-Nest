package com.cloudnest.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * FileEntity — Represents an uploaded file in the system.
 *
 * WHY THIS CLASS EXISTS:
 * - Maps to the "files" table in PostgreSQL
 * - Tracks metadata about each uploaded file: name, size, type, storage location
 * - Links each file to its owner (User) and optionally to a Folder
 *
 * NOTE: We name it "FileEntity" (not "File") to avoid conflict with java.io.File
 *
 * STORAGE NODE SIMULATION:
 * - The "storageNode" field records which simulated node stores the actual file
 * - This demonstrates distributed storage concepts in the presentation
 */
@Entity
@Table(name = "files")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @jakarta.persistence.Version
    private Long version;

    // The original filename the user uploaded (e.g., "report.pdf")
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    // The UUID-based name used for physical storage (prevents filename collisions)
    @Column(name = "stored_name", nullable = false, length = 255)
    private String storedName;

    // MIME type of the file (e.g., "application/pdf", "image/png")
    @Column(name = "file_type", length = 100)
    private String fileType;

    // Size in bytes
    @Column(name = "file_size")
    private Long fileSize;

    // Which simulated storage node holds this file (e.g., "node1", "node2", "node3")
    @Column(name = "storage_node", length = 20)
    private String storageNode;

    // Hash for Data Deduplication
    @Column(name = "file_hash", length = 64)
    private String fileHash;

    // Many files belong to one user
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // A file can optionally belong to a folder (null = root level)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "folder_id")
    private Folder folder;

    // Soft delete flag
    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean isDeleted = false;

    // Timestamp when the file was moved to trash (used by auto-purge scheduler)
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @CreationTimestamp
    @Column(name = "uploaded_at", updatable = false)
    private LocalDateTime uploadedAt;
}
