package com.cloudnest.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * SharedLink Entity — Represents a shareable link for a file.
 *
 * WHY THIS CLASS EXISTS:
 * - Maps to the "shared_links" table in PostgreSQL
 * - Enables file sharing via unique tokens (like Google Drive's "anyone with the link")
 * - Each link has an expiration date for security
 *
 * HOW IT WORKS:
 * 1. User clicks "Share" on a file
 * 2. System generates a unique UUID token
 * 3. A URL like /share/{token} is created
 * 4. Anyone with that URL can download the file (even without logging in)
 * 5. The link expires after a set time
 */
@Entity
@Table(name = "shared_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SharedLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique token used in the shareable URL (UUID format)
    @Column(nullable = false, unique = true, length = 255)
    private String token;

    // The file being shared
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false)
    private FileEntity file;

    // The user who created the share link
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    // When the link stops working (null = never expires)
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Check if this share link has expired.
     * Returns false if expiresAt is null (link never expires).
     */
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
}
