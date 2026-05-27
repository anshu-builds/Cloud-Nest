# CloudNest — Production Audit Report

> **Audited By:** Anshu Jaiswal  
> **Date:** 2026-05-26  
> **Project:** CloudNest — Mini Google Drive Clone  
> **Stack:** Java 21 / Spring Boot 3.4.5 / PostgreSQL / Thymeleaf / Lucide + GSAP (frontend)  
> **Context:** Codebase was recently modified by an AI assistant (Gemini Flash 1.5). This audit identifies regressions, bugs, and improvement opportunities.

---

# Bug Report

## BUG-01 — Hardcoded Database Password in Source Control ⛔
| Property | Value |
|---|---|
| **Type** | Security / Data Error |
| **Severity** | 🔴 **Critical** |
| **Layer** | Configuration |
| **File** | [application.properties](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/resources/application.properties#L16) |

### Root Cause
The database password `#nanshu@229` is hardcoded in plain text in `application.properties` line 16. This file is checked into version control, meaning anyone with repo access can read the production DB password.

### Buggy Code
```properties
# BUG: Password is hardcoded in plain text — exposed in version control
spring.datasource.password=#nanshu@229
```

### Fixed Code
```properties
# FIX: Use environment variable — never commit secrets
spring.datasource.password=${DB_PASSWORD}
```

### Why It Was Broken
Secrets should never appear in source-tracked files. If this repo is pushed to GitHub (even private), the credential is compromised. This is **OWASP Top 10 #2: Cryptographic Failures**.

### Impact If Unfixed
Full unauthorized access to the production PostgreSQL database. Data breach, exfiltration, or ransomware.

---

## BUG-02 — Schema File Uses MySQL Syntax but App Connects to PostgreSQL
| Property | Value |
|---|---|
| **Type** | Data / Configuration Error |
| **Severity** | 🔴 **Critical** |
| **Layer** | Database |
| **File** | [schema.sql](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/schema.sql) vs [application.properties](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/resources/application.properties#L14) |

### Root Cause
The `schema.sql` uses MySQL-specific syntax (`AUTO_INCREMENT`, `ENGINE=InnoDB`, `USE cloudnest_db`), yet `application.properties` connects to PostgreSQL (`jdbc:postgresql://localhost:5432/cloudnest_db`). If anyone attempts to run `schema.sql` against PostgreSQL, it will **fail completely**.

Additionally, the schema is **out of date** with the JPA entities — it's missing:
- `role` column on `users` table
- `is_deleted` column on `files` table
- `is_deleted` column on `folders` table
- `file_hash` column on `files` table

### Buggy Code
```sql
-- BUG: MySQL syntax; missing columns for role, is_deleted, file_hash
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,  -- MySQL syntax
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;           -- MySQL-only clause
```

### Fixed Code
```sql
-- FIX: PostgreSQL syntax with all missing columns
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS files (
    id            BIGSERIAL PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    stored_name   VARCHAR(255) NOT NULL,
    file_type     VARCHAR(100),
    file_size     BIGINT,
    storage_node  VARCHAR(20),
    file_hash     VARCHAR(64),
    is_deleted    BOOLEAN DEFAULT FALSE,
    user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    folder_id     BIGINT REFERENCES folders(id) ON DELETE SET NULL,
    uploaded_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Why It Was Broken
This is a likely AI-generated regression. The original project may have targeted MySQL, and the AI migrated to PostgreSQL in `application.properties` and JPA entities but **forgot to update the reference SQL schema**. The entity classes also have columns (`role`, `is_deleted`, `file_hash`) that never existed in the SQL file.

> [!IMPORTANT]
> Since `ddl-auto=update` is used, Hibernate auto-creates the tables, so the app technically runs. But the `schema.sql` is completely misleading documentation.

### Impact If Unfixed
- Manual database setup will fail
- New developers will be confused by the MySQL/PostgreSQL mismatch
- Missing column documentation makes debugging harder

---

## BUG-03 — `AdminController.showAdminDashboard()` Causes N+1 Query + Potential NPE
| Property | Value |
|---|---|
| **Type** | Runtime / Logic Error |
| **Severity** | 🟡 **High** |
| **Layer** | Java Backend |
| **File** | [AdminController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/controller/AdminController.java#L50-L54) |

### Root Cause
Line 50-54: `fileRepository.findAll()` fetches **all files globally**, then iterates them in-memory via Java streams. The `getFileSize()` call can return `null` (the `fileSize` field in `FileEntity` is `Long`, not `long`) which will cause an **unboxing NPE** in `mapToLong(FileEntity::getFileSize)`.

### Buggy Code
```java
// BUG: NPE when fileSize is null; N+1 query problem loading all files into memory
List<FileEntity> allFiles = fileRepository.findAll();
long totalStorageBytes = allFiles.stream()
        .filter(f -> !f.isDeleted())
        .mapToLong(FileEntity::getFileSize)  // NPE if fileSize is null!
        .sum();
```

### Fixed Code
```java
// FIX: Null-safe mapping + use database aggregation instead of loading all files
long totalStorageBytes = allFiles.stream()
        .filter(f -> !f.isDeleted())
        .mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0L)
        .sum();
```

Better fix: create a repository method `@Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileEntity f WHERE f.isDeleted = false")`.

### Why It Was Broken
The AI likely generated the streaming code without considering that `fileSize` is a nullable `Long` wrapper type, not a primitive `long`. Additionally, loading all files into memory doesn't scale.

### Impact If Unfixed
- **NPE crash** on the admin dashboard if any file has a null `fileSize`
- **OutOfMemoryError** with thousands of files (loads entire files table into JVM heap)

---

## BUG-04 — `SharedLink.deleteByFileId()` Missing `@Transactional` Annotation
| Property | Value |
|---|---|
| **Type** | Runtime Error |
| **Severity** | 🟡 **High** |
| **Layer** | Java Backend |
| **File** | [SharedLinkRepository.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/repository/SharedLinkRepository.java#L30) |

### Root Cause
`deleteByFileId(Long fileId)` is a derived delete query in JPA. In Spring Data JPA, **custom delete methods derived from method names require `@Modifying` and `@Transactional`** — otherwise they throw `TransactionRequiredException` at runtime.

### Buggy Code
```java
// BUG: Missing @Modifying annotation; will throw TransactionRequiredException
void deleteByFileId(Long fileId);
```

### Fixed Code
```java
// FIX: Add @Modifying for derived delete operations
@org.springframework.data.jpa.repository.Modifying
void deleteByFileId(Long fileId);
```

### Why It Was Broken
The calling service (`SharedLinkService`) has `@Transactional` on the class, but the `deleteByFileId` method in the repository still requires `@Modifying` to signal Spring Data that it's a write operation. Without it, Spring Data may try to execute it as a query and fail.

### Impact If Unfixed
Sharing cleanup silently fails or throws a runtime exception when a file with shared links is deleted.

---

## BUG-05 — Folder Soft Delete Does NOT Cascade to Child Files
| Property | Value |
|---|---|
| **Type** | Logic Error |
| **Severity** | 🟡 **High** |
| **Layer** | Java Backend |
| **File** | [FolderService.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/service/FolderService.java#L89-L101) |

### Root Cause
When a folder is soft-deleted (`isDeleted = true`), only the folder entity is marked. Files inside the folder remain with `isDeleted = false`. This means:
1. Search results (`searchByName`) will still return files from "deleted" folders
2. Dashboard file counts will include files from deleted folders
3. Storage quota calculations will count those files

### Buggy Code
```java
// BUG: Only marks folder as deleted — files inside remain active and searchable
public void deleteFolder(Long folderId, User user) {
    Folder folder = folderRepository.findByIdAndUserAndIsDeletedFalse(folderId, user)
            .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
    folder.setDeleted(true);
    // The comment in the code itself acknowledges this design gap!
    folderRepository.save(folder);
}
```

### Fixed Code
```java
// FIX: Cascade soft delete to all files and subfolders recursively
public void deleteFolder(Long folderId, User user) {
    Folder folder = folderRepository.findByIdAndUserAndIsDeletedFalse(folderId, user)
            .orElseThrow(() -> new IllegalArgumentException("Folder not found"));
    softDeleteRecursively(folder);
    folderRepository.save(folder);
}

private void softDeleteRecursively(Folder folder) {
    folder.setDeleted(true);
    if (folder.getFiles() != null) {
        folder.getFiles().forEach(f -> f.setDeleted(true));
    }
    if (folder.getSubFolders() != null) {
        folder.getSubFolders().forEach(this::softDeleteRecursively);
    }
}
```

### Impact If Unfixed
"Deleted" folder's files remain visible in search, count toward quota, and appear in dashboard stats — a broken user experience and misleading data.

---

## BUG-06 — `System.out.println` Used for Deduplication Logging
| Property | Value |
|---|---|
| **Type** | Logic / Standards Error |
| **Severity** | 🟠 **Medium** |
| **Layer** | Java Backend |
| **File** | [FileStorageService.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/service/FileStorageService.java#L135) |

### Root Cause
Line 135 uses `System.out.println("🔥 DEDUPLICATION TRIGGERED!...")` instead of SLF4J logging. This bypasses the logging framework configuration, can't be filtered by log levels, and won't appear in structured log sinks.

### Buggy Code
```java
// BUG: System.out.println bypasses logging framework
System.out.println("🔥 DEDUPLICATION TRIGGERED! Reused existing file for hash: " + fileHash);
```

### Fixed Code
```java
// FIX: Use SLF4J logger
private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(FileStorageService.class);
// ...
log.info("Deduplication triggered — reused existing file for hash: {}", fileHash);
```

---

## BUG-07 — IDOR Vulnerability on Shared File Downloads
| Property | Value |
|---|---|
| **Type** | Security Error |
| **Severity** | 🟡 **High** |
| **Layer** | API Boundary |
| **File** | [ShareController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/controller/ShareController.java#L99-L118) |

### Root Cause
`downloadSharedFile()` resolves the token and constructs a `Path` from user-controlled data (`file.getStorageNode()`, `file.getStoredName()`). While the values come from the DB, there is no verification that the physical file exists before streaming it. Additionally, the shared download doesn't check if the file was soft-deleted (`isDeleted = true`).

### Buggy Code
```java
// BUG: No check if file is soft-deleted; no existence check on disk
public ResponseEntity<Resource> downloadSharedFile(@PathVariable String token) {
    SharedLink link = sharedLinkService.resolveShareLink(token);
    FileEntity file = link.getFile();
    // Missing: if (file.isDeleted()) throw ...
    // Missing: if (!Files.exists(path)) throw ...
```

### Fixed Code
```java
// FIX: Check soft-delete status and file existence
public ResponseEntity<Resource> downloadSharedFile(@PathVariable String token) {
    SharedLink link = sharedLinkService.resolveShareLink(token);
    FileEntity file = link.getFile();
    
    if (file.isDeleted()) {
        throw new FileNotFoundException("This file has been deleted");
    }
    
    String filePath = storageNodeService.getFilePath(file.getStorageNode(), file.getStoredName());
    Path path = Paths.get(filePath);
    
    if (!Files.exists(path)) {
        throw new FileNotFoundException("Shared file not found on disk");
    }
    // ... proceed with download
```

---

## BUG-08 — `UserRegistrationDto` Allows Anyone to Self-Assign ADMIN Role
| Property | Value |
|---|---|
| **Type** | Security / Logic Error |
| **Severity** | 🔴 **Critical** |
| **Layer** | Java Backend |
| **File** | [UserService.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/service/UserService.java#L73-L77) + [UserRegistrationDto.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/dto/UserRegistrationDto.java#L39) |

### Root Cause
`UserRegistrationDto` has a `role` field. In `UserService.registerUser()`, if `dto.getRole()` equals `"ADMIN"`, the user gets `ROLE_ADMIN`. Since the registration form is public, any anonymous user can POST with `role=ADMIN` and **create an admin account**, bypassing all authorization.

### Buggy Code
```java
// BUG: User-controlled role assignment — privilege escalation!
String finalRole = "ROLE_USER";
if (dto.getRole() != null && dto.getRole().equalsIgnoreCase("ADMIN")) {
    finalRole = "ROLE_ADMIN";
}
```

### Fixed Code
```java
// FIX: Ignore the role field from user input — only assign ROLE_USER on public registration
String finalRole = "ROLE_USER";
// Admin role should only be assignable via admin panel toggle, never via registration
```

### Why It Was Broken
The AI likely added the `role` field for flexibility without considering that the registration endpoint is publicly accessible. This is a **critical privilege escalation vulnerability**.

### Impact If Unfixed
Any anonymous user can create an admin account and gain full administrative access to the system (manage users, delete any file globally, etc.).

---

## BUG-09 — `GlobalExceptionHandler` Swallows All Exceptions Silently
| Property | Value |
|---|---|
| **Type** | Logic Error |
| **Severity** | 🟠 **Medium** |
| **Layer** | Java Backend |
| **File** | [GlobalExceptionHandler.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/exception/GlobalExceptionHandler.java#L44-L48) |

### Root Cause
The catch-all `@ExceptionHandler(Exception.class)` on line 44 catches *every* exception (including `NullPointerException`, `StackOverflowError`, etc.) and redirects to `/dashboard` with a generic message — **without logging the actual exception**. This makes debugging production issues nearly impossible.

### Buggy Code
```java
// BUG: Swallows exception without logging — impossible to debug in production
@ExceptionHandler(Exception.class)
public String handleGenericException(Exception ex, RedirectAttributes redirectAttributes) {
    redirectAttributes.addFlashAttribute("error", "An unexpected error occurred. Please try again.");
    return "redirect:/dashboard";
}
```

### Fixed Code
```java
// FIX: Log the full exception before redirecting
private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

@ExceptionHandler(Exception.class)
public String handleGenericException(Exception ex, RedirectAttributes redirectAttributes) {
    log.error("Unhandled exception caught by GlobalExceptionHandler", ex);
    redirectAttributes.addFlashAttribute("error", "An unexpected error occurred. Please try again.");
    return "redirect:/dashboard";
}
```

---

## BUG-10 — `moveFolder()` Doesn't Prevent Moving Folder into Its Own Descendant
| Property | Value |
|---|---|
| **Type** | Logic Error |
| **Severity** | 🟠 **Medium** |
| **Layer** | Java Backend |
| **File** | [FolderService.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/service/FolderService.java#L120-L136) |

### Root Cause
The code checks `folderId.equals(targetFolderId)` to prevent a folder from being moved into itself, but it does NOT check if `targetFolderId` is a **descendant** of `folderId`. Moving a folder into its own child creates a **circular reference** that breaks navigation and can cause infinite loops in breadcrumb generation.

### Buggy Code
```java
// BUG: Only checks direct self-move, not descendant moves (circular reference risk)
if (targetFolderId != null && folderId.equals(targetFolderId)) {
    throw new IllegalArgumentException("Cannot move a folder into itself");
}
```

### Fixed Code
```java
// FIX: Walk up from target to root, checking for cycles
if (targetFolderId != null) {
    if (folderId.equals(targetFolderId)) {
        throw new IllegalArgumentException("Cannot move a folder into itself");
    }
    // Prevent circular reference: walk up from target to check if source is an ancestor
    Folder ancestor = targetFolder;
    while (ancestor != null) {
        if (ancestor.getId().equals(folderId)) {
            throw new IllegalArgumentException("Cannot move a folder into its own descendant");
        }
        ancestor = ancestor.getParent();
    }
}
```

---

# Test Results

## Java Backend Edge Cases

| Test Category | Result | Notes |
|---|---|---|
| Null inputs / empty collections | ⚠️ **PARTIAL FAIL** | `AdminController.getFileSize()` NPE on null; `FolderService.createFolder` handles null name ✅ |
| Integer overflow / floating point | ✅ PASS | `formatBytes()` handles all ranges including >1GB correctly |
| Concurrent requests (race conditions) | ⚠️ **UNTESTED** | No optimistic locking (`@Version`) on any entity — concurrent updates can silently overwrite |
| Large payloads (>10MB) | ✅ PASS | Configured at 50MB max upload in `application.properties` |
| Invalid enum values / malformed JSON | ✅ PASS | App uses Thymeleaf forms (not JSON), so Spring MVC binding handles this |
| Transaction rollback on partial failure | ✅ PASS | `@Transactional` on all service classes |
| Exception propagation | ⚠️ **FAIL** | `GlobalExceptionHandler` swallows exceptions without logging (BUG-09) |

## PostgreSQL Edge Cases

| Test Category | Result | Notes |
|---|---|---|
| NULL vs empty string | ⚠️ **PARTIAL FAIL** | `fileSize` can be `null`, causing NPE in admin dashboard (BUG-03) |
| Timezone handling | ✅ PASS | Uses `LocalDateTime` + `@CreationTimestamp`; PostgreSQL stores as UTC |
| LIKE queries with special characters | ⚠️ **FAIL** | Search queries use `LIKE CONCAT('%', :keyword, '%')` — special characters `%`, `_` are not escaped |
| Concurrent writes (deadlocks) | ⚠️ **UNTESTED** | No `@Version` / optimistic locking |
| Missing indexes on FKs | ⚠️ **FAIL** | No explicit indexes; PostgreSQL auto-indexes PKs but NOT FK columns |
| N+1 query problem | ⚠️ **FAIL** | `AdminController.findAll()` loads all files + user associations lazily (N+1) |
| Connection pool exhaustion | ✅ PASS | HikariCP (Spring Boot default) with default pool size |

## Frontend / JavaScript Edge Cases

| Test Category | Result | Notes |
|---|---|---|
| XSS in Toast messages | ✅ PASS | `escapeHtml()` function is used in Toast system |
| Mobile/touch support | ✅ PASS | Mobile menu, responsive breakpoints, magnetic buttons disabled on mobile |
| Memory leaks | ✅ PASS | No infinite loops; `IntersectionObserver` properly unobserves after animation |
| WebGL / Three.js | N/A | **No Three.js is used in this project** despite the user's prompt |
| Node.js middleware | N/A | **No Node.js is used** — this is a pure Spring Boot + Thymeleaf app |
| `lucide@latest` CDN version pinning | ⚠️ **FAIL** | Using `@latest` in production CDN URLs is risky — a breaking update could crash icons |

## API / Integration Edge Cases

| Test Category | Result | Notes |
|---|---|---|
| Auth token expired/missing | ✅ PASS | Spring Security redirects to `/login` for unauthenticated requests |
| Rate limiting | ⚠️ **FAIL** | No rate limiting on any endpoint (login, registration, upload, share) |
| CSRF protection | ✅ PASS | Spring Security CSRF is enabled; all forms include CSRF tokens |
| File download without existence check | ⚠️ **FAIL** | Shared file download doesn't verify physical file exists (BUG-07) |

## Security Edge Cases

| Test Category | Result | Notes |
|---|---|---|
| SQL injection | ✅ PASS | All queries use parameterized JPA (`@Query` with `:param`), no string concat |
| XSS via user content | ✅ PASS | Thymeleaf auto-escapes `th:text`; Toast uses `escapeHtml()` |
| CORS misconfiguration | ✅ PASS | No CORS config = Spring Security blocks cross-origin by default |
| IDOR on DB queries | ⚠️ **PARTIAL FAIL** | File/folder operations check ownership ✅, but shared links can access soft-deleted files (BUG-07) |
| Secrets in logs/errors | ⚠️ **FAIL** | DB password hardcoded (BUG-01); exception messages sometimes leak internal paths |
| Privilege escalation | ⚠️ **FAIL** | Registration allows self-assigning ADMIN role (BUG-08) |

---

# Standards Violations

## Violation 1 — No Database Migration Tool
| Standard | Status |
|---|---|
| **DB migrations managed (Flyway/Liquibase)** | ❌ Violated |
| **File** | [application.properties](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/resources/application.properties#L22) |

`ddl-auto=update` is used, which auto-mutates the production schema without any version tracking. If a column is renamed, Hibernate will **create a new column and leave orphaned data** in the old one. Industry standard is Flyway or Liquibase with `ddl-auto=validate`.

## Violation 2 — Controllers Contain Business Logic
| Standard | Status |
|---|---|
| **Controllers should be thin** | ❌ Violated |
| **File** | [AdminController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/controller/AdminController.java#L50-L59), [DashboardController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/controller/DashboardController.java#L62-L102) |

`AdminController.showAdminDashboard()` performs raw `fileRepository.findAll()` calls and Java stream processing directly inside the controller. `DashboardController` similarly builds DTOs in-place. This logic belongs in a dedicated service layer (e.g., `AdminService`, `DashboardService`).

## Violation 3 — Repository Injected Directly into Controller
| Standard | Status |
|---|---|
| **Service layer properly separated from repository** | ❌ Violated |
| **File** | [AdminController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/controller/AdminController.java#L29-L31), [DashboardController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/controller/DashboardController.java#L36) |

Both controllers inject `FileRepository` and `UserRepository` directly, bypassing the service layer. All data access should flow through services for consistency, transaction management, and testability.

## Violation 4 — Admin Template Directly Accesses Entity Lazy Fields
| Standard | Status |
|---|---|
| **DTOs used at API boundaries (not exposing raw entities)** | ❌ Violated |
| **File** | [admin.html](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/resources/templates/admin.html#L212) |

Line 212: `${fileItem.user.username}` accesses the lazy-loaded `User` entity from `FileEntity`. This can trigger a `LazyInitializationException` if the Hibernate session is closed, and it exposes the raw entity to the view layer.

## Violation 5 — Logging Uses `System.out.println`
| Standard | Status |
|---|---|
| **Structured logging (no System.out)** | ❌ Violated |
| **File** | [FileStorageService.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/service/FileStorageService.java#L135) |

See BUG-06.

## Violation 6 — No API Versioning
| Standard | Status |
|---|---|
| **API versioning (/api/v1/...)** | ❌ Violated |

All endpoints use flat paths (`/files`, `/dashboard`, `/admin`). While acceptable for a Thymeleaf-rendered app, any future API endpoints should be versioned.

## Violation 7 — No Health Check Endpoint
| Standard | Status |
|---|---|
| **/health, /ready endpoints** | ❌ Violated |

No Spring Boot Actuator dependency. No `/health` or `/ready` endpoint exists. Required for production deployment behind a load balancer or in Kubernetes.

## Violation 8 — No Request Correlation IDs / Structured Logging
| Standard | Status |
|---|---|
| **Request logging with correlation IDs** | ❌ Violated |

No MDC-based correlation IDs are set. Logs cannot be traced across request boundaries.

## Violation 9 — `ddl-auto=update` in Main Config
| Standard | Status |
|---|---|
| **Environment configs externalized** | ⚠️ Partially Violated |
| **File** | [application.properties](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/resources/application.properties#L22-L23) |

`spring.jpa.show-sql=true` and `ddl-auto=update` are development settings baked into the main config. These should be profiled (`application-dev.properties` vs `application-prod.properties`).

## Violation 10 — CDN Dependencies Without Version Pinning
| Standard | Status |
|---|---|
| **Dependencies up to date / pinned** | ❌ Violated |
| **File** | [header.html](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/resources/templates/fragments/header.html#L24) |

```html
<!-- BUG: @latest can break at any time -->
<script src="https://unpkg.com/lucide@latest/dist/umd/lucide.min.js"></script>
```

Should pin to a specific version: `lucide@0.325.0`.

## Violation 11 — No Input Validation on Folder Names for Path Traversal
| Standard | Status |
|---|---|
| **Input validation at API layer** | ❌ Violated |
| **File** | [FolderService.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/service/FolderService.java#L43-L47) |

Folder names are only checked for emptiness, not for dangerous characters (`..`, `/`, `\`, null bytes). While folder names are stored in the DB (not used for filesystem paths directly), the ZIP download feature at line 166 **uses the folder name directly in ZIP entry paths**, creating a potential path traversal in ZIP files (Zip Slip vulnerability).

## Violation 12 — Entity Comments Reference MySQL
| Standard | Status |
|---|---|
| **Documentation accuracy** | ❌ Violated |
| **Files** | [User.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/entity/User.java#L16), [FileEntity.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/entity/FileEntity.java#L14), [Folder.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/entity/Folder.java#L17) |

Entity Javadocs say "Maps to the \"users\" table in **MySQL**" — but the app uses PostgreSQL. The AI migrated the database driver without updating documentation.

## Violation 13 — `formatBytes()` Duplicated in 3 Classes
| Standard | Status |
|---|---|
| **DRY (Don't Repeat Yourself)** | ❌ Violated |
| **Files** | [DashboardController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/controller/DashboardController.java#L124-L129), [AdminController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/controller/AdminController.java#L125-L130), [ShareController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/controller/ShareController.java#L120-L125) |

The `formatBytes()` method is copy-pasted in three controllers. Should be extracted to a shared utility class.

## Violation 14 — No `@Version` for Optimistic Locking
| Standard | Status |
|---|---|
| **Concurrent write safety** | ❌ Violated |

None of the entities use `@Version`. Concurrent updates (e.g., two admins toggling the same user's role) will silently overwrite each other (last-write-wins).

## Violation 15 — Exception Classes Shadow JDK Names
| Standard | Status |
|---|---|
| **Clean naming** | ⚠️ Warning |
| **File** | [FileNotFoundException.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Java%20Project/src/main/java/com/cloudnest/exception/FileNotFoundException.java) |

`com.cloudnest.exception.FileNotFoundException` shadows `java.io.FileNotFoundException`. This forces fully-qualified class names in any file that imports both, and it confuses IDE auto-imports.

---

# Feature Recommendations

## Features to ADD

| # | Feature | Why Needed | Standard | Complexity | Approach |
|---|---|---|---|---|---|
| 1 | **Rate Limiting** | Prevent brute-force login, upload spam | OWASP | Low | Spring Boot `bucket4j-spring-boot-starter` or filter-based rate limiter |
| 2 | **Spring Boot Actuator** | Health checks, metrics, production monitoring | 12-Factor App | Low | Add `spring-boot-starter-actuator` dependency, expose `/health` and `/info` |
| 3 | **Flyway DB Migrations** | Reproducible schema changes, version tracking | Industry Standard | Medium | Add `flyway-core` dependency, create `V1__init.sql` from current schema |
| 4 | **API Documentation (OpenAPI)** | Self-documenting endpoints for future API consumers | REST standard | Low | Add `springdoc-openapi-starter-webmvc-ui` |
| 5 | **Password Strength Indicator** | Better UX and security on registration | UX Standard | Low | JavaScript client-side + backend `@Pattern` validation |
| 6 | **Account Lockout After Failed Logins** | Brute-force protection | OWASP | Medium | Custom `AuthenticationFailureHandler` with failure counter |
| 7 | **Email Verification on Registration** | Prevent fake/spam accounts | Industry Standard | Medium | JavaMailSender + verification token table |
| 8 | **Audit Trail / Event Log** | Track who did what and when | Compliance (SOC2, GDPR) | Medium | JPA `@EntityListeners` or Spring AOP for logging operations |
| 9 | **Configurable Share Link Expiry** | Users want control over link duration | UX Enhancement | Low | Add expiry duration dropdown to share form |
| 10 | **Bulk File Operations** | Select multiple files, delete/move/download | UX Enhancement | Medium | JavaScript multi-select + batch endpoint |
| 11 | **File Versioning** | Recover previous versions of a file | Google Drive feature | High | New `FileVersion` entity + version chain |
| 12 | **Caching Layer (Redis or Caffeine)** | Speed up dashboard/admin statistics | Performance Standard | Medium | Spring Cache + `@Cacheable` on repository aggregate queries |

## Features to ENHANCE

| # | Current Issue | Industry Approach | Migration Path |
|---|---|---|---|
| 1 | **Search** only searches files by name/type — no folder search | Full-text search with PostgreSQL `tsvector` or Elasticsearch | Add `@Query` with PostgreSQL `ILIKE` on folders; long-term: Elasticsearch |
| 2 | **Trash** has no auto-expiry — deleted files sit forever | Auto-purge after 30 days (like Google Drive) | Scheduled task with `@Scheduled` to purge items older than 30 days |
| 3 | **Deduplication** page is static HTML — no real data | Show actual dedup savings from the database | Create a `DedupService` that queries `countByStoredName > 1` |
| 4 | **Error pages** (404/500) are basic HTML | Custom branded error pages with navigation back | Enhance existing templates to include sidebar shell |
| 5 | **Shared link page** (`shared.html`) is minimal | Show file preview (images inline, PDF viewer) | Add conditional `<img>` or `<iframe>` based on `fileType` |
| 6 | **Password storage** uses BCrypt default rounds (10) | BCrypt with 12+ rounds for 2026 hardware | `new BCryptPasswordEncoder(12)` |

## Features to REMOVE

| # | Feature | Why Remove | Safe Removal Steps | Risk of Keeping |
|---|---|---|---|---|
| 1 | **`role` field in `UserRegistrationDto`** | Critical security vulnerability (BUG-08) — allows privilege escalation | Remove the field from DTO; remove the role-check logic from `registerUser()` | Any anonymous user can become admin |
| 2 | **`System.out.println` in dedup logic** | Pollutes stdout, bypasses logging framework | Replace with SLF4J `log.info()` | Missing log context in production; impossible to filter/search |
| 3 | **`schema.sql` in current form** | Completely wrong dialect (MySQL for a PostgreSQL app), missing columns | Either delete entirely (rely on Flyway/Hibernate) or rewrite for PostgreSQL | Confuses developers; manual setup fails |

## Priority Matrix

```
                        HIGH IMPACT
                            │
         ┌──────────────────┼──────────────────┐
         │                  │                  │
         │  DO IMMEDIATELY  │  PLAN NEXT SPRINT│
         │                  │                  │
         │ • BUG-08 (role   │ • BUG-05 (folder │
         │   escalation)    │   soft-delete     │
         │ • BUG-01 (creds) │   cascade)        │
         │ • BUG-03 (NPE)   │ • Flyway setup   │
         │ • BUG-09 (logging)│ • Actuator       │
         │ • CDN pinning    │ • Rate limiting  │
         │                  │ • Admin service  │
 LOW ────┼──────────────────┼──────────────────┤ HIGH
 EFFORT  │                  │                  │ EFFORT
         │  NICE TO HAVE    │  BACKLOG / DROP  │
         │                  │                  │
         │ • Fix MySQL refs │ • File versioning│
         │ • Extract        │ • Elasticsearch  │
         │   formatBytes()  │ • Redis caching  │
         │ • Pin Lucide ver │ • Email verify   │
         │ • BCrypt rounds  │                  │
         │                  │                  │
         └──────────────────┼──────────────────┘
                            │
                        LOW IMPACT
```

---

# Priority Action Plan

> **Top 5 things to fix RIGHT NOW, in order:**

### 1. 🔴 Fix Privilege Escalation (BUG-08)
Remove the `role` field from `UserRegistrationDto` and strip the role-selection logic from `UserService.registerUser()`. This is a critical auth bypass — any user can make themselves admin.

### 2. 🔴 Externalize Database Credentials (BUG-01)
Replace the hardcoded password in `application.properties` with `${DB_PASSWORD}` environment variable. Rotate the current password immediately if the repo was ever shared.

### 3. 🟡 Fix Admin Dashboard NPE (BUG-03)
Add null-safe handling for `fileSize` in `AdminController`. Better yet, move the aggregation logic to a repository `@Query` to avoid loading all entities into memory.

### 4. 🟡 Add Exception Logging to GlobalExceptionHandler (BUG-09)
Add `log.error("Unhandled exception", ex)` to the catch-all handler so production errors are actually visible.

### 5. 🟡 Cascade Soft Delete to Folder Contents (BUG-05)
Implement recursive soft-delete for files and subfolders when a folder is deleted, ensuring search results and quota calculations remain accurate.

---

> [!NOTE]
> **No Three.js or Node.js components exist in this project.** Despite the user prompt mentioning these technologies, the codebase is a pure Java/Spring Boot + Thymeleaf server-rendered application with Lucide icons and GSAP animations on the frontend. All Three.js and Node.js edge case categories are therefore N/A.

> [!CAUTION]
> **BUG-08 (Privilege Escalation) is the most dangerous issue.** It is trivially exploitable — a simple `curl` command with `role=ADMIN` in the POST body creates an admin account. Fix this before anything else.
