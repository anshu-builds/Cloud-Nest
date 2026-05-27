package com.cloudnest.repository;

import com.cloudnest.entity.SharedLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * SharedLinkRepository — Data access interface for SharedLink entities.
 *
 * WHY THIS EXISTS:
 * - Provides operations for creating and resolving share links
 * - Token-based lookup for shared file access
 */
@Repository
public interface SharedLinkRepository extends JpaRepository<SharedLink, Long> {

    // Find a share link by its unique token
    Optional<SharedLink> findByToken(String token);

    // Find all share links created by a user
    List<SharedLink> findByCreatedByIdOrderByCreatedAtDesc(Long userId);

    // Find share links for a specific file
    List<SharedLink> findByFileId(Long fileId);

    // Delete all share links for a specific file (when file is deleted)
    @org.springframework.data.jpa.repository.Modifying
    void deleteByFileId(Long fileId);
}
