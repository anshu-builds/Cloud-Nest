package com.cloudnest.exception;

/**
 * StorageException — Custom exception for file storage operation failures.
 *
 * WHY THIS EXISTS:
 * - Wraps I/O errors that occur during file upload, download, or delete
 * - Provides clear error context for debugging
 */
public class StorageException extends RuntimeException {

    public StorageException(String message) {
        super(message);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
