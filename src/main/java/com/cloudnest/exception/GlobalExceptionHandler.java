package com.cloudnest.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * GlobalExceptionHandler — Catches exceptions across ALL controllers.
 *
 * WHY THIS EXISTS:
 * - @ControllerAdvice makes this a global handler for all controllers
 * - Instead of every controller having try-catch blocks, this centralizes error handling
 * - Users see friendly error messages instead of stack traces
 *
 * HOW IT WORKS:
 * 1. A controller throws an exception (e.g., FileNotFoundException)
 * 2. Spring checks if any @ExceptionHandler method can handle it
 * 3. This class catches it and redirects with an error message
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles FileNotFoundException — redirects to files page with error message.
     */
    @ExceptionHandler(FileNotFoundException.class)
    public Object handleFileNotFound(FileNotFoundException ex,
                                     HttpServletRequest request,
                                     RedirectAttributes redirectAttributes) {
        log.warn("File not found: {}", ex.getMessage());

        if (!expectsHtmlResponse(request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }

        redirectAttributes.addFlashAttribute("error", ex.getMessage());
        return "redirect:/files";
    }

    /**
     * Handles StorageException — redirects to files page with error message.
     */
    @ExceptionHandler(StorageException.class)
    public String handleStorageException(StorageException ex, RedirectAttributes redirectAttributes) {
        log.error("Storage exception: {}", ex.getMessage(), ex);
        redirectAttributes.addFlashAttribute("error", "Storage error: " + ex.getMessage());
        return "redirect:/files";
    }

    /**
     * Handles any other unexpected exceptions.
     */
    @ExceptionHandler(Exception.class)
    public String handleGenericException(Exception ex, RedirectAttributes redirectAttributes) {
        if (ex instanceof AccessDeniedException accessDeniedException) {
            throw accessDeniedException;
        }

        log.error("Unhandled exception caught by GlobalExceptionHandler", ex);
        redirectAttributes.addFlashAttribute("error", "An unexpected error occurred. Please try again.");
        return "redirect:/dashboard";
    }

    private boolean expectsHtmlResponse(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri != null && (uri.contains("/download/") || uri.contains("/preview/"))) {
            return false;
        }

        String accept = request.getHeader(HttpHeaders.ACCEPT);
        return accept == null || accept.contains("text/html") || accept.contains("*/*");
    }
}
