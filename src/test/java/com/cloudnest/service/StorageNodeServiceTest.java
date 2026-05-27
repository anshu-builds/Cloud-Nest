package com.cloudnest.service;

import com.cloudnest.config.AppConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class StorageNodeServiceTest {

    private AppConfig appConfig;
    private StorageNodeService storageNodeService;

    @BeforeEach
    void setUp() {
        appConfig = Mockito.mock(AppConfig.class);
        storageNodeService = new StorageNodeService(appConfig);
    }

    @Test
    void testSelectNode_ReturnsValidNodes() {
        when(appConfig.getNodeCount()).thenReturn(3);

        Set<String> selectedNodes = new HashSet<>();
        // Select node multiple times to verify distribution and range
        for (int i = 0; i < 100; i++) {
            String node = storageNodeService.selectNode();
            assertTrue(node.matches("node[1-3]"), "Node should be node1, node2, or node3");
            selectedNodes.add(node);
        }

        // Verify that random selection eventually hits all nodes (probability is extremely high for 100 iterations)
        assertTrue(selectedNodes.contains("node1"), "Should select node1 at least once");
        assertTrue(selectedNodes.contains("node2"), "Should select node2 at least once");
        assertTrue(selectedNodes.contains("node3"), "Should select node3 at least once");
    }

    @Test
    void testGetFilePath_FormatsCorrectPath() {
        when(appConfig.getStoragePath()).thenReturn("target/test-storage");

        String filePath = storageNodeService.getFilePath("node2", "test-uuid.txt");
        assertEquals("target/test-storage/node2/test-uuid.txt", filePath);
    }

    @Test
    void testGetFilePath_HandlesPathsWithSpaces() {
        when(appConfig.getStoragePath()).thenReturn("target/test storage folder");

        String filePath = storageNodeService.getFilePath("node1", "spaced file.txt");
        assertEquals("target/test storage folder/node1/spaced file.txt", filePath);
    }
}
