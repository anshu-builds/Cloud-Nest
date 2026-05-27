package com.cloudnest.service;

import com.cloudnest.entity.FileEntity;
import com.cloudnest.repository.FileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled task that permanently deletes files that have been
 * in the trash for more than 30 days.
 */
@Component
public class TrashCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(TrashCleanupScheduler.class);
    private static final int RETENTION_DAYS = 30;

    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;

    public TrashCleanupScheduler(FileRepository fileRepository,
                                  FileStorageService fileStorageService) {
        this.fileRepository = fileRepository;
        this.fileStorageService = fileStorageService;
    }

    @Scheduled(cron = "0 0 2 * * *") // Run daily at 2:00 AM
    @Transactional
    public void purgeExpiredTrashItems() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);
        List<FileEntity> expired = fileRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff);

        if (expired.isEmpty()) {
            log.info("Trash cleanup: no expired items found.");
            return;
        }

        log.info("Trash cleanup: purging {} items older than {} days.", expired.size(), RETENTION_DAYS);
        for (FileEntity file : expired) {
            try {
                fileStorageService.permanentDeleteFileAdmin(file.getId());
            } catch (Exception e) {
                log.error("Failed to auto-purge file id={}: {}", file.getId(), e.getMessage());
            }
        }
    }
}
