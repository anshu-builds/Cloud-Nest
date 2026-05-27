package com.cloudnest.util;

/**
 * Utility class for formatting values across the application.
 *
 * WHY THIS EXISTS:
 * - Eliminates duplication of formatBytes() across 3+ controllers
 *   (AdminController, DashboardController, ShareController)
 * - Single source of truth for byte formatting logic
 */
public final class FormatUtils {

    private FormatUtils() {} // Utility class — cannot be instantiated

    /**
     * Format byte count to human-readable string.
     *
     * Examples:
     *   512       → "512 B"
     *   1024      → "1.0 KB"
     *   1048576   → "1.0 MB"
     *   1073741824 → "1.00 GB"
     *
     * @param bytes the number of bytes to format
     * @return a human-readable string representation
     */
    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
