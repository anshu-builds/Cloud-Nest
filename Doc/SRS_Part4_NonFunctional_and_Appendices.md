# Software Requirements Specification (SRS)

## Project: CloudNest — Enterprise Distributed Cloud Storage System

### Part 4 of 4: Non-Functional Requirements, Testing, Deployment & Appendices

---

## 9. Non-Functional Requirements

> All non-functional requirements are identified with a unique ID in the format `NFR-X.Y`.

---

### 9.1 Security Requirements

#### NFR-1.1 — Password Hashing

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-1.1                                                                                              |
| **Priority**  | Critical                                                                                             |
| **Requirement** | All user passwords **shall** be cryptographically hashed using BCrypt with a strength factor of 12 before storage. Plain-text passwords shall never be stored, logged, or transmitted in responses. |
| **Implementation** | `SecurityConfig.passwordEncoder()` returns `new BCryptPasswordEncoder(12)`. BCrypt automatically generates a unique random salt per password. |
| **Verification** | Inspect `users.password` column — all values start with `$2a$12$` (BCrypt version 2a, 12 rounds). |

#### NFR-1.2 — CSRF Protection

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-1.2                                                                                              |
| **Priority**  | Critical                                                                                             |
| **Requirement** | The system **shall** implement CSRF (Cross-Site Request Forgery) protection on all state-changing endpoints (POST, PUT, DELETE). |
| **Implementation** | Spring Security enables CSRF by default. All Thymeleaf forms include a hidden `_csrf` token rendered by `th:action`. The `SecurityConfig` does **not** disable CSRF. |
| **Session Handling** | `request.getSession(true)` is called in `AuthController` login/register GET methods to force early session creation, ensuring the CSRF token is available before form submission. |

#### NFR-1.3 — SQL Injection Prevention

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-1.3                                                                                              |
| **Priority**  | Critical                                                                                             |
| **Requirement** | All database queries **shall** use parameterized queries to prevent SQL injection attacks.           |
| **Implementation** | All data access uses Spring Data JPA: <br> • Derived query methods (auto-generated parameterized SQL) <br> • `@Query` JPQL with named parameters (`:user`, `:keyword`, `:type`) <br> • Zero raw string concatenation in SQL <br> • No `JdbcTemplate` queries with user input (the only `jdbcTemplate.execute()` is in `DatabaseMigrationConfig` with hardcoded SQL) |

#### NFR-1.4 — Horizontal Privilege Escalation Protection

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-1.4                                                                                              |
| **Priority**  | Critical                                                                                             |
| **Requirement** | Users **shall not** be able to access, download, modify, or delete files or folders belonging to other users. |
| **Implementation** | Every file/folder operation includes an ownership check: <br> • `FileStorageService`: `fileEntity.getUser().getId().equals(user.getId())` — returns generic "File not found" if mismatch (no information leakage about other users' files) <br> • `FolderService`: `findByIdAndUser()` / `findByIdAndUserAndIsDeletedFalse()` — queries include the user as a filter parameter <br> • Exception type is `FileNotFoundException` with a non-descriptive message to prevent IDOR (Insecure Direct Object Reference) enumeration |

#### NFR-1.5 — Admin Self-Demotion Protection

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-1.5                                                                                              |
| **Priority**  | High                                                                                                 |
| **Requirement** | An administrator **shall not** be able to demote their own account to prevent the system from being left with no admin. |
| **Implementation** | `AdminController.toggleUserRole()` checks `currentUser.getId().equals(targetUser.getId())` before role changes. |

#### NFR-1.6 — Blocked File Extensions

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-1.6                                                                                              |
| **Priority**  | High                                                                                                 |
| **Requirement** | The system **shall** reject uploads of potentially executable file types: `.exe`, `.bat`, `.sh`, `.ps1`, `.cmd`. |
| **Implementation** | `FileStorageService` maintains `BLOCKED_EXTENSIONS = Set.of(".exe", ".bat", ".sh", ".ps1", ".cmd")`. Extension is extracted and compared before any disk write. |

#### NFR-1.7 — Path Traversal Prevention

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-1.7                                                                                              |
| **Priority**  | High                                                                                                 |
| **Requirement** | Folder names **shall not** contain path traversal characters (`..`, `/`, `\`, null bytes).           |
| **Implementation** | `FolderService.createFolder()` validates: `name.contains("..") || name.contains("/") || name.contains("\\") || name.contains("\0") || name.length() > 255` → throws `IllegalArgumentException`. |

#### NFR-1.8 — Shared Link Security (Soft-Delete Check)

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-1.8                                                                                              |
| **Priority**  | High                                                                                                 |
| **Requirement** | Shared file links **shall not** allow access to files that have been soft-deleted by their owner.    |
| **Implementation** | **BUG-07 Fix**: `ShareController.viewSharedFile()` and `downloadSharedFile()` both check `file.isDeleted()` after resolving the share link. |

#### NFR-1.9 — Registration Privilege Escalation Prevention

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-1.9                                                                                              |
| **Priority**  | Critical                                                                                             |
| **Requirement** | Public registration **shall never** create administrator accounts regardless of form data manipulation. |
| **Implementation** | `UserService.registerUser()` hardcodes `String finalRole = "ROLE_USER"` — the role field from the DTO (if any) is ignored. |

#### NFR-1.10 — Session Security

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-1.10                                                                                             |
| **Priority**  | High                                                                                                 |
| **Requirement** | On logout, the HTTP session **shall** be invalidated and the `JSESSIONID` cookie **shall** be deleted. |
| **Implementation** | `SecurityConfig`: `.invalidateHttpSession(true).deleteCookies("JSESSIONID")` in the logout configuration. |

---

### 9.2 Performance Requirements

#### NFR-2.1 — Stream-Based ZIP Generation

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-2.1                                                                                              |
| **Priority**  | High                                                                                                 |
| **Requirement** | Folder ZIP downloads **shall** use direct output stream buffering to prevent server RAM exhaustion.  |
| **Implementation** | `FolderService.downloadFolderAsZip()` creates `ZipOutputStream` wrapping `HttpServletResponse.getOutputStream()`. Files are streamed via `InputStream.transferTo(ZipOutputStream)` — never fully loaded into memory. |

#### NFR-2.2 — Concurrent Upload Safety

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-2.2                                                                                              |
| **Priority**  | Medium                                                                                               |
| **Requirement** | The system **should** handle concurrent file uploads without database deadlocks.                     |
| **Implementation** | `@Version` optimistic locking on `FileEntity` and `User` entities prevents dirty writes. `@Transactional` on service methods ensures atomicity. |

#### NFR-2.3 — Database Query Performance

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-2.3                                                                                              |
| **Priority**  | Medium                                                                                               |
| **Requirement** | Frequently queried columns **shall** have database indexes to optimize query performance.            |
| **Implementation** | 8 indexes defined in `schema.sql` on foreign keys (`user_id`, `folder_id`, `file_id`, `parent_id`) and frequently filtered columns (`file_hash`, `is_deleted`, `token`). |

#### NFR-2.4 — Lazy Loading

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-2.4                                                                                              |
| **Priority**  | Medium                                                                                               |
| **Requirement** | Entity relationships **shall** use lazy loading to prevent unnecessary database joins.               |
| **Implementation** | All `@ManyToOne` relationships use `fetch = FetchType.LAZY`. DTOs are used in templates to avoid `LazyInitializationException`. |

#### NFR-2.5 — Upload Size Limit

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-2.5                                                                                              |
| **Priority**  | High                                                                                                 |
| **Requirement** | Individual file uploads **shall** be limited to 50 MB to prevent server memory exhaustion during SHA-256 hashing. |
| **Implementation** | `spring.servlet.multipart.max-file-size=50MB` and `spring.servlet.multipart.max-request-size=50MB` in `application.properties`. |

---

### 9.3 Usability Requirements

#### NFR-3.1 — Responsive Design

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-3.1                                                                                              |
| **Priority**  | High                                                                                                 |
| **Requirement** | The user interface **shall** be fully responsive and functional on both desktop (1920px+) and mobile (320px+) devices. |
| **Implementation** | `design-system.css` (71 KB) contains CSS custom properties, responsive grid layouts, and media query breakpoints. |

#### NFR-3.2 — Dark/Light Theme

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-3.2                                                                                              |
| **Priority**  | Medium                                                                                               |
| **Requirement** | The system **shall** provide a light/dark theme toggle that persists across page navigations.        |
| **Implementation** | CSS custom properties (`--cn-*`) define color tokens for both themes. JavaScript toggle in `app.js` switches a CSS class on `<body>`. |

#### NFR-3.3 — User-Friendly Error Pages

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-3.3                                                                                              |
| **Priority**  | Medium                                                                                               |
| **Requirement** | The system **shall** display custom-branded error pages for 404 and 500 errors instead of exposing server stack traces or default Tomcat error pages. |
| **Implementation** | `templates/error/404.html` and `templates/error/500.html` with Lucide icons, CloudNest branding, and navigation links. |

#### NFR-3.4 — Human-Readable File Sizes

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-3.4                                                                                              |
| **Priority**  | Low                                                                                                  |
| **Requirement** | File sizes **shall** be displayed in human-readable format (B, KB, MB, GB) throughout the application. |
| **Implementation** | `FormatUtils.formatBytes()` and `FileDto.getFormattedSize()` both implement the same logic: `< 1024 → B`, `< 1MB → KB`, `< 1GB → MB`, `≥ 1GB → GB`. |

#### NFR-3.5 — File Type Icons

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-3.5                                                                                              |
| **Priority**  | Low                                                                                                  |
| **Requirement** | Files **shall** display context-appropriate icons based on their MIME type.                          |
| **Implementation** | `FileDto.getFileIconClass()` returns Bootstrap Icon CSS class names based on MIME type matching: `image/*` → `bi-file-earmark-image`, `application/pdf` → `bi-file-earmark-pdf`, `video/*` → `bi-file-earmark-play`, etc. |

#### NFR-3.6 — Flash Messages

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-3.6                                                                                              |
| **Priority**  | Medium                                                                                               |
| **Requirement** | All user actions **shall** provide immediate feedback via success or error flash messages.           |
| **Implementation** | `RedirectAttributes.addFlashAttribute("success", ...)` and `addFlashAttribute("error", ...)` in all controllers. Thymeleaf templates conditionally render toast notifications. |

#### NFR-3.7 — Modern Typography

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-3.7                                                                                              |
| **Priority**  | Low                                                                                                  |
| **Requirement** | The application **shall** use modern web typography instead of browser defaults.                     |
| **Implementation** | Google Fonts: `Inter` (weights 300–900) for body text, `JetBrains Mono` (400–600) for code/monospace elements. |

---

### 9.4 Reliability Requirements

#### NFR-4.1 — Deduplication-Safe Deletion

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-4.1                                                                                              |
| **Priority**  | Critical                                                                                             |
| **Requirement** | Physical files **shall not** be deleted from disk if other database records still reference the same physical file (deduplication protection). |
| **Implementation** | `FileStorageService.permanentDeleteFile()` checks `countByStoredName(storedName)`. Physical deletion occurs **only when reference count ≤ 1**. |

#### NFR-4.2 — Trash Auto-Purge Resilience

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-4.2                                                                                              |
| **Priority**  | Medium                                                                                               |
| **Requirement** | The automatic trash cleanup scheduler **shall** continue processing remaining items even if individual deletions fail. |
| **Implementation** | `TrashCleanupScheduler.purgeExpiredTrashItems()` wraps each `permanentDeleteFileAdmin()` call in a try-catch, logging errors but continuing the batch. |

#### NFR-4.3 — Folder Move Cycle Prevention

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-4.3                                                                                              |
| **Priority**  | High                                                                                                 |
| **Requirement** | Moving a folder into its own descendant **shall** be prevented to avoid infinite hierarchy loops.    |
| **Implementation** | **BUG-10 Fix**: `FolderService.moveFolder()` walks up the ancestor chain from the target folder. If the source folder ID is found, the operation is rejected. |

#### NFR-4.4 — Database Migration Safety

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-4.4                                                                                              |
| **Priority**  | Medium                                                                                               |
| **Requirement** | Startup migrations **shall** be idempotent and safe to run on both fresh and existing databases.     |
| **Implementation** | `DatabaseMigrationConfig.migrateNullVersions()` uses try-catch — if tables don't exist yet (fresh DB), the error is caught and logged as expected. |

---

### 9.5 Maintainability Requirements

#### NFR-5.1 — Logging

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-5.1                                                                                              |
| **Requirement** | The system **shall** use structured SLF4J logging with appropriate log levels.                      |
| **Implementation** | `logging.level.com.cloudnest=DEBUG` (development), `logging.level.org.springframework.security=INFO`. `Logger` instances in `UserService`, `FileStorageService`, `GlobalExceptionHandler`, `TrashCleanupScheduler`. |

#### NFR-5.2 — DTO Pattern

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-5.2                                                                                              |
| **Requirement** | Entity objects **shall never** be passed directly to Thymeleaf templates to prevent lazy-loading exceptions and information leakage. |
| **Implementation** | `convertToDto()` methods in `FileStorageService` and `FolderService` map entities to lightweight DTOs. |

#### NFR-5.3 — Code Deduplication

| Attribute     | Detail                                                                                               |
| ------------- | ---------------------------------------------------------------------------------------------------- |
| **ID**        | NFR-5.3                                                                                              |
| **Requirement** | Shared logic **shall** be extracted into utility classes to eliminate code duplication.               |
| **Implementation** | `FormatUtils.formatBytes()` centralizes byte formatting used across `AdminController`, `DashboardController`, `ShareController`. |

---

## 10. Testing Specification

### 10.1 Test Environment

| Component         | Configuration                                                     |
| ----------------- | ----------------------------------------------------------------- |
| Database          | H2 In-Memory Database (PostgreSQL compatibility mode: `MODE=PostgreSQL`) |
| DDL Mode          | `create-drop` (tables created at test start, dropped after)       |
| Storage Path      | `target/test-storage` (isolated from production)                  |
| Quota             | 1 GB (same as production)                                         |
| Node Count        | 3 (same as production)                                            |
| Logging           | `com.cloudnest=INFO`, `springframework.security=WARN`             |

### 10.2 Test Suite Inventory

#### Unit Tests (Mocked Dependencies)

| Test Class                    | Target                     | Scope                                           |
| ----------------------------- | -------------------------- | ------------------------------------------------ |
| `UserServiceTest.java`        | `UserService`              | Registration validation, password encoding, user lookup, Spring Security integration |
| `FileStorageServiceTest.java` | `FileStorageService`       | Upload with dedup, quota enforcement, file operations, blocked extensions |
| `FolderServiceTest.java`      | `FolderService`            | Folder creation, deletion, breadcrumbs, hierarchy validation |
| `StorageNodeServiceTest.java` | `StorageNodeService`       | Random node selection, file path generation      |

#### Controller Tests (MockMvc)

| Test Class                   | Target              | Scope                                               |
| ---------------------------- | -------------------- | ---------------------------------------------------- |
| `AuthControllerTest.java`    | `AuthController`     | Login/register page rendering, form submission, validation errors |
| `FileControllerTest.java`    | `FileController`     | File listing, upload, download, search endpoints     |

#### Integration Tests (Full Context)

| Test Class                                  | Target                                     | Scope                                              |
| ------------------------------------------- | ------------------------------------------ | -------------------------------------------------- |
| `AuthIntegrationTest.java`                  | Authentication flow                        | End-to-end registration + login + session creation |
| `FileIntegrationTest.java`                  | File operations                            | Upload → list → download → delete → trash cycle   |
| `FolderIntegrationTest.java`                | Folder operations                          | Create → nest → move → delete → restore cycle     |
| `ShareIntegrationTest.java`                 | Sharing flow                               | Generate link → public access → download → expiry |
| `TrashIntegrationTest.java`                 | Trash lifecycle                            | Soft delete → view trash → restore → permanent delete |
| `DashboardAndAdminIntegrationTest.java`     | Dashboard + Admin                          | Dashboard stats, admin role toggle, admin file delete |

### 10.3 Test Coverage Matrix

| Module                  | Unit | Controller | Integration | Covered Requirements                          |
| ----------------------- | ---- | ---------- | ----------- | --------------------------------------------- |
| Authentication          | ✅    | ✅          | ✅           | FR-1.1 through FR-1.6, NFR-1.1, NFR-1.2      |
| File Management         | ✅    | ✅          | ✅           | FR-2.1 through FR-2.7, NFR-1.4, NFR-2.5      |
| Folder Management       | ✅    | —          | ✅           | FR-3.1 through FR-3.7, NFR-4.3               |
| Deduplication           | ✅    | —          | ✅           | FR-4.1, FR-4.2, NFR-4.1                       |
| Distributed Storage     | ✅    | —          | —           | FR-5.1, FR-5.2                                 |
| Sharing                 | —    | —          | ✅           | FR-7.1 through FR-7.5, NFR-1.8                |
| Trash                   | —    | —          | ✅           | FR-8.1 through FR-8.4, NFR-4.2                |
| Admin                   | —    | —          | ✅           | FR-9.1 through FR-9.3, NFR-1.5                |

---

## 11. Deployment Specification

### 11.1 Development Deployment

```bash
# Step 1: Ensure PostgreSQL is running and database exists
# psql: CREATE DATABASE cloudnest_db;

# Step 2: Set environment variables
# Windows PowerShell:
$env:DB_PASSWORD = "your_password"

# Step 3: Build and Run
./mvnw spring-boot:run

# Application starts at http://localhost:8080
```

### 11.2 Production Build

```bash
# Build executable JAR
./mvnw clean package -DskipTests

# Run JAR
java -jar target/cloudnest-1.0.0.jar \
  --DB_PASSWORD=secure_password \
  --DDL_AUTO=validate \
  --SHOW_SQL=false
```

### 11.3 Configuration Properties Reference

| Property                                  | Default                    | Description                                  |
| ----------------------------------------- | -------------------------- | -------------------------------------------- |
| `server.port`                             | `8080`                     | HTTP port                                    |
| `spring.datasource.url`                   | `jdbc:postgresql://localhost:5432/cloudnest_db` | Database connection URL |
| `spring.datasource.username`              | `postgres`                 | Database username                            |
| `spring.datasource.password`              | `${DB_PASSWORD}`           | Database password (from env)                 |
| `spring.jpa.hibernate.ddl-auto`           | `${DDL_AUTO:update}`       | Schema management mode                       |
| `spring.jpa.show-sql`                     | `${SHOW_SQL:false}`        | SQL query logging                            |
| `spring.servlet.multipart.max-file-size`  | `50MB`                     | Max upload file size                         |
| `spring.servlet.multipart.max-request-size`| `50MB`                    | Max total request size                       |
| `spring.thymeleaf.cache`                  | `false`                    | Template caching (set `true` in production)  |
| `cloudnest.storage.base-path`             | `storage`                  | Root storage directory                       |
| `cloudnest.storage.quota-bytes`           | `1073741824` (1 GB)        | Per-user storage quota                       |
| `cloudnest.storage.node-count`            | `3`                        | Number of simulated storage nodes            |
| `management.endpoints.web.exposure.include`| `health,info`             | Exposed Actuator endpoints                   |
| `logging.level.com.cloudnest`             | `DEBUG`                    | Application log level                        |

---

## 12. Technology Stack Summary

### 12.1 Backend Stack

| Technology               | Version  | Purpose                                              |
| ------------------------ | -------- | ---------------------------------------------------- |
| Java (JDK)               | 21 LTS   | Programming language and runtime                     |
| Spring Boot              | 3.4.5    | Application framework with embedded Tomcat           |
| Spring Web (MVC)         | 6.x      | REST controllers, request mapping, multipart upload  |
| Spring Security          | 6.x      | Authentication, authorization, CSRF, session management |
| Spring Data JPA          | 3.x      | Repository abstraction over Hibernate ORM            |
| Hibernate ORM            | 6.x      | Object-relational mapping, DDL generation            |
| Spring Boot Actuator     | 3.4.5    | Health checks and operational endpoints              |
| Bean Validation (JSR 380)| 3.x      | DTO field validation annotations                     |
| Thymeleaf                | 3.x      | Server-side HTML template rendering                  |
| Thymeleaf Spring Security| 3.x      | `sec:authorize` directives in templates              |
| PostgreSQL JDBC Driver   | 42.x     | Database connectivity (runtime scope)                |
| Lombok                   | 1.18.x   | Compile-time code generation (getters, builders)     |
| Spring Boot DevTools     | 3.4.5    | Hot reload during development                        |
| Maven                    | 3.8.x+   | Build tool and dependency management                 |

### 12.2 Frontend Stack

| Technology        | Version   | Purpose                                             |
| ----------------- | --------- | --------------------------------------------------- |
| HTML5             | —         | Page structure and semantic markup                  |
| CSS3              | —         | Styling, animations, responsive design              |
| JavaScript (ES6)  | —         | Client-side interactivity, drag-and-drop, modals    |
| Lucide Icons      | 0.325.0   | SVG icon library (CDN)                              |
| Three.js          | latest    | WebGL 3D animated backgrounds (CDN)                 |
| Google Fonts      | —         | Inter + JetBrains Mono typefaces (CDN)              |

### 12.3 Testing Stack

| Technology               | Purpose                                        |
| ------------------------ | ---------------------------------------------- |
| Spring Boot Starter Test | JUnit 5 + Mockito + AssertJ + MockMvc          |
| Spring Security Test     | `@WithMockUser`, `SecurityMockMvcRequestPostProcessors` |
| H2 Database              | In-memory database for isolated testing        |

---

## 13. File Inventory

### 13.1 Source File Count

| Category              | Count | Files                                                                |
| --------------------- | ----- | -------------------------------------------------------------------- |
| Java Source (main)    | 24    | 1 entry point + 2 configs + 7 controllers + 4 DTOs + 4 entities + 3 exceptions + 4 repositories + 1 security + 6 services + 1 utility |
| Java Source (test)    | 12    | 4 unit tests + 2 controller tests + 6 integration tests             |
| Thymeleaf Templates   | 16    | 14 pages + 2 fragment components                                     |
| Error Pages           | 2     | 404.html + 500.html                                                  |
| CSS Files             | 3     | design-system.css + animations.css + style.css                       |
| JavaScript Files      | 3     | app.js + gsap-animations.js + webgl-scene.js                        |
| Image Assets          | 1     | logo.svg                                                             |
| Configuration Files   | 4     | pom.xml + application.properties + .env + .gitignore                 |
| Database Scripts      | 1     | schema.sql                                                           |
| **Total**             | **66**| **Across all categories**                                            |

### 13.2 Lines of Code Estimate

| Category          | Approx. LOC | Notes                                     |
| ----------------- | ----------- | ----------------------------------------- |
| Java (main)       | ~2,200      | Business logic, controllers, entities      |
| Java (test)       | ~800        | Unit + controller + integration tests      |
| Thymeleaf (HTML)  | ~3,500      | 16 templates + 2 error pages               |
| CSS               | ~2,800      | Design system + animations                 |
| JavaScript        | ~2,400      | App logic + GSAP + WebGL                   |
| Config/SQL/Other  | ~350        | Properties, schema, pom, env               |
| **Total**         | **~12,050** |                                            |

---

## 14. Known Bug Fixes Documented in Code

| Bug ID  | Description                                                                                   | Fix Location                            |
| ------- | --------------------------------------------------------------------------------------------- | --------------------------------------- |
| BUG-02  | Schema was originally written for MySQL syntax; migrated to PostgreSQL                        | `schema.sql`                            |
| BUG-05  | Deleting a parent folder left child files/folders in non-deleted state (leaked into search)    | `FolderService.softDeleteRecursively()` / `restoreRecursively()` |
| BUG-07  | Shared links allowed downloading soft-deleted files                                           | `ShareController` (both view + download)|
| BUG-10  | Moving a folder into its own descendant created an infinite hierarchy loop                     | `FolderService.moveFolder()`            |
| BUG-VER | Adding `@Version` later caused `NullPointerException` on existing rows with null version      | `DatabaseMigrationConfig.migrateNullVersions()` |

---

## 15. Glossary of Technical Terms

| Term                    | Definition                                                                                       |
| ----------------------- | ------------------------------------------------------------------------------------------------ |
| **BIGSERIAL**           | PostgreSQL auto-incrementing 64-bit integer column type                                          |
| **Builder Pattern**     | Design pattern providing fluent API for object construction (`User.builder().username("...").build()`) |
| **Content-Addressed**   | Storage where the address (filename/location) is derived from the content hash                   |
| **CORS**                | Cross-Origin Resource Sharing — HTTP header-based mechanism for controlling cross-site requests  |
| **Derived Query Method**| Spring Data JPA feature that auto-generates SQL from repository method names                     |
| **Flash Attribute**     | Spring MVC mechanism for passing one-time data across redirects via session                      |
| **IDOR**                | Insecure Direct Object Reference — vulnerability where users access resources by guessing IDs   |
| **Idempotent**          | An operation that produces the same result regardless of how many times it is executed            |
| **Monolith**            | Architecture where all application components run in a single deployable unit                    |
| **N-Tier**              | Architecture with distinct layers (presentation, business, data access, persistence)             |
| **Optimistic Locking**  | Concurrency strategy that detects conflicts at write time using version numbers                  |
| **Orphan Removal**      | JPA feature that auto-deletes child entities when removed from a parent's collection             |
| **Principal**           | Spring Security's representation of the currently authenticated user                             |
| **Query Derivation**    | Spring Data JPA's ability to generate queries from method names (e.g., `findByUsername`)         |
| **TOCTOU**              | Time-of-Check-Time-of-Use — race condition where state changes between check and use             |
| **UUID**                | Universally Unique Identifier — 128-bit value with negligible collision probability              |

---

## 16. Document Revision History

| Version | Date           | Author     | Changes                                                           |
| ------- | -------------- | ---------- | ----------------------------------------------------------------- |
| 1.0     | Initial        | Anmol Raj  | Original SRS with basic requirements                              |
| 2.0     | June 01, 2026  | Anmol Raj  | Comprehensive 4-part rewrite covering all 66 source files, 38+ functional requirements, 24 non-functional requirements, complete architecture diagrams, data models, API catalog, testing spec, and deployment guide |

---

*End of Part 4 — This concludes the complete CloudNest Software Requirements Specification.*

---

### SRS Document Index

| Part | File                                             | Content                                      |
| ---- | ------------------------------------------------ | -------------------------------------------- |
| 1    | `SRS_Part1_Introduction_and_Overview.md`         | Introduction, Scope, Overall Description      |
| 2    | `SRS_Part2_Functional_Requirements.md`           | All Functional Requirements (38+ FRs)         |
| 3    | `SRS_Part3_Architecture_and_DataModels.md`       | Architecture, Data Models, API Catalog        |
| 4    | `SRS_Part4_NonFunctional_and_Appendices.md`      | NFRs, Testing, Deployment, Glossary           |
