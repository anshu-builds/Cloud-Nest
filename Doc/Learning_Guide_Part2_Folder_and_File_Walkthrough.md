# ☁️ CloudNest — Master Learning Guide — Part 2

> **Chapters 3–4: Folder-by-Folder & File-by-File Walkthrough**
> Level: 🟡 Intermediate

---

# Chapter 3 — Folder-by-Folder Project Walkthrough

```
CloudNest/
├── .env                          ← Environment secrets (DB password)
├── .gitignore                    ← Files excluded from Git
├── pom.xml                       ← Maven build configuration
├── schema.sql                    ← PostgreSQL schema reference
├── storage/                      ← Physical file storage (simulated nodes)
│   ├── node1/                    ← Storage Node 1
│   ├── node2/                    ← Storage Node 2
│   └── node3/                    ← Storage Node 3
├── Doc/                          ← Project documentation
├── src/
│   ├── main/
│   │   ├── java/com/cloudnest/   ← All Java source code
│   │   │   ├── CloudNestApplication.java  ← Entry point
│   │   │   ├── config/           ← Configuration classes
│   │   │   ├── controller/       ← HTTP request handlers
│   │   │   ├── dto/              ← Data Transfer Objects
│   │   │   ├── entity/           ← Database table mappings
│   │   │   ├── exception/        ← Custom exceptions
│   │   │   ├── repository/       ← Database access interfaces
│   │   │   ├── security/         ← Security configuration
│   │   │   ├── service/          ← Business logic
│   │   │   └── util/             ← Utility classes
│   │   └── resources/
│   │       ├── application.properties  ← App configuration
│   │       ├── static/           ← CSS, JavaScript files
│   │       │   ├── css/
│   │       │   └── js/
│   │       └── templates/        ← Thymeleaf HTML templates
│   │           ├── fragments/    ← Reusable HTML fragments
│   │           ├── error/        ← Error page templates
│   │           ├── dashboard.html
│   │           ├── files.html
│   │           ├── login.html
│   │           ├── register.html
│   │           ├── admin.html
│   │           ├── trash.html
│   │           ├── shared.html
│   │           └── ... (more pages)
│   └── test/                     ← Test source code
└── target/                       ← Maven build output (auto-generated)
```

### Why Each Folder Exists

| Folder | Purpose | Connects To |
|--------|---------|-------------|
| `config/` | Centralized configuration beans. `AppConfig` reads properties and creates storage dirs. `DatabaseMigrationConfig` fixes data issues at startup. | `application.properties` → Entities |
| `controller/` | HTTP routing. Maps URLs to service methods. Returns template names. | Templates (HTML) ← Controllers → Services |
| `dto/` | Data envelopes. Carry form data in, display data out. Prevent entity exposure. | Controllers ↔ DTOs ↔ Services |
| `entity/` | JPA database table mappings. Define the data model. | Entities → Repositories → Database |
| `exception/` | Custom exception classes + global handler. Centralize error responses. | Services throw → GlobalExceptionHandler catches |
| `repository/` | Database query interfaces. Spring generates implementations. | Services → Repositories → Hibernate → PostgreSQL |
| `security/` | Spring Security configuration. Authentication and authorization rules. | All HTTP requests pass through SecurityConfig |
| `service/` | Business logic. Orchestrates multiple repositories and operations. | Controllers → Services → Repositories |
| `util/` | Shared utility functions (e.g., `formatBytes()`). DRY principle. | Used by multiple controllers/services |
| `templates/` | Thymeleaf HTML files. Server-rendered pages returned by controllers. | Controllers pass Model → Templates render HTML |
| `static/` | CSS and JavaScript files served directly to the browser. | Referenced by templates via `<link>` and `<script>` |
| `storage/` | Physical file storage. Simulated distributed nodes (node1, node2, node3). | Created by `AppConfig.initStorageDirectories()` |

### How Folders Connect — The Dependency Flow

```
                 ┌─────────────────┐
                 │   Templates     │ ◀── Returns HTML view names
                 └────────┬────────┘
                          │
                 ┌────────▼────────┐
                 │  Controllers    │ ◀── Handles HTTP requests
                 │ (controller/)   │     Uses DTOs for form data
                 └────────┬────────┘
                          │
              ┌───────────┼───────────┐
              │           │           │
     ┌────────▼──────┐   │  ┌────────▼──────┐
     │    DTOs       │   │  │  Exceptions   │
     │   (dto/)      │   │  │ (exception/)  │
     └───────────────┘   │  └───────────────┘
                         │
                ┌────────▼────────┐
                │    Services     │ ◀── Business logic
                │  (service/)     │
                └────────┬────────┘
                         │
                ┌────────▼────────┐
                │  Repositories   │ ◀── Data access (Spring Data JPA)
                │ (repository/)   │
                └────────┬────────┘
                         │
                ┌────────▼────────┐
                │    Entities     │ ◀── Database table mappings
                │   (entity/)     │
                └────────┬────────┘
                         │
                ┌────────▼────────┐
                │   PostgreSQL    │ ◀── Physical database
                └─────────────────┘
```

---

# Chapter 4 — File-by-File Deep Explanation

## 4.1 CloudNestApplication.java — The Entry Point

**File**: [CloudNestApplication.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/CloudNestApplication.java)

```java
package com.cloudnest;                                    // Package declaration

import org.springframework.boot.SpringApplication;         // Bootstrap class
import org.springframework.boot.autoconfigure.SpringBootApplication; // Meta-annotation
import org.springframework.scheduling.annotation.EnableScheduling;   // Enables @Scheduled

@SpringBootApplication    // = @Configuration + @EnableAutoConfiguration + @ComponentScan
@EnableScheduling         // Allows TrashCleanupScheduler to run its cron job
public class CloudNestApplication {
    public static void main(String[] args) {
        SpringApplication.run(CloudNestApplication.class, args);  // Bootstraps everything
    }
}
```

**What happens when you run this file:**
1. JVM calls `main()`
2. `SpringApplication.run()` starts the Spring Boot bootstrap process
3. Spring scans `com.cloudnest` and all sub-packages for annotated classes
4. Creates all beans (controllers, services, repositories, configs)
5. Starts embedded Tomcat on port 8080
6. `AppConfig.initStorageDirectories()` creates `storage/node1/`, `node2/`, `node3/`
7. `DatabaseMigrationConfig.migrateNullVersions()` fixes null version fields
8. Application is ready to accept HTTP requests

### How Senior Engineers Think About This
- "This is the entry point. If the app doesn't start, check the logs here first."
- "The `@EnableScheduling` annotation means there's a background task somewhere — find it."
- "Package scanning starts from `com.cloudnest` — any class outside this package won't be detected."

---

## 4.2 AppConfig.java — Configuration & Initialization

**File**: [AppConfig.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/config/AppConfig.java)

```java
@Configuration  // "I define configuration beans"
public class AppConfig {

    @Value("${cloudnest.storage.base-path}")  // Reads from application.properties
    private String storagePath;               // → "storage"

    @Value("${cloudnest.storage.node-count}")
    private int nodeCount;                    // → 3

    @PostConstruct  // Runs ONCE after bean creation
    public void initStorageDirectories() throws IOException {
        for (int i = 1; i <= nodeCount; i++) {
            Path nodePath = Paths.get(storagePath, "node" + i);
            if (!Files.exists(nodePath)) {
                Files.createDirectories(nodePath);  // Creates storage/node1/, node2/, node3/
            }
        }
    }

    // Getter methods for other beans to use
    public String getStoragePath() { return storagePath; }
    public int getNodeCount() { return nodeCount; }
}
```

### Why This Exists
Without this, when a user uploads a file and it tries to write to `storage/node2/`, the directory wouldn't exist and the app would crash with an `IOException`. This **preventive initialization** ensures the infrastructure is ready before any user request arrives.

### `@PostConstruct` — How It Works Internally
1. Spring creates the `AppConfig` bean
2. Spring injects `@Value` fields from `application.properties`
3. Spring sees `@PostConstruct` and calls `initStorageDirectories()` **once**
4. The method runs — directories are created
5. Bean is now fully initialized and ready to use

---

## 4.3 DatabaseMigrationConfig.java — Data Fix at Startup

**File**: [DatabaseMigrationConfig.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/config/DatabaseMigrationConfig.java)

```java
@Configuration
public class DatabaseMigrationConfig {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseMigrationConfig(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;  // Raw SQL executor
    }

    @PostConstruct
    public void migrateNullVersions() {
        try {
            // Fix: @Version was added after data already existed
            // Existing rows had NULL version, causing NullPointerException
            jdbcTemplate.execute("UPDATE folders SET version = 0 WHERE version IS NULL");
            jdbcTemplate.execute("UPDATE files SET version = 0 WHERE version IS NULL");
        } catch (Exception e) {
            // Normal on fresh databases — tables might not exist yet
            System.err.println("Migration failed (this is normal on fresh DBs): " + e.getMessage());
        }
    }
}
```

### Why This Exists — The Real Story
When `@Version` was added to `FileEntity` and `Folder` entities, existing records in the database had `NULL` in the `version` column. When Hibernate tried to increment `null.longValue()`, it threw a `NullPointerException`. This migration sets all NULL versions to 0, fixing the data.

### Engineering Lesson
> When you add a new column with `@Version` or any non-nullable field to existing entities, you MUST migrate existing data. In production, this would be a Flyway migration script (`V2__fix_null_versions.sql`), not a `@PostConstruct` hack.

---

## 4.4 SecurityConfig.java — The Security Fortress

**File**: [SecurityConfig.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/security/SecurityConfig.java)

This is one of the **most important files** in the project. It defines every security rule.

### Line-by-Line Breakdown

```java
@Configuration                    // Marks as a configuration class (defines beans)
@EnableWebSecurity                // Activates Spring Security's web module
public class SecurityConfig {

    @Bean                         // Creates a bean managed by Spring
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);  // Cost factor 12 = 2^12 iterations
    }
```

**Why cost factor 12?** Default is 10. Higher = slower = more secure. At 12, hashing takes ~250ms — fast enough for login, slow enough to deter brute-force.

```java
    @Bean
    public DaoAuthenticationProvider authenticationProvider(UserService userService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userService);   // "Where to find users"
        provider.setPasswordEncoder(passwordEncoder()); // "How to verify passwords"
        return provider;
    }
```

**How authentication works internally:**
1. User submits username + password on login form
2. Spring Security calls `userService.loadUserByUsername(username)`
3. `UserService` fetches the user from PostgreSQL
4. Spring Security calls `passwordEncoder.matches(submittedPassword, storedHash)`
5. BCrypt verifies the password against the stored hash
6. If match → create session → redirect to `/dashboard`
7. If no match → redirect to `/login?error=true`

```java
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                // PUBLIC: No login needed
                .requestMatchers("/login", "/register", "/share/**",
                    "/css/**", "/js/**", "/images/**", "/webjars/**",
                    "/error", "/actuator/health", "/actuator/info"
                ).permitAll()
                // ADMIN ONLY
                .requestMatchers("/admin/**").hasRole("ADMIN")
                // EVERYTHING ELSE: Must be logged in
                .anyRequest().authenticated()
            )
```

**The order matters!** Spring Security evaluates rules top-to-bottom. The first match wins. That's why specific rules (`/admin/**`) come before the catch-all (`.anyRequest()`).

```java
            .formLogin(form -> form
                .loginPage("/login")                    // Our custom login page
                .loginProcessingUrl("/login")           // Where the form POSTs to
                .defaultSuccessUrl("/dashboard", true)  // Where to go after login
                .failureUrl("/login?error=true")        // Where to go on failure
                .usernameParameter("username")          // HTML form field name
                .passwordParameter("password")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)            // Destroy the server session
                .deleteCookies("JSESSIONID")            // Delete the cookie
                .permitAll()
            );
        return http.build();
    }
}
```

> **🎓 Interview Question**: "What is a SecurityFilterChain?" → It's an ordered list of servlet filters that Spring Security applies to every incoming HTTP request. Each filter handles a specific concern: CSRF validation, authentication, authorization, session management, etc. The `filterChain()` method configures which filters are active and how they behave.

---

## 4.5 UserService.java — Authentication Core

**File**: [UserService.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/service/UserService.java)

This class implements `UserDetailsService` — the interface Spring Security uses to load user data during authentication.

### Key Method: `loadUserByUsername()`

```java
@Override
public UserDetails loadUserByUsername(String loginId) throws UsernameNotFoundException {
    // Supports login by username OR email (case-insensitive)
    User user = userRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase(loginId, loginId)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + loginId));

    // Convert our entity to Spring Security's UserDetails
    return new org.springframework.security.core.userdetails.User(
            user.getUsername(),
            user.getPassword(),    // BCrypt hash from DB
            Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
    );
}
```

**Why we return Spring's `User` class, not our entity:**
Spring Security has its own `UserDetails` interface with methods like `getAuthorities()`, `isAccountNonLocked()`, etc. We convert our domain entity into this interface so Spring Security can work with it.

### Registration Logic

```java
public User registerUser(UserRegistrationDto dto) {
    // Step 1: Passwords must match
    if (!dto.getPassword().equals(dto.getConfirmPassword()))
        throw new IllegalArgumentException("Passwords do not match");

    // Step 2: Username must be unique
    if (userRepository.existsByUsername(dto.getUsername()))
        throw new IllegalArgumentException("Username already taken");

    // Step 3: Email must be unique
    if (userRepository.existsByEmail(dto.getEmail()))
        throw new IllegalArgumentException("Email already registered");

    // SECURITY: Always assign ROLE_USER — never trust user input for roles
    String finalRole = "ROLE_USER";

    // Step 4: Build and save
    User user = User.builder()
            .username(dto.getUsername())
            .email(dto.getEmail())
            .password(passwordEncoder.encode(dto.getPassword()))  // BCrypt hash!
            .role(finalRole)
            .build();

    return userRepository.save(user);
}
```

---

## 4.6 FileStorageService.java — The Heart of the System

**File**: [FileStorageService.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/service/FileStorageService.java) (426 lines — the largest service)

This file handles **all file operations**: upload, download, delete, restore, move, search.

### Upload Flow (The Most Complex Operation)

```java
public FileDto uploadFile(MultipartFile file, User user, Long folderId) {
    // Step 1: Validate — is the file empty?
    if (file.isEmpty()) throw new StorageException("Cannot upload an empty file");

    // Step 2: Quota check — has the user exceeded 1GB?
    long currentStorage = fileRepository.sumFileSizeByUser(user);
    if (currentStorage + file.getSize() > quotaBytes)
        throw new StorageException("Storage quota exceeded.");

    // Step 3: Extension validation — block dangerous file types
    String extension = originalName.substring(originalName.lastIndexOf(".")).toLowerCase();
    if (BLOCKED_EXTENSIONS.contains(extension))  // .exe, .bat, .sh, .ps1, .cmd
        throw new StorageException("File type not allowed: " + extension);

    // Step 4: HASH-BEFORE-WRITE — compute SHA-256 before touching disk
    byte[] fileBytes = file.getBytes();
    String fileHash = computeSha256(fileBytes);

    // Step 5: Deduplication check
    FileEntity existingFile = fileRepository.findFirstByFileHash(fileHash);
    if (existingFile != null) {
        // DEDUP HIT: Reuse existing physical file
        storedName = existingFile.getStoredName();
        node = existingFile.getStorageNode();
    } else {
        // NEW FILE: Write to disk
        storedName = UUID.randomUUID() + extension;
        node = storageNodeService.selectNode();
        Files.write(targetPath, fileBytes);
    }

    // Step 6: Save metadata to database
    FileEntity fileEntity = FileEntity.builder()
        .originalName(originalName).storedName(storedName)
        .fileType(file.getContentType()).fileSize(file.getSize())
        .storageNode(node).fileHash(fileHash)
        .user(user).folder(folder)
        .build();

    return convertToDto(fileRepository.save(fileEntity));
}
```

### Why Hash-Before-Write (Not Write-Then-Hash)?
This is a **TOCTOU (Time-of-Check-to-Time-of-Use)** prevention. If we wrote the file first, then computed the hash, and then checked for duplicates — a race condition could exist where two identical files arrive simultaneously, both pass the "no duplicate" check, and both get written. By hashing in memory first, we avoid touching disk for duplicates entirely.

### SHA-256 Hash Computation
```java
private String computeSha256(byte[] data) {
    MessageDigest digest = MessageDigest.getInstance("SHA-256");
    byte[] hashBytes = digest.digest(data);
    // Convert to hex string: each byte → 2 hex characters
    StringBuilder hexString = new StringBuilder(64);
    for (byte b : hashBytes) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1) hexString.append('0');
        hexString.append(hex);
    }
    return hexString.toString();
    // Result: "a1b2c3d4e5f6..." (64 characters)
}
```

### Permanent Delete with Dedup Awareness
```java
public void permanentDeleteFile(Long fileId, User user) {
    FileEntity fileEntity = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileNotFoundException("File not found"));

    // CRITICAL: Check if other records share this physical file
    long referenceCount = fileRepository.countByStoredName(fileEntity.getStoredName());

    if (referenceCount <= 1) {
        // Last reference — safe to delete the physical file
        Files.deleteIfExists(Paths.get(filePath));
    }
    // If referenceCount > 1, keep the physical file (other records use it)

    sharedLinkService.deleteLinksForFile(fileEntity.getId());  // Clean up shared links
    fileRepository.delete(fileEntity);                          // Delete the DB record
}
```

---

## 4.7 FolderService.java — Recursive Operations

**File**: [FolderService.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/service/FolderService.java)

### Recursive Soft Delete (BUG-05 Fix)
```java
private void softDeleteRecursively(Folder folder) {
    LocalDateTime now = LocalDateTime.now();
    folder.setDeleted(true);
    folder.setDeletedAt(now);
    folderRepository.save(folder);

    // Soft-delete all files in this folder
    if (folder.getFiles() != null) {
        for (FileEntity file : folder.getFiles()) {
            file.setDeleted(true);
            file.setDeletedAt(now);
        }
        fileRepository.saveAll(folder.getFiles());
    }

    // Recurse into subfolders
    List<Folder> subFolders = folderRepository
        .findByParentAndUserAndIsDeletedFalse(folder, folder.getUser());
    for (Folder sub : subFolders) {
        softDeleteRecursively(sub);  // ← RECURSION
    }
}
```

### Cycle Detection in Move (BUG-10 Fix)
```java
public void moveFolder(Long folderId, Long targetFolderId, User user) {
    // ... get folder and targetFolder ...

    // Walk up from target to root, checking for cycles
    Folder ancestor = targetFolder;
    while (ancestor != null) {
        if (ancestor.getId().equals(folderId)) {
            throw new IllegalArgumentException(
                "Cannot move a folder into its own descendant");
        }
        ancestor = ancestor.getParent();  // Walk up the tree
    }

    folder.setParent(targetFolder);
    folderRepository.save(folder);
}
```

### ZIP Download
```java
private void zipFolder(Folder folder, String currentPath, ZipOutputStream zos)
        throws Exception {
    String newPath = currentPath + folder.getName() + "/";

    zos.putNextEntry(new ZipEntry(newPath));  // Add folder entry
    zos.closeEntry();

    // Add files
    for (FileEntity fileEntity : folder.getFiles()) {
        if (fileEntity.isDeleted()) continue;
        Path path = Paths.get(storageNodeService.getFilePath(
            fileEntity.getStorageNode(), fileEntity.getStoredName()));

        if (Files.exists(path)) {
            zos.putNextEntry(new ZipEntry(newPath + fileEntity.getOriginalName()));
            try (InputStream is = Files.newInputStream(path)) {
                is.transferTo(zos);  // Stream file content into ZIP
            }
            zos.closeEntry();
        }
    }

    // Recursively add sub-folders
    for (Folder subFolder : folder.getSubFolders()) {
        if (subFolder.isDeleted()) continue;
        zipFolder(subFolder, newPath, zos);
    }
}
```

---

## 4.8 Entity Classes — The Data Model

### User.java
**File**: [User.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/entity/User.java)

| Field | Type | Column | Purpose |
|-------|------|--------|---------|
| `id` | `Long` | `BIGSERIAL PK` | Auto-increment primary key |
| `version` | `Long` | `BIGINT` | Optimistic locking |
| `username` | `String` | `VARCHAR(50) UNIQUE` | Login identifier |
| `email` | `String` | `VARCHAR(100) UNIQUE` | Contact info |
| `password` | `String` | `VARCHAR(255)` | BCrypt hash (never plain text!) |
| `role` | `String` | `VARCHAR(20)` | `ROLE_USER` or `ROLE_ADMIN` |
| `createdAt` | `LocalDateTime` | `TIMESTAMP` | Auto-set on creation |
| `files` | `List<FileEntity>` | (mapped) | One-to-many relationship |
| `folders` | `List<Folder>` | (mapped) | One-to-many relationship |

### FileEntity.java
**File**: [FileEntity.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/entity/FileEntity.java)

| Field | Type | Column | Purpose |
|-------|------|--------|---------|
| `id` | `Long` | `BIGSERIAL PK` | Primary key |
| `version` | `Long` | `BIGINT` | Optimistic locking |
| `originalName` | `String` | `VARCHAR(255)` | What the user called the file |
| `storedName` | `String` | `VARCHAR(255)` | UUID name on disk (prevents collisions) |
| `fileType` | `String` | `VARCHAR(100)` | MIME type (e.g., `application/pdf`) |
| `fileSize` | `Long` | `BIGINT` | Size in bytes |
| `storageNode` | `String` | `VARCHAR(20)` | Which node stores the file |
| `fileHash` | `String` | `VARCHAR(64)` | SHA-256 hash for deduplication |
| `user` | `User` | `FK → users.id` | File owner |
| `folder` | `Folder` | `FK → folders.id` | Parent folder (null = root) |
| `isDeleted` | `boolean` | `BOOLEAN` | Soft delete flag |
| `deletedAt` | `LocalDateTime` | `TIMESTAMP` | When moved to trash |
| `uploadedAt` | `LocalDateTime` | `TIMESTAMP` | When uploaded |

### Why `storedName` Uses UUID Instead of `originalName`
If two users both upload "report.pdf", the files would collide on disk. UUIDs guarantee uniqueness: `a1b2c3d4-e5f6-7890-abcd-ef1234567890.pdf`.

### Folder.java — Self-Referencing Relationship
**File**: [Folder.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/entity/Folder.java)

The most interesting part of `Folder` is the **self-referencing relationship**:
```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "parent_id")
private Folder parent;  // My parent folder (null = root level)

@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
private List<Folder> subFolders = new ArrayList<>();  // My child folders
```

This creates a **tree structure** in the database:
```
Root (parent_id = NULL)
├── Documents (parent_id = 1)
│   ├── Work (parent_id = 2)
│   └── Personal (parent_id = 2)
└── Photos (parent_id = 1)
    └── Vacation (parent_id = 5)
```

### SharedLink.java — Time-Bound Access
**File**: [SharedLink.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/entity/SharedLink.java)

```java
public boolean isExpired() {
    return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
}
```
Simple but critical. If `expiresAt` is null, the link never expires. If it's in the past, the link is dead.

---

## 4.9 Controller Classes — HTTP Routing Layer

### AuthController.java
**File**: [AuthController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/controller/AuthController.java)

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `GET /login` | `showLoginPage()` | Render login form |
| `GET /register` | `showRegistrationPage()` | Render registration form with empty DTO |
| `POST /register` | `registerUser()` | Process registration, redirect to login |
| `GET /` | `redirectToDashboard()` | Root URL → redirect to `/dashboard` |

### FileController.java
**File**: [FileController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/controller/FileController.java)

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `GET /files` | `listFiles()` | List files (optional folder filter) |
| `POST /files/upload` | `uploadFiles()` | Multi-file upload with batch error handling |
| `GET /files/download/{id}` | `downloadFile()` | Stream file as download |
| `GET /files/preview/{id}` | `previewFile()` | Stream file inline (for images, PDFs) |
| `POST /files/delete/{id}` | `deleteFile()` | Soft-delete a file |
| `POST /files/move/{id}` | `moveFile()` | Move file to another folder |
| `GET /files/search` | `searchFiles()` | Search by name or type |

### AdminController.java
**File**: [AdminController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/controller/AdminController.java)

| Endpoint | Method | Purpose |
|----------|--------|---------|
| `GET /admin/dashboard` | `showAdminDashboard()` | System metrics, user list, file list |
| `POST /admin/users/toggle-role/{id}` | `toggleUserRole()` | Promote/demote user |
| `POST /admin/files/delete/{id}` | `adminDeleteFile()` | Force-delete any file |

**Security**: All `/admin/**` routes require `ROLE_ADMIN` (enforced by `SecurityConfig`).

---

## 4.10 Repository Interfaces — The Query Layer

### FileRepository.java — The Most Complex Repository
**File**: [FileRepository.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/repository/FileRepository.java)

| Method | Generated SQL | Purpose |
|--------|--------------|---------|
| `findByUserAndIsDeletedFalseOrderByUploadedAtDesc` | `WHERE user_id=? AND is_deleted=false ORDER BY uploaded_at DESC` | All active files for user |
| `findByUserAndFolderIsNullAndIsDeletedFalseOrderByUploadedAtDesc` | `WHERE user_id=? AND folder_id IS NULL AND is_deleted=false` | Root-level files only |
| `countByUserAndIsDeletedFalse` | `SELECT COUNT(*) WHERE user_id=? AND is_deleted=false` | Total file count |
| `sumFileSizeByUser` | `SELECT COALESCE(SUM(file_size), 0) WHERE user_id=? AND is_deleted=false` | Storage usage |
| `findTop5ByUserAndIsDeletedFalseOrderByUploadedAtDesc` | `WHERE ... ORDER BY uploaded_at DESC LIMIT 5` | Recent files for dashboard |
| `findFirstByFileHash` | `WHERE file_hash=? LIMIT 1` | Deduplication lookup |
| `countByStoredName` | `SELECT COUNT(*) WHERE stored_name=?` | Dedup-aware deletion check |

---

## 4.11 application.properties — The Configuration Bible

**File**: [application.properties](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/resources/application.properties)

```properties
# Server
server.port=8080                          # Tomcat listens on port 8080

# Database
spring.datasource.url=jdbc:postgresql://localhost:5432/cloudnest_db
spring.datasource.username=postgres
spring.datasource.password=${DB_PASSWORD}  # From .env file

# Hibernate
spring.jpa.hibernate.ddl-auto=${DDL_AUTO:update}  # Default: update
spring.jpa.show-sql=${SHOW_SQL:false}
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect

# Thymeleaf
spring.thymeleaf.cache=false              # Disable cache for hot-reload in dev

# File Upload
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB

# Custom CloudNest Properties
cloudnest.storage.base-path=${STORAGE_BASE_PATH:storage}
cloudnest.storage.quota-bytes=1073741824  # 1 GB per user
cloudnest.storage.node-count=3            # 3 simulated storage nodes

# Actuator
management.endpoints.web.exposure.include=health,info

# Logging
logging.level.com.cloudnest=DEBUG
```

### The `${VAR:default}` Syntax
`${DDL_AUTO:update}` means: "Use the `DDL_AUTO` environment variable. If it doesn't exist, use `update` as the default." This is how we externalize configuration without hardcoding.

---

## 4.12 GlobalExceptionHandler.java — Centralized Error Handling

**File**: [GlobalExceptionHandler.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/exception/GlobalExceptionHandler.java)

```java
@ControllerAdvice  // "This handles exceptions for ALL controllers"
public class GlobalExceptionHandler {

    @ExceptionHandler(FileNotFoundException.class)  // Catches this specific exception
    public Object handleFileNotFound(FileNotFoundException ex, ...) {
        // For download/preview → return 404 HTTP status
        // For browser requests → redirect to /files with error message
    }

    @ExceptionHandler(StorageException.class)
    public String handleStorageException(StorageException ex, ...) {
        // Redirect to /files with storage error message
    }

    @ExceptionHandler(Exception.class)  // Catch-all for everything else
    public String handleGenericException(Exception ex, ...) {
        // Re-throw AccessDeniedException (let Spring Security handle 403)
        // Log everything else and redirect to /dashboard
    }
}
```

### How `@ControllerAdvice` Works
1. Spring registers this class as a global exception handler
2. When any controller method throws an exception, Spring finds the best matching `@ExceptionHandler`
3. The most specific match wins (`FileNotFoundException` before `Exception`)
4. The handler processes the error and returns a response

---

## 4.13 FormatUtils.java — DRY Utility

**File**: [FormatUtils.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/util/FormatUtils.java)

```java
public final class FormatUtils {
    private FormatUtils() {} // Private constructor = cannot be instantiated

    public static String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
```

### Why This Exists
This method was **duplicated in 3 controllers** (AdminController, DashboardController, ShareController). Extracting it into a utility class follows the **DRY principle** (Don't Repeat Yourself). Now all controllers call `FormatUtils.formatBytes()`.

---

## 4.14 DashboardController.java — The Metrics Aggregator

**File**: [DashboardController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/controller/DashboardController.java) (241 lines)

This is the **most query-heavy controller** — each dashboard load runs 5+ database queries.

### Dependencies
```java
private final UserService userService;           // Resolve Principal → User entity
private final FileStorageService fileStorageService; // Get recent files
private final FolderService folderService;        // Count folders
private final FileRepository fileRepository;      // Direct repo access (BAD — should go through service)

@Value("${cloudnest.storage.quota-bytes:1073741824}")
private long quotaBytes;  // 1 GB per user (read from application.properties)
```

> **⚠️ Audit Finding**: `FileRepository` is injected directly into the controller, bypassing the service layer. This should be refactored into a `DashboardService`.

### Main Method: `showDashboard()`

```java
@GetMapping("/dashboard")
public String showDashboard(Principal principal, Model model) {
    // Step 1: Resolve the logged-in user
    User user = userService.findByUsername(principal.getName());

    // Step 2: Gather statistics (each = 1 database query)
    long totalFiles = fileRepository.countByUserAndIsDeletedFalse(user);    // Query 1
    long totalStorage = fileRepository.sumFileSizeByUser(user);              // Query 2
    long totalFolders = folderService.countByUser(user);                    // Query 3
    List<FileDto> recentFiles = fileStorageService.getRecentFiles(user);    // Query 4

    // Step 3: Build node distribution (Query 5)
    Map<String, Long> nodeDistribution = new LinkedHashMap<>();
    for (Object[] row : fileRepository.countByStorageNode(user)) {
        nodeDistribution.put((String) row[0], (Long) row[1]);
    }

    // Step 4: Build file type distribution (Query 6)
    Map<String, Long> typeDistribution = new LinkedHashMap<>();
    for (Object[] row : fileRepository.countByFileType(user)) {
        String type = simplifyFileType((String) row[0]);  // "application/pdf" → "PDF"
        typeDistribution.merge(type, (Long) row[1], Long::sum);
    }

    // Step 5: Calculate quota percentage
    int quotaPercentage = (int) ((totalStorage * 100) / quotaBytes);

    // Step 6: Build the DTO and add to model
    DashboardDto dashboard = DashboardDto.builder()
            .totalFiles(totalFiles)
            .totalFolders(totalFolders)
            .totalStorageBytes(totalStorage)
            .formattedStorage(FormatUtils.formatBytes(totalStorage))
            // ... more fields ...
            .build();

    model.addAttribute("dashboard", dashboard);
    model.addAttribute("username", user.getUsername());
    return "dashboard";  // → templates/dashboard.html
}
```

### `simplifyFileType()` — MIME Type Simplification
```java
private String simplifyFileType(String mimeType) {
    if (mimeType.startsWith("image/")) return "Images";
    if (mimeType.equals("application/pdf")) return "PDF";
    if (mimeType.startsWith("video/")) return "Videos";
    if (mimeType.startsWith("audio/")) return "Audio";
    if (mimeType.contains("zip") || mimeType.contains("rar")) return "Archives";
    if (mimeType.contains("word") || mimeType.contains("document")) return "Documents";
    if (mimeType.contains("sheet") || mimeType.contains("excel")) return "Spreadsheets";
    if (mimeType.startsWith("text/")) return "Text";
    return "Other";
}
```

This turns verbose MIME types into readable labels for the dashboard chart. The `merge()` call in the loop aggregates multiple MIME types (e.g., `image/png` + `image/jpeg` both become "Images").

### Additional Endpoints (Enterprise Features)
The controller also serves several infrastructure visualization pages:

| Endpoint | Template | Purpose |
|----------|----------|---------|
| `GET /nodes` | `nodes.html` | Storage node topology with per-node capacity |
| `GET /deduplication` | `deduplication.html` | SHA-256 dedup center |
| `GET /replication` | `replication.html` | Cross-node replication animation |
| `GET /network` | `network.html` | Real-time network activity |
| `GET /analytics` | `analytics.html` | Storage analytics |
| `GET /monitoring` | `monitoring.html` | JVM memory health + dedup stats |

### How Senior Engineers Would Improve This
1. **Extract to `DashboardService`** — move all query logic out of the controller
2. **Cache with 5-minute TTL** — `@Cacheable("dashboard-stats")` to avoid 6+ queries per page load
3. **Use a single aggregation query** — instead of 6 separate queries, use one JPQL query with projections

---

## 4.15 SharedLinkService.java — Token-Based Sharing Engine

**File**: [SharedLinkService.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/service/SharedLinkService.java) (91 lines — clean, focused service)

### Class Structure
```java
@Service
@Transactional  // All methods run inside a database transaction
public class SharedLinkService {

    private final SharedLinkRepository sharedLinkRepository;

    // Constructor injection — Spring provides the repository proxy
    public SharedLinkService(SharedLinkRepository sharedLinkRepository) {
        this.sharedLinkRepository = sharedLinkRepository;
    }
```

### `generateShareLink()` — Creating a Shareable URL
```java
public String generateShareLink(FileEntity file, User user) {
    // Step 1: Generate a cryptographically random UUID
    String token = UUID.randomUUID().toString();
    // → "a1b2c3d4-e5f6-7890-abcd-ef1234567890"

    // Step 2: Build the SharedLink entity with 7-day expiry
    SharedLink sharedLink = SharedLink.builder()
            .token(token)           // The URL identifier
            .file(file)             // The file being shared
            .createdBy(user)        // Who created the link
            .expiresAt(LocalDateTime.now().plusDays(7))  // 7-day lifespan
            .build();

    // Step 3: Save to database and return the token
    sharedLinkRepository.save(sharedLink);
    return token;  // Controller uses this to build: /share/{token}
}
```

**Why UUID?** UUIDs are 128-bit random values. The probability of collision is 1 in 2^122, which is effectively zero. They're unguessable — an attacker can't brute-force valid tokens.

### `resolveShareLink()` — Accessing a Shared File
```java
public SharedLink resolveShareLink(String token) {
    // Find the link by token — 404 if not found
    SharedLink link = sharedLinkRepository.findByToken(token)
            .orElseThrow(() -> new FileNotFoundException("Share link not found or invalid"));

    // Check expiration — reject if past the deadline
    if (link.isExpired()) {
        throw new FileNotFoundException("This share link has expired");
    }

    return link;  // Controller extracts .getFile() to serve the download
}
```

### `deleteLinksForFile()` — Cleanup on File Deletion
```java
public void deleteLinksForFile(Long fileId) {
    sharedLinkRepository.deleteByFileId(fileId);
    // Called by FileStorageService.permanentDeleteFile()
    // Ensures no orphan share links point to deleted files
}
```

### Why This Design Is Clean
- **Single Responsibility**: Only handles share link CRUD — doesn't know about file storage, security, or HTTP
- **Transaction Boundary**: `@Transactional` ensures token generation + database save are atomic
- **Separation from Controller**: `ShareController` handles HTTP concerns; this service handles business logic

---

## 4.16 TrashCleanupScheduler.java — Automated Background Janitor

**File**: [TrashCleanupScheduler.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/service/TrashCleanupScheduler.java) (54 lines — the smallest service)

### Full Code with Line-by-Line Explanation
```java
@Component  // Detected by Spring's component scan (not @Service — it's a background task, not business logic)
public class TrashCleanupScheduler {

    // SLF4J logger — proper logging (not System.out.println!)
    private static final Logger log = LoggerFactory.getLogger(TrashCleanupScheduler.class);
    private static final int RETENTION_DAYS = 30;  // How long trashed items survive

    private final FileRepository fileRepository;
    private final FileStorageService fileStorageService;

    // Constructor injection
    public TrashCleanupScheduler(FileRepository fileRepository,
                                  FileStorageService fileStorageService) {
        this.fileRepository = fileRepository;
        this.fileStorageService = fileStorageService;
    }

    @Scheduled(cron = "0 0 2 * * *")  // Runs at 02:00:00 every day
    @Transactional                     // Entire method is one database transaction
    public void purgeExpiredTrashItems() {
        // Calculate the cutoff: anything deleted before this timestamp gets purged
        LocalDateTime cutoff = LocalDateTime.now().minusDays(RETENTION_DAYS);

        // Find all soft-deleted files older than 30 days
        List<FileEntity> expired = fileRepository
            .findByIsDeletedTrueAndDeletedAtBefore(cutoff);

        // Early return if nothing to do
        if (expired.isEmpty()) {
            log.info("Trash cleanup: no expired items found.");
            return;
        }

        // Purge each expired file
        log.info("Trash cleanup: purging {} items older than {} days.",
                 expired.size(), RETENTION_DAYS);

        for (FileEntity file : expired) {
            try {
                fileStorageService.permanentDeleteFileAdmin(file.getId());
                // ↑ This handles: dedup check → disk deletion → shared link cleanup → DB delete
            } catch (Exception e) {
                // DON'T let one failure stop the entire cleanup
                log.error("Failed to auto-purge file id={}: {}", file.getId(), e.getMessage());
            }
        }
    }
}
```

### Key Design Decisions
1. **`@Component` not `@Service`** — Semantically, this is a scheduled task, not a business service
2. **Individual try-catch in the loop** — If file #3 fails to delete, files #4-100 still get processed
3. **Uses `permanentDeleteFileAdmin()`** — The admin variant doesn't check user ownership (the scheduler acts as the system)
4. **SLF4J logging** — Proper logging with `{}` placeholders (not string concatenation)

### How `@Scheduled` Works Internally
1. `CloudNestApplication` has `@EnableScheduling` → Spring creates a `TaskScheduler` bean
2. At startup, Spring scans for `@Scheduled` methods
3. Creates a `ScheduledThreadPoolExecutor` (default: 1 thread)
4. Registers the cron trigger: "fire at 02:00:00 every day"
5. When triggered, Spring calls `purgeExpiredTrashItems()` on the scheduler thread

### Production Concern ⚠️
With the default single-thread pool, if the cleanup takes 30 minutes and another scheduled task is due, it will be delayed. Configure multiple threads:
```properties
spring.task.scheduling.pool.size=3
```

---

## 4.17 schema.sql — The Database Blueprint

**File**: [schema.sql](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/schema.sql) (73 lines)

This file is the **PostgreSQL reference schema**. With `ddl-auto=update`, Hibernate creates tables automatically, but this script documents the intended schema and can be used for CI/CD or manual database setup.

### Line-by-Line Walkthrough

```sql
-- Step 1: Users table
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,      -- Auto-increment 64-bit integer
    username    VARCHAR(50)  NOT NULL UNIQUE,-- Login identifier, must be unique
    email       VARCHAR(100) NOT NULL UNIQUE,-- Contact email, must be unique
    password    VARCHAR(255) NOT NULL,       -- BCrypt hash (60 chars, but 255 for safety)
    role        VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER', -- ROLE_USER or ROLE_ADMIN
    version     BIGINT       DEFAULT 0,     -- Optimistic locking counter
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP -- Auto-set on INSERT
);
```

**Why `BIGSERIAL`?** Equivalent to `BIGINT NOT NULL DEFAULT nextval('users_id_seq')`. Auto-creates a sequence. Supports up to 9.2 × 10^18 values.

**Why `VARCHAR(255)` for password?** BCrypt hashes are exactly 60 characters (`$2a$12$...`). Using 255 provides headroom for future algorithms (Argon2 hashes can be longer).

```sql
-- Step 2: Folders table (self-referencing for nested folders)
CREATE TABLE IF NOT EXISTS folders (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    parent_id   BIGINT REFERENCES folders(id) ON DELETE CASCADE,
    is_deleted  BOOLEAN DEFAULT FALSE,
    deleted_at  TIMESTAMP,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Key insight**: `parent_id REFERENCES folders(id)` is a **self-referencing foreign key**. A folder can be inside another folder. When `parent_id IS NULL`, the folder is at root level.

**`ON DELETE CASCADE`** on `parent_id`: deleting a parent folder hard-deletes all sub-folders. But in practice, we use soft-delete in the application, so this cascade is a safety net.

```sql
-- Step 3: Files table
CREATE TABLE IF NOT EXISTS files (
    id            BIGSERIAL PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,       -- User's filename ("report.pdf")
    stored_name   VARCHAR(255) NOT NULL,       -- UUID on disk ("a1b2c3.pdf")
    file_type     VARCHAR(100),                -- MIME type ("application/pdf")
    file_size     BIGINT,                      -- Size in bytes
    storage_node  VARCHAR(20),                 -- "node1", "node2", or "node3"
    file_hash     VARCHAR(64),                 -- SHA-256 hex digest for dedup
    is_deleted    BOOLEAN DEFAULT FALSE,       -- Soft delete flag
    deleted_at    TIMESTAMP,                   -- When moved to trash
    version       BIGINT  DEFAULT 0,           -- Optimistic locking
    user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    folder_id     BIGINT REFERENCES folders(id) ON DELETE SET NULL,
    uploaded_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**`ON DELETE SET NULL`** on `folder_id`: when a folder is hard-deleted, files move to root level (folder_id becomes NULL) instead of being deleted. This preserves user files even if their folder is removed.

```sql
-- Step 4: Shared links table
CREATE TABLE IF NOT EXISTS shared_links (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,  -- UUID share token (used in URL)
    file_id     BIGINT NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    created_by  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at  TIMESTAMP,                     -- NULL = never expires
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**`ON DELETE CASCADE`** on `file_id`: when a file is hard-deleted, all its share links are automatically removed. No orphan links.

```sql
-- Performance Indexes
CREATE INDEX IF NOT EXISTS idx_files_user_id     ON files(user_id);
CREATE INDEX IF NOT EXISTS idx_files_folder_id   ON files(folder_id);
CREATE INDEX IF NOT EXISTS idx_files_file_hash   ON files(file_hash);
CREATE INDEX IF NOT EXISTS idx_files_is_deleted  ON files(is_deleted);
CREATE INDEX IF NOT EXISTS idx_folders_user_id   ON folders(user_id);
CREATE INDEX IF NOT EXISTS idx_folders_parent_id ON folders(parent_id);
CREATE INDEX IF NOT EXISTS idx_shared_links_file_id ON shared_links(file_id);
CREATE INDEX IF NOT EXISTS idx_shared_links_token   ON shared_links(token);
```

**Why `IF NOT EXISTS`?** Safe to run multiple times (idempotent). Won't fail if the index already exists.

---

## 4.18 pom.xml — The Dependency Bible

**File**: [pom.xml](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/pom.xml) (155 lines)

### Project Coordinates
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.4.5</version>   <!-- Spring Boot version — controls ALL Spring dependency versions -->
</parent>

<groupId>com.cloudnest</groupId>   <!-- Your organization/project namespace -->
<artifactId>cloudnest</artifactId> <!-- Project name (used in JAR filename) -->
<version>1.0.0</version>           <!-- Your project's version -->
```

**Why the `<parent>` block?** `spring-boot-starter-parent` is a special POM that defines compatible versions for hundreds of Spring libraries. You don't specify versions for individual dependencies — the parent manages them all. This prevents version conflicts.

### Every Dependency Explained

| Dependency | Purpose | Scope | What It Provides |
|-----------|---------|-------|-----------------|
| `spring-boot-starter-actuator` | Health monitoring | compile | `/actuator/health`, `/actuator/info` endpoints |
| `spring-boot-starter-web` | Web framework | compile | Embedded Tomcat, Spring MVC, JSON serialization |
| `spring-boot-starter-thymeleaf` | Template engine | compile | Server-side HTML rendering with `th:` attributes |
| `thymeleaf-extras-springsecurity6` | Security tags | compile | `sec:authorize` in templates for role-based UI |
| `spring-boot-starter-security` | Security framework | compile | Authentication, authorization, CSRF, sessions |
| `spring-boot-starter-data-jpa` | Database access | compile | Spring Data JPA, Hibernate ORM, EntityManager |
| `spring-boot-starter-validation` | Input validation | compile | `@NotBlank`, `@Email`, `@Size` annotations |
| `postgresql` | Database driver | runtime | JDBC driver for PostgreSQL (not needed at compile time) |
| `lombok` | Boilerplate reducer | optional | `@Getter`, `@Setter`, `@Builder`, `@Data` at compile time |
| `spring-boot-devtools` | Hot reload | runtime | Auto-restart on code changes during development |
| `spring-boot-starter-test` | Testing | test | JUnit 5, Mockito, AssertJ, TestRestTemplate |
| `spring-security-test` | Security testing | test | `@WithMockUser`, `SecurityMockMvcRequestPostProcessors` |
| `h2` | Test database | test | In-memory database for integration tests |

### Build Plugins
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-maven-plugin</artifactId>
            <configuration>
                <excludes>
                    <exclude>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                    </exclude>
                </excludes>
            </configuration>
        </plugin>
    </plugins>
</build>
```

**What this does**: Packages the app into an executable JAR with an embedded Tomcat server. The Lombok exclusion prevents Lombok (a compile-time-only tool) from being packaged into the production JAR.

### Dependency Versions — Where Do They Come From?
None of the dependencies above specify a `<version>`. They're all managed by `spring-boot-starter-parent:3.4.5`, which defines:
- Spring Framework: 6.2.x
- Hibernate: 6.6.x
- Tomcat: 10.1.x
- Thymeleaf: 3.1.x
- JUnit: 5.11.x
- Lombok: 1.18.x
- PostgreSQL driver: 42.7.x

This is called **dependency management** — the parent POM acts as a bill of materials (BOM) that guarantees all versions are compatible.

---

> **📖 Continue to Part 3** → `Learning_Guide_Part3_Request_Flows_and_Database.md`
