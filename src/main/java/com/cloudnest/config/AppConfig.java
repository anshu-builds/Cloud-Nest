package com.cloudnest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * AppConfig — General application configuration.
 *
 * This class reads custom properties from application.properties
 * and performs initialization tasks like creating storage directories.
 *
 * WHY THIS CLASS EXISTS:
 * - Centralizes configuration values so they can be injected anywhere
 * - Ensures the simulated distributed storage folders (node1, node2, node3)
 *   are created when the application starts
 */
@Configuration
public class AppConfig {

    // Injected from application.properties: cloudnest.storage.base-path
    @Value("${cloudnest.storage.base-path}")
    private String storagePath;

    // Injected from application.properties: cloudnest.storage.node-count
    @Value("${cloudnest.storage.node-count}")
    private int nodeCount;

    /**
     * @PostConstruct runs ONCE after the bean is created.
     * Here, we create the simulated distributed storage directories:
     *   storage/node1/
     *   storage/node2/
     *   storage/node3/
     */
    @PostConstruct
    public void initStorageDirectories() throws IOException {
        for (int i = 1; i <= nodeCount; i++) {
            Path nodePath = Paths.get(storagePath, "node" + i);
            if (!Files.exists(nodePath)) {
                Files.createDirectories(nodePath);
            }
        }
    }

    public String getStoragePath() {
        return storagePath;
    }

    public int getNodeCount() {
        return nodeCount;
    }
}
