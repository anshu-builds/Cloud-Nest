package com.cloudnest.repository;

import com.cloudnest.entity.Folder;
import com.cloudnest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * FolderRepository — Data access interface for Folder entities.
 *
 * WHY THIS EXISTS:
 * - Provides CRUD operations for folders
 * - Supports querying folders by user and parent for navigation
 */
@Repository
public interface FolderRepository extends JpaRepository<Folder, Long> {

    // Find all root-level folders for a user (folders with no parent)
    List<Folder> findByUserAndParentIsNullAndIsDeletedFalseOrderByNameAsc(User user);

    // Find ALL folders for a user (for the move dropdown)
    List<Folder> findByUserAndIsDeletedFalseOrderByNameAsc(User user);

    // Find all sub-folders inside a specific parent folder
    List<Folder> findByUserAndParentIdAndIsDeletedFalseOrderByNameAsc(User user, Long parentId);

    // Find a specific folder owned by a specific user (security check)
    Optional<Folder> findByIdAndUserAndIsDeletedFalse(Long id, User user);

    // For permanent operations or restores, we need to find it even if deleted
    Optional<Folder> findByIdAndUser(Long id, User user);

    // Count folders for a user
    long countByUserAndIsDeletedFalse(User user);

    // Check if a folder with the same name exists at the same level
    boolean existsByUserAndNameAndParentIdAndIsDeletedFalse(User user, String name, Long parentId);

    boolean existsByUserAndNameAndParentIsNullAndIsDeletedFalse(User user, String name);

    // Find all deleted folders for the Trash page
    List<Folder> findByUserAndIsDeletedTrueOrderByNameAsc(User user);

    // BUG-05: Find child folders for cascade soft-delete
    List<Folder> findByParentAndUserAndIsDeletedFalse(Folder parent, User user);

    // BUG-05: Find child folders for cascade restore (includes deleted ones)
    List<Folder> findByParentAndUser(Folder parent, User user);
}
