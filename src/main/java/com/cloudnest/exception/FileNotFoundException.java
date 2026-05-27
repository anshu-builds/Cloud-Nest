package com.cloudnest.exception;

/**
 * FileNotFoundException — Custom exception for when a requested file doesn't exist.
 *
 * WHY CUSTOM EXCEPTIONS?
 * - More descriptive error messages specific to our domain
 * - The GlobalExceptionHandler can catch them and return proper HTTP responses
 * - Better than throwing generic RuntimeException everywhere
 */
public class FileNotFoundException extends RuntimeException {

    public FileNotFoundException(String message) {
        super(message);
    }

    public FileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
