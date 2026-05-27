package com.cloudnest.service;

import com.cloudnest.config.AppConfig;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;

/**
 * StorageNodeService — Simulates distributed file storage across multiple nodes.
 *
 * WHY THIS EXISTS:
 * - In real distributed systems (like Google Drive), files are stored across
 *   multiple servers (nodes) for redundancy and performance
 * - This service simulates that by randomly assigning files to node1, node2, or node3
 * - During the presentation, this demonstrates your understanding of distributed storage
 *
 * HOW IT WORKS:
 * - When a file is uploaded, this service picks a random node
 * - The file is physically stored in: storage/node{X}/filename
 * - The node assignment is saved in the database for retrieval
 */
@Service
public class StorageNodeService {

    private final AppConfig appConfig;

    // Constructor injection — Spring automatically passes the AppConfig bean
    public StorageNodeService(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    /**
     * Randomly select a storage node for a new file upload.
     *
     * In a real system, node selection would consider:
     * - Available disk space on each node
     * - Network latency
     * - Load balancing
     * - Replication factor
     *
     * For our simulation, we use random selection.
     *
     * @return Node name like "node1", "node2", or "node3"
     */
    public String selectNode() {
        int nodeNumber = ThreadLocalRandom.current().nextInt(1, appConfig.getNodeCount() + 1);
        return "node" + nodeNumber;
    }

    /**
     * Get the full file system path for a file on a specific node.
     *
     * @param node       The storage node (e.g., "node1")
     * @param storedName The UUID-based filename
     * @return Full path like "storage/node1/abc-123-def.pdf"
     */
    public String getFilePath(String node, String storedName) {
        return appConfig.getStoragePath() + "/" + node + "/" + storedName;
    }
}
