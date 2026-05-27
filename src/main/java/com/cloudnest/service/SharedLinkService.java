package com.cloudnest.service;

import com.cloudnest.entity.FileEntity;
import com.cloudnest.entity.SharedLink;
import com.cloudnest.entity.User;
import com.cloudnest.exception.FileNotFoundException;
import com.cloudnest.repository.SharedLinkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * SharedLinkService — Generates and resolves shareable file links.
 *
 * HOW SHARING WORKS:
 * 1. User clicks "Share" on a file
 * 2. This service generates a unique UUID token
 * 3. A SharedLink record is saved in the database
 * 4. The URL /share/{token} is returned to the user
 * 5. Anyone with that URL can download the file (no login required)
 * 6. Links expire after 7 days by default
 */
@Service
@Transactional
public class SharedLinkService {

    private final SharedLinkRepository sharedLinkRepository;

    public SharedLinkService(SharedLinkRepository sharedLinkRepository) {
        this.sharedLinkRepository = sharedLinkRepository;
    }

    /**
     * Generate a shareable link for a file.
     *
     * @param file The file to share
     * @param user The user creating the link
     * @return The generated token (used in the share URL)
     */
    public String generateShareLink(FileEntity file, User user) {
        // Generate a unique token
        String token = UUID.randomUUID().toString();

        // Create the share link with 7-day expiration
        SharedLink sharedLink = SharedLink.builder()
                .token(token)
                .file(file)
                .createdBy(user)
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();

        sharedLinkRepository.save(sharedLink);
        return token;
    }

    /**
     * Resolve a share token to the actual file.
     *
     * @param token The share token from the URL
     * @return The SharedLink containing the file reference
     */
    public SharedLink resolveShareLink(String token) {
        SharedLink link = sharedLinkRepository.findByToken(token)
                .orElseThrow(() -> new FileNotFoundException("Share link not found or invalid"));

        // Check if the link has expired
        if (link.isExpired()) {
            throw new FileNotFoundException("This share link has expired");
        }

        return link;
    }

    /**
     * Get all share links created by a user.
     */
    public List<SharedLink> getUserShareLinks(Long userId) {
        return sharedLinkRepository.findByCreatedByIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Delete all share links for a file (called when file is deleted).
     */
    public void deleteLinksForFile(Long fileId) {
        sharedLinkRepository.deleteByFileId(fileId);
    }
}
