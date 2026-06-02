package com.cloudnest.repository;

import com.cloudnest.entity.FileEntity;
import com.cloudnest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * FileRepository — Data access interface for FileEntity.
 *
 * WHY THIS EXISTS:
 * - Provides all database operations for files
 * - Custom queries for search, statistics, and filtering
 *
 * CUSTOM QUERIES:
 * - @Query allows writing JPQL (Java Persistence Query Language)
 * - JPQL uses entity class names (FileEntity) instead of table names (files)
 */
@Repository
public interface FileRepository extends JpaRepository<FileEntity, Long> {

    // Find all files uploaded by a specific user that are not deleted
    List<FileEntity> findByUserAndIsDeletedFalseOrderByUploadedAtDesc(User user);

    // Find files in a specific folder for a user
    List<FileEntity> findByUserAndFolderIdAndIsDeletedFalseOrderByUploadedAtDesc(User user, Long folderId);

    // Find files at root level (no folder) for a user
    List<FileEntity> findByUserAndFolderIsNullAndIsDeletedFalseOrderByUploadedAtDesc(User user);

    // Search files by name (case-insensitive partial match)
    @Query("SELECT f FROM FileEntity f WHERE f.user = :user AND f.isDeleted = false AND " +
           "(LOWER(f.originalName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(f.fileType) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<FileEntity> searchByName(@Param("user") User user, @Param("keyword") String keyword);

    // Search files by type
    @Query("SELECT f FROM FileEntity f WHERE f.user = :user AND f.isDeleted = false AND " +
           "LOWER(f.fileType) LIKE LOWER(CONCAT('%', :type, '%'))")
    List<FileEntity> searchByType(@Param("user") User user, @Param("type") String type);

    // Count total files for a user
    long countByUserAndIsDeletedFalse(User user);

    // Sum of all file sizes for a user (for storage usage)
    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileEntity f WHERE f.user = :user AND f.isDeleted = false")
    long sumFileSizeByUser(@Param("user") User user);

    // Get the 5 most recent uploads for a user
    List<FileEntity> findTop5ByUserAndIsDeletedFalseOrderByUploadedAtDesc(User user);

    // Count files per storage node for a user
    @Query("SELECT f.storageNode, COUNT(f) FROM FileEntity f WHERE f.user = :user AND f.isDeleted = false GROUP BY f.storageNode")
    List<Object[]> countByStorageNode(@Param("user") User user);

    // Count files per file type category for a user
    @Query("SELECT f.fileType, COUNT(f) FROM FileEntity f WHERE f.user = :user AND f.isDeleted = false GROUP BY f.fileType")
    List<Object[]> countByFileType(@Param("user") User user);

    // Find a file by its hash (used for data deduplication)
    FileEntity findFirstByFileHash(String fileHash);

    // Count how many files share the same physical storage name
    long countByStoredName(String storedName);

    // Find all deleted files for the Trash page
    List<FileEntity> findByUserAndIsDeletedTrueOrderByUploadedAtDesc(User user);

    // For Trash auto-purge scheduler — uses deletedAt (when file was actually trashed)
    List<FileEntity> findByIsDeletedTrueAndDeletedAtBefore(java.time.LocalDateTime cutoff);

    // Admin / Global statistics queries
    @Query("SELECT COUNT(f) FROM FileEntity f WHERE f.isDeleted = false")
    long countActiveFiles();

    @Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileEntity f WHERE f.isDeleted = false")
    long sumTotalFileSize();

    @Query("SELECT f.storageNode, COUNT(f), COALESCE(SUM(f.fileSize), 0) FROM FileEntity f WHERE f.isDeleted = false GROUP BY f.storageNode")
    List<Object[]> getNodeStats();

    @Query("SELECT COUNT(DISTINCT f.fileHash) FROM FileEntity f WHERE f.isDeleted = false")
    long countUniqueHashes();

    // Legacy: kept for backward compatibility (queries by upload date, not deletion date)
    @Deprecated
    List<FileEntity> findByIsDeletedTrueAndUploadedAtBefore(java.time.LocalDateTime cutoff);
}
