package com.cloudnest.controller;

import com.cloudnest.dto.FileDto;
import com.cloudnest.entity.FileEntity;
import com.cloudnest.entity.User;
import com.cloudnest.service.FileStorageService;
import com.cloudnest.service.FolderService;
import com.cloudnest.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class FileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FileStorageService fileStorageService;

    @MockBean
    private FolderService folderService;

    @MockBean
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("john_doe")
                .email("john@gmail.com")
                .build();
        when(userService.findByUsername("john_doe")).thenReturn(testUser);
    }

    @Test
    @WithMockUser(username = "john_doe")
    void testListFiles_Root() throws Exception {
        FileDto fileDto = FileDto.builder()
                .id(1L)
                .originalName("document.pdf")
                .fileType("application/pdf")
                .fileSize(100L)
                .build();
        when(fileStorageService.getRootFiles(any(User.class))).thenReturn(List.of(fileDto));
        when(folderService.getRootFolders(any(User.class))).thenReturn(Collections.emptyList());
        when(folderService.getAllFolders(any(User.class))).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/files"))
                .andExpect(status().isOk())
                .andExpect(view().name("files"))
                .andExpect(model().attributeExists("files", "folders", "allFolders"))
                .andExpect(model().attribute("currentFolderId", (Object) null));
    }

    @Test
    @WithMockUser(username = "john_doe")
    void testUploadFiles_Success() throws Exception {
        MockMultipartFile mockFile = new MockMultipartFile(
                "files",
                "photo.jpg",
                "image/jpeg",
                "image-content".getBytes()
        );

        mockMvc.perform(multipart("/files/upload")
                        .file(mockFile)
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files"))
                .andExpect(flash().attribute("success", "1 file(s) uploaded successfully!"));

        verify(fileStorageService, times(1)).uploadFile(any(), any(), any());
    }

    @Test
    @WithMockUser(username = "john_doe")
    void testMoveFile_Success() throws Exception {
        mockMvc.perform(post("/files/move/1")
                        .param("targetFolderId", "2")
                        .param("currentFolderId", "3")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/files?folderId=3"))
                .andExpect(flash().attribute("success", "File moved successfully!"));

        verify(fileStorageService, times(1)).moveFile(eq(1L), eq(2L), any(User.class));
    }

    @Test
    @WithMockUser(username = "john_doe")
    void testSearchFiles() throws Exception {
        FileDto fileDto = FileDto.builder()
                .id(1L)
                .originalName("document.pdf")
                .build();
        when(fileStorageService.searchFiles(any(User.class), eq("document"))).thenReturn(List.of(fileDto));

        mockMvc.perform(get("/files/search").param("query", "document"))
                .andExpect(status().isOk())
                .andExpect(view().name("files"))
                .andExpect(model().attribute("searchQuery", "document"))
                .andExpect(model().attribute("searchMode", true))
                .andExpect(model().attributeExists("files", "allFolders"));
    }
}
