# Software Requirements Specification (SRS)

## Project: CloudNest — Enterprise Distributed Cloud Storage System

### Part 2 of 4: Functional Requirements

---

## 3. Functional Requirements

> All functional requirements are identified with a unique ID in the format `FR-X.Y` and are traced to the specific source file(s) implementing them.

---

### 3.1 Authentication & User Management Module

**Source Files:**
- Controller: `AuthController.java`
- Service: `UserService.java`
- Security: `SecurityConfig.java`
- DTO: `UserRegistrationDto.java`
- Entity: `User.java`
- Repository: `UserRepository.java`

#### FR-1.1 — User Registration

| Attribute     | Detail                                                                                                                 |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **ID**        | FR-1.1                                                                                                                 |
| **Priority**  | High                                                                                                                   |
| **Endpoint**  | `GET /register` (show form) · `POST /register` (submit form)                                                          |
| **Input**     | `username` (3–50 chars), `email` (valid format), `password` (6–100 chars), `confirmPassword`                           |
| **Validation**| `@NotBlank`, `@Email`, `@Size` via Bean Validation (JSR 380) on `UserRegistrationDto`                                  |
| **Processing**| 1. Validate passwords match (`password == confirmPassword`) <br> 2. Check username uniqueness (`existsByUsername()`) <br> 3. Check email uniqueness (`existsByEmail()`) <br> 4. Hash password with BCrypt (strength 12) <br> 5. Force role to `ROLE_USER` (never `ROLE_ADMIN` from registration) <br> 6. Save user entity to database |
| **Output**    | Redirect to `/login?success` with flash message "Registration successful! Please log in."                              |
| **Error**     | If validation fails → re-render form with field-level errors. If business rule fails → re-render with error message.   |

#### FR-1.2 — Password Security

| Attribute     | Detail                                                                                           |
| ------------- | ------------------------------------------------------------------------------------------------ |
| **ID**        | FR-1.2                                                                                           |
| **Priority**  | Critical                                                                                         |
| **Processing**| Passwords are **never stored in plain text**. `BCryptPasswordEncoder` with strength factor **12** is used (`new BCryptPasswordEncoder(12)`). BCrypt automatically generates a random salt per password, preventing rainbow table attacks. |
| **Source**    | `SecurityConfig.passwordEncoder()` · `UserService.registerUser()` → `passwordEncoder.encode(dto.getPassword())` |

#### FR-1.3 — User Login (Form-Based Authentication)

| Attribute       | Detail                                                                                               |
| --------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**          | FR-1.3                                                                                               |
| **Priority**    | High                                                                                                 |
| **Endpoint**    | `GET /login` (show form) · `POST /login` (processed by Spring Security filter chain)                 |
| **Input**       | `username` field (accepts either username or email, case-insensitive), `password` field               |
| **Processing**  | 1. Spring Security calls `UserService.loadUserByUsername(loginId)` <br> 2. `UserRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(loginId, loginId)` resolves the user <br> 3. Spring Security compares entered password hash against stored BCrypt hash <br> 4. On success: HTTP session is created, `JSESSIONID` cookie is set, redirect to `/dashboard` <br> 5. On failure: redirect to `/login?error=true` |
| **Session**     | `request.getSession(true)` — early session creation for CSRF token availability                      |
| **Source**      | `SecurityConfig.filterChain()` · `AuthController.showLoginPage()` · `UserService.loadUserByUsername()`|

#### FR-1.4 — User Logout

| Attribute       | Detail                                                                              |
| --------------- | ------------------------------------------------------------------------------------ |
| **ID**          | FR-1.4                                                                               |
| **Priority**    | High                                                                                 |
| **Endpoint**    | `POST /logout` (handled by Spring Security)                                          |
| **Processing**  | 1. Invalidate HTTP session <br> 2. Delete `JSESSIONID` cookie <br> 3. Redirect to `/login?logout=true` |
| **Source**      | `SecurityConfig.filterChain()` → `.logout(...)` configuration                        |

#### FR-1.5 — Role-Based Access Control

| Attribute     | Detail                                                                                                                 |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **ID**        | FR-1.5                                                                                                                 |
| **Priority**  | Critical                                                                                                               |
| **Roles**     | `ROLE_USER` (default) · `ROLE_ADMIN` (elevated)                                                                       |
| **Rules**     | • Public pages (no auth): `/login`, `/register`, `/share/**`, `/css/**`, `/js/**`, `/images/**`, `/error`, `/actuator/health`, `/actuator/info` <br> • Admin-only pages: `/admin/**` (requires `ROLE_ADMIN`) <br> • All other pages: require authentication (any role) |
| **Source**    | `SecurityConfig.filterChain()` → `.authorizeHttpRequests(...)` configuration                                           |

#### FR-1.6 — Root URL Redirect

| Attribute     | Detail                                                                  |
| ------------- | ----------------------------------------------------------------------- |
| **ID**        | FR-1.6                                                                  |
| **Endpoint**  | `GET /`                                                                  |
| **Processing**| Immediately redirects to `/dashboard`. If user is not authenticated, Spring Security intercepts and redirects to `/login`. |
| **Source**    | `AuthController.redirectToDashboard()`                                   |

---

### 3.2 File Management Module

**Source Files:**
- Controller: `FileController.java`
- Service: `FileStorageService.java`
- Entity: `FileEntity.java`
- Repository: `FileRepository.java`
- DTO: `FileDto.java`

#### FR-2.1 — Multi-File Upload with Drag-and-Drop

| Attribute     | Detail                                                                                                                 |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **ID**        | FR-2.1                                                                                                                 |
| **Priority**  | High                                                                                                                   |
| **Endpoint**  | `POST /files/upload`                                                                                                   |
| **Input**     | `files` — `List<MultipartFile>` (one or more files), `folderId` — optional target folder ID                            |
| **Processing**| For each file in the batch: <br> 1. Reject if file is empty <br> 2. Check storage quota (`sumFileSizeByUser()` + new file ≤ 1 GB) <br> 3. Extract file extension; reject if in blocked list (`.exe`, `.bat`, `.sh`, `.ps1`, `.cmd`) <br> 4. Read entire file bytes into memory <br> 5. Compute SHA-256 hash of bytes <br> 6. **Deduplication check**: query `findFirstByFileHash(hash)` <br> &nbsp;&nbsp; — If match found: reuse existing `storedName` and `storageNode` (no disk write) <br> &nbsp;&nbsp; — If no match: generate UUID filename, select random node via `StorageNodeService.selectNode()`, write bytes to `storage/nodeX/<uuid>.<ext>` <br> 7. Resolve target folder (if `folderId` provided) <br> 8. Build `FileEntity` with all metadata and save to database |
| **Output**    | Redirect to `/files` (or `/files?folderId=X`) with success/error flash messages                                        |
| **Size Limit**| 50 MB per file (configured: `spring.servlet.multipart.max-file-size=50MB`)                                             |
| **Source**    | `FileController.uploadFiles()` · `FileStorageService.uploadFile()`                                                     |

#### FR-2.2 — File Listing and Navigation

| Attribute     | Detail                                                                                                                 |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **ID**        | FR-2.2                                                                                                                 |
| **Priority**  | High                                                                                                                   |
| **Endpoint**  | `GET /files` · `GET /files?folderId={id}`                                                                              |
| **Processing**| • Root level: queries `findByUserAndFolderIsNullAndIsDeletedFalseOrderByUploadedAtDesc()` for files and `findByUserAndParentIsNullAndIsDeletedFalseOrderByNameAsc()` for folders <br> • Inside folder: queries `findByUserAndFolderIdAndIsDeletedFalseOrderByUploadedAtDesc()` for files and `findByUserAndParentIdAndIsDeletedFalseOrderByNameAsc()` for sub-folders <br> • Breadcrumb navigation is generated by walking up the `parent` chain from current folder to root |
| **Model Data**| `files` (List\<FileDto\>), `folders` (List\<FolderDto\>), `allFolders` (for move dropdown), `breadcrumbs`, `currentFolder`, `currentFolderId` |
| **Source**    | `FileController.listFiles()` · `FileStorageService.getUserFiles()` · `FileStorageService.getRootFiles()` · `FolderService.getBreadcrumbs()` |

#### FR-2.3 — File Download

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-2.3                                                                                               |
| **Priority**  | High                                                                                                 |
| **Endpoint**  | `GET /files/download/{id}`                                                                           |
| **Processing**| 1. Fetch `FileEntity` by ID <br> 2. Verify file is not soft-deleted <br> 3. Verify ownership (user ID match) <br> 4. Resolve physical path via `StorageNodeService.getFilePath(node, storedName)` <br> 5. Verify physical file exists on disk <br> 6. Return as `ResponseEntity<Resource>` with `Content-Disposition: attachment` header |
| **MIME Type** | `application/octet-stream` (forces browser download dialog)                                          |
| **Source**    | `FileController.downloadFile()` · `FileStorageService.getFilePath()` · `FileStorageService.getFileEntity()` |

#### FR-2.4 — In-Browser File Preview

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-2.4                                                                                               |
| **Priority**  | Medium                                                                                               |
| **Endpoint**  | `GET /files/preview/{id}`                                                                            |
| **Processing**| Same ownership and existence checks as download, but returns with `Content-Disposition: inline` and the file's actual MIME type so the browser renders it natively |
| **Supported** | Images (`image/*`), PDFs (`application/pdf`), plain text (`text/*`)                                  |
| **Source**    | `FileController.previewFile()`                                                                       |

#### FR-2.5 — File Search

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-2.5                                                                                               |
| **Priority**  | Medium                                                                                               |
| **Endpoint**  | `GET /files/search?query={keyword}`                                                                  |
| **Processing**| 1. Search by filename (case-insensitive partial match) using JPQL `LIKE %keyword%` on `originalName` <br> 2. If no results by name, fallback to search by file type (`fileType` LIKE `%keyword%`) <br> 3. Only non-deleted files belonging to the authenticated user are returned |
| **JPQL**      | `SELECT f FROM FileEntity f WHERE f.user = :user AND f.isDeleted = false AND (LOWER(f.originalName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(f.fileType) LIKE LOWER(CONCAT('%', :keyword, '%')))` |
| **Source**    | `FileController.searchFiles()` · `FileStorageService.searchFiles()` · `FileRepository.searchByName()` / `searchByType()` |

#### FR-2.6 — File Soft Delete (Move to Trash)

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-2.6                                                                                               |
| **Priority**  | High                                                                                                 |
| **Endpoint**  | `POST /files/delete/{id}`                                                                            |
| **Processing**| 1. Verify ownership <br> 2. Set `isDeleted = true` and `deletedAt = LocalDateTime.now()` <br> 3. Save (file remains on disk; database record is updated) |
| **Source**    | `FileController.deleteFile()` · `FileStorageService.deleteFile()`                                    |

#### FR-2.7 — File Move

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-2.7                                                                                               |
| **Priority**  | Medium                                                                                               |
| **Endpoint**  | `POST /files/move/{id}`                                                                              |
| **Input**     | `targetFolderId` (null = move to root), `currentFolderId`                                            |
| **Processing**| 1. Verify file ownership <br> 2. Verify target folder ownership and existence <br> 3. Update `folder` foreign key on `FileEntity` <br> 4. Save |
| **Source**    | `FileController.moveFile()` · `FileStorageService.moveFile()`                                        |

---

### 3.3 Folder Management Module

**Source Files:**
- Controller: `FolderController.java`
- Service: `FolderService.java`
- Entity: `Folder.java`
- Repository: `FolderRepository.java`
- DTO: `FolderDto.java`

#### FR-3.1 — Folder Creation (Nested Hierarchies)

| Attribute     | Detail                                                                                                                 |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **ID**        | FR-3.1                                                                                                                 |
| **Priority**  | High                                                                                                                   |
| **Endpoint**  | `POST /folders/create`                                                                                                 |
| **Input**     | `name` (folder name), `parentId` (null = root level)                                                                   |
| **Validation**| • Name cannot be empty or blank <br> • Name cannot contain `..`, `/`, `\`, null characters (path traversal prevention) <br> • Name length ≤ 255 characters <br> • Duplicate name check at the same hierarchy level (same parent) |
| **Processing**| 1. Validate name <br> 2. Check for duplicate name at same parent level <br> 3. If `parentId` is provided, verify parent folder exists and is owned by user <br> 4. Build `Folder` entity with `parent` reference <br> 5. Save and add to parent's `subFolders` collection |
| **Source**    | `FolderController.createFolder()` · `FolderService.createFolder()`                                                     |

#### FR-3.2 — Folder Soft Delete (Recursive Cascade)

| Attribute     | Detail                                                                                                                 |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **ID**        | FR-3.2                                                                                                                 |
| **Priority**  | High                                                                                                                   |
| **Endpoint**  | `POST /folders/delete/{id}`                                                                                            |
| **Processing**| **Recursive cascade** soft-delete: <br> 1. Mark the folder as `isDeleted = true`, set `deletedAt` <br> 2. Mark all files in the folder as `isDeleted = true` <br> 3. Query child sub-folders via `findByParentAndUserAndIsDeletedFalse()` <br> 4. Recursively apply steps 1–3 to each sub-folder <br> *(This ensures search results, quota calculations, and file listings are clean)* |
| **Bug Fix**   | Addresses **BUG-05** — previously, deleting a parent folder left child files/folders in non-deleted state, leaking into search results |
| **Source**    | `FolderController.deleteFolder()` · `FolderService.deleteFolder()` · `FolderService.softDeleteRecursively()`           |

#### FR-3.3 — Folder Restore (Recursive Cascade)

| Attribute     | Detail                                                                                                                 |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **ID**        | FR-3.3                                                                                                                 |
| **Priority**  | High                                                                                                                   |
| **Endpoint**  | `POST /trash/restore/folder/{id}`                                                                                      |
| **Processing**| Reverse of FR-3.2: recursively unmarks the folder tree and its files, setting `isDeleted = false` and `deletedAt = null` |
| **Source**    | `TrashController.restoreFolder()` · `FolderService.restoreFolder()` · `FolderService.restoreRecursively()`             |

#### FR-3.4 — Folder Permanent Deletion

| Attribute     | Detail                                                                                                                 |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **ID**        | FR-3.4                                                                                                                 |
| **Priority**  | High                                                                                                                   |
| **Endpoint**  | `POST /trash/delete/folder/{id}`                                                                                       |
| **Processing**| 1. Recursively iterate through all files in the folder tree <br> 2. For each file, check deduplication reference count (`countByStoredName()`) <br> 3. Delete physical file from disk **only if reference count ≤ 1** <br> 4. Delete the folder entity from database (JPA cascades delete sub-folders and file records) |
| **Source**    | `TrashController.permanentDeleteFolder()` · `FolderService.permanentDeleteFolder()` · `FolderService.deleteFolderRecursively()` · `FileStorageService.deletePhysicalFileIfLastReference()` |

#### FR-3.5 — Folder Move (With Cycle Detection)

| Attribute     | Detail                                                                                                                 |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **ID**        | FR-3.5                                                                                                                 |
| **Priority**  | Medium                                                                                                                 |
| **Endpoint**  | `POST /folders/move/{id}`                                                                                              |
| **Input**     | `targetFolderId` (null = move to root), `currentFolderId`                                                              |
| **Validation**| • Cannot move a folder into itself <br> • **Cycle detection (BUG-10 Fix)**: walks up the ancestor chain from the target folder to the root; if the source folder ID is found in the chain, the move is rejected with "Cannot move a folder into its own descendant" |
| **Source**    | `FolderController.moveFolder()` · `FolderService.moveFolder()`                                                         |

#### FR-3.6 — Folder Download as ZIP Archive

| Attribute     | Detail                                                                                                                 |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **ID**        | FR-3.6                                                                                                                 |
| **Priority**  | Medium                                                                                                                 |
| **Endpoint**  | `GET /folders/download/{id}`                                                                                           |
| **Processing**| 1. Verify folder ownership <br> 2. Set response headers: `Content-Type: application/zip`, `Content-Disposition: attachment` <br> 3. Create `ZipOutputStream` writing directly to `HttpServletResponse.getOutputStream()` (stream buffering — no in-memory assembly) <br> 4. Recursively traverse folder tree, adding each physical file as a `ZipEntry` with its `originalName` <br> 5. Skip soft-deleted files and folders <br> 6. Include empty folder entries for folder structure preservation |
| **Performance**| Direct output stream buffering prevents server RAM exhaustion for large folder downloads                               |
| **Source**    | `FolderController.downloadFolder()` · `FolderService.downloadFolderAsZip()` · `FolderService.zipFolder()`             |

#### FR-3.7 — Breadcrumb Navigation

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-3.7                                                                                               |
| **Priority**  | Medium                                                                                               |
| **Processing**| Starting from the current folder, walk up the `parent` reference chain to root, collecting each folder. Reverse the list to get root-first ordering. |
| **Source**    | `FolderService.getBreadcrumbs()`                                                                     |

---

### 3.4 Data Deduplication Module

**Source Files:**
- Service: `FileStorageService.java` (upload flow, `computeSha256()`)
- Repository: `FileRepository.java` (`findFirstByFileHash()`, `countByStoredName()`)

#### FR-4.1 — SHA-256 Content-Addressed Deduplication

| Attribute     | Detail                                                                                                                 |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **ID**        | FR-4.1                                                                                                                 |
| **Priority**  | High                                                                                                                   |
| **Algorithm** | SHA-256 (java.security.MessageDigest)                                                                                  |
| **Strategy**  | **Hash-Before-Write**: File bytes are buffered in memory (bounded by 50 MB max-file-size), hashed, then checked against the database *before* any disk I/O occurs. This avoids TOCTOU race conditions. |
| **Processing**| 1. `file.getBytes()` → buffer in memory <br> 2. `computeSha256(bytes)` → 64-character lowercase hex string <br> 3. `findFirstByFileHash(hash)` → check for existing file with identical content <br> 4. If match: reuse `storedName` + `storageNode` from existing record; skip disk write <br> 5. If no match: generate new UUID filename, write to disk, store hash in new record |
| **Source**    | `FileStorageService.uploadFile()` · `FileStorageService.computeSha256()`                                               |

#### FR-4.2 — Deduplication-Aware Permanent Deletion

| Attribute     | Detail                                                                                                                 |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **ID**        | FR-4.2                                                                                                                 |
| **Priority**  | High                                                                                                                   |
| **Processing**| Before deleting a physical file from disk, the system counts all `FileEntity` records sharing the same `storedName` (`countByStoredName()`). The physical file is deleted **only if the reference count is ≤ 1** (i.e., this is the last database record pointing to that physical file). |
| **Source**    | `FileStorageService.permanentDeleteFile()` · `FileStorageService.deletePhysicalFileIfLastReference()` · `FileStorageService.permanentDeleteFileAdmin()` |

---

### 3.5 Distributed Storage Simulation Module

**Source Files:**
- Service: `StorageNodeService.java`
- Config: `AppConfig.java`

#### FR-5.1 — Simulated Node Selection

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-5.1                                                                                               |
| **Priority**  | Medium                                                                                               |
| **Processing**| `ThreadLocalRandom.current().nextInt(1, nodeCount + 1)` generates a random node number. Files are stored at `storage/node{X}/uuid-filename`. |
| **Configuration** | `cloudnest.storage.node-count=3` (configurable), `cloudnest.storage.base-path=storage`          |
| **Source**    | `StorageNodeService.selectNode()` · `StorageNodeService.getFilePath()`                               |

#### FR-5.2 — Automatic Storage Directory Initialization

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-5.2                                                                                               |
| **Priority**  | Medium                                                                                               |
| **Processing**| On application startup (`@PostConstruct`), the system creates `storage/node1/`, `storage/node2/`, `storage/node3/` directories if they do not exist. |
| **Source**    | `AppConfig.initStorageDirectories()`                                                                 |

---

### 3.6 Storage Quota Module

#### FR-6.1 — Per-User Storage Quota Enforcement

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-6.1                                                                                               |
| **Priority**  | High                                                                                                 |
| **Limit**     | 1 GB (1,073,741,824 bytes) per user                                                                 |
| **Processing**| Before every upload: `currentStorage = sumFileSizeByUser(user)`. If `currentStorage + file.getSize() > quotaBytes`, throw `StorageException("Storage quota exceeded")`. Only non-deleted files count toward the quota. |
| **Configuration** | `cloudnest.storage.quota-bytes=1073741824`                                                      |
| **Source**    | `FileStorageService.uploadFile()` · `FileRepository.sumFileSizeByUser()`                             |

#### FR-6.2 — Storage Usage Dashboard Display

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-6.2                                                                                               |
| **Priority**  | Medium                                                                                               |
| **Processing**| Dashboard displays: used storage (formatted), total quota (formatted), percentage bar. Calculated as `(totalStorage * 100) / quotaBytes`, capped at 100%. |
| **Formatting**| `FormatUtils.formatBytes()` → "512 B" / "1.0 KB" / "1.0 MB" / "1.00 GB"                            |
| **Source**    | `DashboardController.showDashboard()` · `FormatUtils.formatBytes()`                                  |

---

### 3.7 File Sharing Module

**Source Files:**
- Controller: `ShareController.java`
- Service: `SharedLinkService.java`
- Entity: `SharedLink.java`
- Repository: `SharedLinkRepository.java`

#### FR-7.1 — Share Link Generation

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-7.1                                                                                               |
| **Priority**  | High                                                                                                 |
| **Endpoint**  | `POST /share/generate/{fileId}`                                                                      |
| **Auth**      | Required (authenticated user only)                                                                   |
| **Processing**| 1. Verify file ownership <br> 2. Generate UUID token (`UUID.randomUUID().toString()`) <br> 3. Create `SharedLink` with 7-day expiration (`LocalDateTime.now().plusDays(7)`) <br> 4. Save to database <br> 5. Return token in flash attribute as `/share/{token}` |
| **Source**    | `ShareController.generateShareLink()` · `SharedLinkService.generateShareLink()`                      |

#### FR-7.2 — Shared File View (Public Access)

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-7.2                                                                                               |
| **Priority**  | High                                                                                                 |
| **Endpoint**  | `GET /share/{token}`                                                                                 |
| **Auth**      | **Not required** — publicly accessible                                                               |
| **Processing**| 1. Look up `SharedLink` by token <br> 2. Check expiration (`isExpired()`) <br> 3. **Security (BUG-07 Fix)**: reject if the underlying file has been soft-deleted <br> 4. Display file info (name, size, type, upload date, shared by, expiry date) |
| **Source**    | `ShareController.viewSharedFile()` · `SharedLinkService.resolveShareLink()`                          |

#### FR-7.3 — Shared File Download (Public Access)

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-7.3                                                                                               |
| **Priority**  | High                                                                                                 |
| **Endpoint**  | `GET /share/download/{token}`                                                                        |
| **Auth**      | **Not required** — publicly accessible                                                               |
| **Processing**| Same validation as FR-7.2. Additionally verifies physical file exists on disk. Returns `ResponseEntity<Resource>` with `Content-Disposition: attachment`. |
| **Source**    | `ShareController.downloadSharedFile()`                                                               |

#### FR-7.4 — Share Link Auto-Expiration

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-7.4                                                                                               |
| **Priority**  | Medium                                                                                               |
| **Processing**| `SharedLink.isExpired()` returns `true` if `expiresAt != null && LocalDateTime.now().isAfter(expiresAt)`. Expired links return a "This share link has expired" error. |
| **Default TTL**| 7 days from creation                                                                                |
| **Source**    | `SharedLink.isExpired()` · `SharedLinkService.resolveShareLink()`                                    |

#### FR-7.5 — Cascade Link Deletion on File Delete

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-7.5                                                                                               |
| **Priority**  | Medium                                                                                               |
| **Processing**| When a file is permanently deleted, all associated share links are deleted first (`deleteByFileId()`). Uses `@Modifying` annotation for bulk delete. |
| **Source**    | `FileStorageService.permanentDeleteFile()` → `SharedLinkService.deleteLinksForFile()` → `SharedLinkRepository.deleteByFileId()` |

---

### 3.8 Recycle Bin (Trash) Module

**Source Files:**
- Controller: `TrashController.java`
- Service: `FileStorageService.java` (trash operations), `FolderService.java` (trash operations)
- Scheduler: `TrashCleanupScheduler.java`

#### FR-8.1 — Trash View

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-8.1                                                                                               |
| **Priority**  | High                                                                                                 |
| **Endpoint**  | `GET /trash`                                                                                         |
| **Processing**| Queries `findByUserAndIsDeletedTrueOrderByUploadedAtDesc()` for deleted files and `findByUserAndIsDeletedTrueOrderByNameAsc()` for deleted folders. Displays them in the trash template. |
| **Source**    | `TrashController.viewTrash()` · `FileStorageService.getTrashFiles()` · `FolderService.getTrashFolders()` |

#### FR-8.2 — File Restore from Trash

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-8.2                                                                                               |
| **Endpoint**  | `POST /trash/restore/file/{id}`                                                                      |
| **Processing**| Sets `isDeleted = false` and `deletedAt = null` on the `FileEntity`.                                 |
| **Source**    | `TrashController.restoreFile()` · `FileStorageService.restoreFile()`                                 |

#### FR-8.3 — File Permanent Deletion from Trash

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-8.3                                                                                               |
| **Endpoint**  | `POST /trash/delete/file/{id}`                                                                       |
| **Processing**| 1. Check deduplication reference count <br> 2. Delete physical file only if last reference <br> 3. Delete all share links <br> 4. Delete database record |
| **Source**    | `TrashController.permanentDeleteFile()` · `FileStorageService.permanentDeleteFile()`                  |

#### FR-8.4 — Automatic Trash Purge (Scheduled Task)

| Attribute     | Detail                                                                                                                 |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- |
| **ID**        | FR-8.4                                                                                                                 |
| **Priority**  | Medium                                                                                                                 |
| **Schedule**  | Daily at **02:00 AM** — cron expression: `0 0 2 * * *`                                                                |
| **Retention** | 30 days — files with `deletedAt` older than 30 days are permanently purged                                             |
| **Processing**| 1. Calculate cutoff: `LocalDateTime.now().minusDays(30)` <br> 2. Query `findByIsDeletedTrueAndDeletedAtBefore(cutoff)` <br> 3. For each expired file, call `permanentDeleteFileAdmin()` (dedup-safe deletion) <br> 4. Errors on individual files are logged but do not halt the batch |
| **Source**    | `TrashCleanupScheduler.purgeExpiredTrashItems()` — requires `@EnableScheduling` on `CloudNestApplication`              |

---

### 3.9 Administrative Module

**Source Files:**
- Controller: `AdminController.java`

#### FR-9.1 — Admin Dashboard

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-9.1                                                                                               |
| **Priority**  | Medium                                                                                               |
| **Endpoint**  | `GET /admin/dashboard`                                                                               |
| **Auth**      | `ROLE_ADMIN` only                                                                                    |
| **Data**      | Total users, total active files, total storage (formatted), per-node file counts (node1/node2/node3), list of all users, list of all files |
| **Source**    | `AdminController.showAdminDashboard()`                                                               |

#### FR-9.2 — User Role Toggle

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-9.2                                                                                               |
| **Priority**  | Medium                                                                                               |
| **Endpoint**  | `POST /admin/users/toggle-role/{id}`                                                                 |
| **Processing**| Toggles user role between `ROLE_USER` ↔ `ROLE_ADMIN`. **Self-demotion protection**: an admin cannot demote their own account. |
| **Source**    | `AdminController.toggleUserRole()`                                                                   |

#### FR-9.3 — Administrative File Deletion

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-9.3                                                                                               |
| **Priority**  | Medium                                                                                               |
| **Endpoint**  | `POST /admin/files/delete/{id}`                                                                      |
| **Processing**| Permanently deletes any file from the system (no ownership check). Dedup-safe. Cascades to share links. |
| **Source**    | `AdminController.adminDeleteFile()` · `FileStorageService.permanentDeleteFileAdmin()`                |

---

### 3.10 Dashboard & Enterprise Visualization Module

**Source Files:**
- Controller: `DashboardController.java`
- DTO: `DashboardDto.java`

#### FR-10.1 — User Dashboard

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-10.1                                                                                              |
| **Endpoint**  | `GET /dashboard`                                                                                     |
| **Data**      | Total files, total folders, storage used (bytes + formatted), quota (bytes + formatted), quota percentage, 5 most recent uploads, node distribution map, file type distribution map (simplified MIME categories) |
| **MIME Mapping** | `image/*` → "Images", `application/pdf` → "PDF", `video/*` → "Videos", `audio/*` → "Audio", `*zip*` / `*rar*` → "Archives", `*word*` / `*document*` → "Documents", `*sheet*` / `*excel*` → "Spreadsheets", `text/*` → "Text", default → "Other" |
| **Source**    | `DashboardController.showDashboard()` · `DashboardController.simplifyFileType()`                     |

#### FR-10.2 — Storage Node Topology Page

| Attribute     | Detail                                                               |
| ------------- | -------------------------------------------------------------------- |
| **ID**        | FR-10.2                                                              |
| **Endpoint**  | `GET /nodes`                                                         |
| **Data**      | Per-node: used storage (formatted), capacity (quota/3), usage percentage, file count |
| **Source**    | `DashboardController.showNodes()`                                    |

#### FR-10.3 — Deduplication Center Page

| Attribute     | Detail                                           |
| ------------- | ------------------------------------------------ |
| **ID**        | FR-10.3                                          |
| **Endpoint**  | `GET /deduplication`                             |
| **Source**    | `DashboardController.showDeduplication()`        |

#### FR-10.4 — Replication View Page

| Attribute     | Detail                                           |
| ------------- | ------------------------------------------------ |
| **ID**        | FR-10.4                                          |
| **Endpoint**  | `GET /replication`                               |
| **Source**    | `DashboardController.showReplication()`          |

#### FR-10.5 — Network Activity Dashboard Page

| Attribute     | Detail                                           |
| ------------- | ------------------------------------------------ |
| **ID**        | FR-10.5                                          |
| **Endpoint**  | `GET /network`                                   |
| **Source**    | `DashboardController.showNetwork()`              |

#### FR-10.6 — Storage Analytics Page

| Attribute     | Detail                                           |
| ------------- | ------------------------------------------------ |
| **ID**        | FR-10.6                                          |
| **Endpoint**  | `GET /analytics`                                 |
| **Source**    | `DashboardController.showAnalytics()`            |

#### FR-10.7 — System Health Monitoring Page

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-10.7                                                                                              |
| **Endpoint**  | `GET /monitoring`                                                                                    |
| **Data**      | **Real JVM metrics**: max memory, allocated memory, free memory, used memory, memory health percentage (100 - used%), SVG stroke offset for circular progress indicator. **Deduplication stats**: total active files, unique hashes, deduplicated files saved. |
| **Source**    | `DashboardController.showMonitoring()` · `FileRepository.countActiveFiles()` · `FileRepository.countUniqueHashes()` |

---

### 3.11 Error Handling Module

**Source Files:**
- `GlobalExceptionHandler.java`
- `FileNotFoundException.java`
- `StorageException.java`
- `error/404.html`, `error/500.html`, `error.html`

#### FR-11.1 — Custom Error Pages

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-11.1                                                                                              |
| **Pages**     | • `404.html` — "Page not found" with Lucide cloud-off icon and dashboard link <br> • `500.html` — "Server error" with go-back button and dashboard link <br> • `error.html` — Generic fallback error page |
| **Behavior**  | Server stack traces are **never** exposed to end users                                               |

#### FR-11.2 — Global Exception Handling

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | FR-11.2                                                                                              |
| **Processing**| `@ControllerAdvice` catches all exceptions globally: <br> • `FileNotFoundException` → redirect to `/files` with error message (or 404 JSON for download/preview requests) <br> • `StorageException` → redirect to `/files` with storage error message <br> • `AccessDeniedException` → re-thrown to Spring Security for 403 handling <br> • All others → logged at ERROR level, redirect to `/dashboard` with generic message |
| **Smart Response** | `expectsHtmlResponse()` checks URI (download/preview) and `Accept` header to decide between redirect and ResponseEntity |
| **Source**    | `GlobalExceptionHandler.handleFileNotFound()` · `handleStorageException()` · `handleGenericException()` |

---

*End of Part 2. Continue to **Part 3: System Architecture & Data Models** for the complete technical architecture.*
