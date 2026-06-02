# ☁️ CloudNest — Master Learning Guide — Part 5

> **Chapters 9–10: Production Engineering & Bug/Audit Learning**
> Level: 🔴 Advanced

---

# Chapter 9 — Production Engineering Concepts

## 9.1 Scalability

### Current State
CloudNest runs as a **single instance** on one server. All data is on one PostgreSQL database and one local filesystem. This handles ~100 concurrent users comfortably.

### How to Scale

**Vertical Scaling** (Bigger server): More RAM, faster CPU. Simple but limited — you hit a ceiling.

**Horizontal Scaling** (More servers):
```
                    ┌───────────┐
                    │   Nginx   │  (Load Balancer)
                    │           │
                    └─────┬─────┘
                ┌─────────┼─────────┐
                ▼         ▼         ▼
         ┌──────────┐ ┌──────────┐ ┌──────────┐
         │ CloudNest│ │ CloudNest│ │ CloudNest│
         │ Instance1│ │ Instance2│ │ Instance3│
         └─────┬────┘ └─────┬────┘ └─────┬────┘
               │             │             │
               └──────┬──────┘──────┬──────┘
                      ▼             ▼
               ┌──────────┐  ┌──────────┐
               │PostgreSQL│  │  S3/MinIO │
               │ (shared) │  │ (shared)  │
               └──────────┘  └──────────┘
```

**Problem with multiple instances**: Sessions. If User A logs into Instance 1, their session is stored in Instance 1's memory. If the next request goes to Instance 2, the user is not authenticated!

**Solution**: Store sessions in Redis (shared across all instances):
```properties
# application.properties for horizontal scaling
spring.session.store-type=redis
spring.redis.host=redis-server
spring.redis.port=6379
```

### Connection to CloudNest
The `storage/` directory is a local filesystem. To scale horizontally, replace it with object storage (S3/MinIO). The `StorageNodeService` would need refactoring to use an S3 client instead of `Files.write()`.

---

## 9.2 Concurrency & Race Conditions

### What is a Race Condition?
Two operations running simultaneously produce incorrect results because they interfere with each other.

### CloudNest Example: Concurrent File Upload (Same Content)

```
Thread 1: Upload file.pdf (hash = abc123)
Thread 2: Upload file.pdf (hash = abc123) — SAME FILE, same instant

Time →
Thread 1: findFirstByFileHash("abc123") → null (no match yet)
Thread 2: findFirstByFileHash("abc123") → null (no match yet)  ← RACE!
Thread 1: Write file to disk, save record to DB
Thread 2: Write DUPLICATE file to disk, save record to DB

Result: Two physical copies instead of one. Dedup failed!
```

Both threads see "no duplicate" and both write the file. The dedup check is not atomic with the write.

### How `@Version` Prevents Lost Updates

```java
@Version
private Long version;  // Starts at 0
```

When you save an entity, Hibernate adds a version check:
```sql
UPDATE files SET original_name = ?, version = 1 WHERE id = ? AND version = 0
```

If two requests try to update the same record:
- Thread 1: `UPDATE ... WHERE version = 0` → succeeds, version becomes 1
- Thread 2: `UPDATE ... WHERE version = 0` → **0 rows affected** → throws `OptimisticLockingFailureException`

This is called **optimistic locking** — assume no conflict, detect and handle it if one occurs.

### CloudNest Example: Admin Role Toggle Race

```
Admin A: sees user "john" (version=0, role=ROLE_USER)
Admin B: sees user "john" (version=0, role=ROLE_USER)

Admin A clicks "Promote": UPDATE users SET role='ROLE_ADMIN', version=1 WHERE id=5 AND version=0 → ✅
Admin B clicks "Promote": UPDATE users SET role='ROLE_ADMIN', version=1 WHERE id=5 AND version=0 → ❌ FAIL
                                                                        (version is now 1, not 0!)

Admin B gets: "This record was modified. Please refresh."
```

---

## 9.3 TOCTOU (Time-of-Check-to-Time-of-Use)

### What It Is
A vulnerability where you **check** a condition, then **use** the result — but the condition changes between the check and the use.

### CloudNest's Approach
The upload code reads the file into memory and computes the SHA-256 hash **before** checking the database or touching disk:

```java
byte[] fileBytes = file.getBytes();          // Read into memory
String fileHash = computeSha256(fileBytes);   // Hash in memory
FileEntity existing = fileRepository.findFirstByFileHash(fileHash);  // Check DB
```

By hashing in memory first, we avoid a scenario where we write a file to disk, then discover it's a duplicate, and have to clean up.

However, a true TOCTOU fix for the dedup race condition would require a database-level unique constraint or advisory lock:
```sql
-- Option 1: Unique constraint (fails on duplicate insert)
ALTER TABLE files ADD CONSTRAINT uq_file_hash UNIQUE (file_hash);

-- Option 2: Advisory lock (serialize access for same hash)
SELECT pg_advisory_lock(hashtext('abc123...'));
-- ... check and insert ...
SELECT pg_advisory_unlock(hashtext('abc123...'));
```

---

## 9.4 N+1 Query Problem

### What It Is
You query for N items, then for each item, you make 1 additional query to fetch related data. Total: N+1 queries instead of 1–2.

### CloudNest Example (in AdminController)

```java
List<FileEntity> allFiles = fileRepository.findAll();  // 1 query: SELECT * FROM files
```

Then the admin template accesses:
```html
<td th:text="${fileItem.user.username}">Owner</td>
```

For each file, Hibernate lazily loads the user:
```sql
SELECT * FROM users WHERE id = 1;  -- For file 1
SELECT * FROM users WHERE id = 2;  -- For file 2
SELECT * FROM users WHERE id = 1;  -- For file 3 (same user, but Hibernate may re-query!)
... (N more queries)
```

If there are 1000 files owned by 50 users, this generates **1001 queries** instead of 1 query with a JOIN.

### Fix: JOIN FETCH
```java
@Query("SELECT f FROM FileEntity f JOIN FETCH f.user WHERE f.isDeleted = false")
List<FileEntity> findAllWithUser();
```

This generates a single query:
```sql
SELECT f.*, u.* FROM files f
INNER JOIN users u ON f.user_id = u.id
WHERE f.is_deleted = false;
```

One query instead of 1001. Massive performance improvement.

---

## 9.5 Pagination

### The Problem
`fileRepository.findAll()` loads **all records into memory**. With 100,000 files, this causes `OutOfMemoryError`.

### The Solution
Spring Data JPA supports `Pageable`:
```java
// Repository
Page<FileEntity> findByUserAndIsDeletedFalse(User user, Pageable pageable);

// Service usage
Pageable pageable = PageRequest.of(
    0,           // Page number (0-indexed)
    20,          // Page size
    Sort.by("uploadedAt").descending()
);
Page<FileEntity> page = fileRepository.findByUserAndIsDeletedFalse(user, pageable);

page.getContent()        // → List of 20 files for this page
page.getTotalPages()     // → Total number of pages
page.getTotalElements()  // → Total number of files across all pages
page.hasNext()           // → Is there a next page?
```

Generated SQL:
```sql
SELECT * FROM files
WHERE user_id = ? AND is_deleted = false
ORDER BY uploaded_at DESC
LIMIT 20 OFFSET 0;
```

CloudNest currently doesn't use pagination — this is a key scalability improvement needed for production.

---

## 9.6 Caching

### Where Caching Would Help in CloudNest

| Data | Currently | With Cache | TTL |
|------|-----------|------------|-----|
| Dashboard stats | DB query every page load | Cache in Caffeine/Redis | 5 min |
| User lookup by username | DB query every request | Cache in Caffeine | 10 min |
| Node statistics | DB aggregate query | Cache in Redis | 15 min |
| File type distributions | DB GROUP BY query | Cache in Caffeine | 5 min |

### How Spring Cache Works

Add the dependency and annotation:
```java
@EnableCaching  // On CloudNestApplication.java

@Cacheable("dashboard-stats")  // On the service method
public DashboardDto getDashboardStats(User user) {
    // This expensive computation only runs once every 5 minutes
    // Subsequent calls return the cached result instantly
    long totalFiles = fileRepository.countByUserAndIsDeletedFalse(user);
    long storageUsed = fileRepository.sumFileSizeByUser(user);
    // ... build DTO ...
}
```

First call: runs the method, caches the result (key = user's ID).
Next 100 calls within 5 minutes: returns cached result instantly (no DB queries!).
After 5 minutes: cache expires, next call runs the method again.

### Cache Invalidation — The Hard Problem

> "There are only two hard things in Computer Science: cache invalidation and naming things." — Phil Karlton

When a user uploads a file, the dashboard cache becomes stale. You must **evict** the cache:
```java
@CacheEvict(value = "dashboard-stats", key = "#user.id")
public FileDto uploadFile(MultipartFile file, User user, Long folderId) {
    // After this method runs, the cached dashboard stats for this user are cleared
}
```

---

## 9.7 Logging Best Practices

### Current Problem in CloudNest
```java
// BAD: System.out.println bypasses the logging framework
System.out.println("🔥 DEDUPLICATION TRIGGERED! Reused existing file for hash: " + fileHash);
```

### Why This Is Bad
- Can't filter by log level (DEBUG, INFO, WARN, ERROR)
- Can't route to files, monitoring systems, or log aggregators
- Can't search/filter in production
- No timestamps, no thread names, no class context

### How to Do It Right
```java
private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

// Contextual, structured logging:
log.info("Deduplication triggered — reused existing file for hash: {}", fileHash);
log.warn("File not found on disk: node={}, storedName={}", node, storedName);
log.error("Failed to upload file '{}' for user '{}': {}",
          originalName, user.getUsername(), e.getMessage(), e);
```

### Log Levels Explained

| Level | When to Use | Example |
|-------|-------------|---------|
| `TRACE` | Ultra-detailed debugging | Method entry/exit |
| `DEBUG` | Detailed debugging | Variable values, flow decisions |
| `INFO` | Normal operations | "User registered", "File uploaded" |
| `WARN` | Something unexpected but recoverable | "File not found on disk, but DB record exists" |
| `ERROR` | Something failed | "Database connection lost", "Disk write failed" |

### Production Log Configuration
```properties
# application-prod.properties
logging.level.com.cloudnest=INFO          # Only INFO and above in production
logging.level.org.springframework=WARN    # Reduce Spring's verbosity
logging.file.name=/var/log/cloudnest/app.log
logging.file.max-size=100MB
logging.file.max-history=30               # Keep 30 days of logs
```

---

## 9.8 Health Checks & Monitoring

### What Health Checks Do
Health checks let infrastructure (load balancers, Kubernetes, monitoring) know if your application is alive and ready to serve traffic.

### Current State
CloudNest has Spring Boot Actuator with basic health endpoint:
```properties
management.endpoints.web.exposure.include=health,info
```

`GET /actuator/health` returns:
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" }
  }
}
```

### What's Missing for Production

| Check | Purpose | How to Add |
|-------|---------|-----------|
| Database connectivity | Is PostgreSQL reachable? | ✅ Built-in with Actuator |
| Disk space | Is storage full? | ✅ Built-in with Actuator |
| Storage nodes accessible | Can we write to node1/2/3? | Custom `HealthIndicator` |
| Memory usage | Is JVM memory low? | Expose JVM metrics via Actuator |
| Response time | Are requests slow? | Micrometer + Prometheus |

---

# Chapter 10 — Bug & Audit Learning Section

This section uses the real audit findings from your project as teaching material. For every bug, we explain **what happened**, **why it happened**, **how senior engineers prevent it**, and **the engineering lesson**.

## BUG-01: Hardcoded Database Password 🔴 CRITICAL

### What Happened
The database password `#nanshu@229` was hardcoded in plain text in `application.properties` and committed to Git.

### The Security Impact
Anyone with repository access (teammates, GitHub, CI/CD logs) can read the production database password. This is **OWASP Top 10 #2: Cryptographic Failures**.

### Root Cause
During development, it's tempting to hardcode values for convenience. The developer forgot to externalize the secret before committing.

### Engineering Lesson
> **Never commit secrets to version control.** Once a secret is in Git history, it's there forever — even if you delete it in a later commit. The Git reflog and old commits still contain it.

### How Senior Engineers Prevent This
1. **`.env` files** — Store secrets in `.env`, add `.env` to `.gitignore`
2. **Environment variables** — `${DB_PASSWORD}` in properties
3. **Pre-commit hooks** — Tools like `git-secrets` or `truffleHog` scan for secret patterns
4. **Secret managers** — HashiCorp Vault, AWS Secrets Manager, Azure Key Vault
5. **Code review** — Any reviewer should flag hardcoded credentials instantly

### The Fix
```properties
# Before (DANGEROUS):
spring.datasource.password=#nanshu@229

# After (SAFE):
spring.datasource.password=${DB_PASSWORD}
```

---

## BUG-03: Admin Dashboard NPE 🟡 HIGH

### What Happened
`AdminController.showAdminDashboard()` used `fileRepository.findAll()` and then streamed the results with `.mapToLong(FileEntity::getFileSize)`. Since `fileSize` is a `Long` (wrapper type, not primitive `long`), null values cause an **unboxing NullPointerException**.

### Root Cause
The AI generated the streaming code without considering that `fileSize` is a nullable `Long` wrapper type.

### Engineering Lesson
> **Always consider null safety when working with wrapper types.** In Java, `Long` can be null, but `long` cannot. Automatic unboxing (`Long` → `long`) throws NPE if the value is null.

### How Senior Engineers Think About This
- "Is this field nullable? Let me check the entity — `private Long fileSize;` — yes, it's a wrapper type."
- "What if a file was created without a size being set? The stream will NPE."
- "Better yet, why am I loading ALL files into memory? This should be a database aggregation."

### The Fix
```java
// Quick fix (null-safe mapping):
long total = files.stream()
    .filter(f -> !f.isDeleted())
    .mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0L)
    .sum();

// Better fix (database aggregation — no files loaded into memory):
@Query("SELECT COALESCE(SUM(f.fileSize), 0) FROM FileEntity f WHERE f.isDeleted = false")
long sumTotalFileSize();
```

---

## BUG-05: Folder Soft Delete Doesn't Cascade 🟡 HIGH

### What Happened
When a folder was soft-deleted, only the folder's `isDeleted` flag was set to `true`. Files inside the folder stayed with `isDeleted = false`, meaning they:
- Still appeared in search results
- Still counted toward storage quota
- Still showed on the dashboard

### Root Cause
The developer implemented the simplest possible soft-delete without considering child entities. This is a **data consistency** problem.

### Engineering Lesson
> **Always think about cascading effects.** When you change a parent entity's state, ask: "What should happen to the children?" Database-level `ON DELETE CASCADE` handles hard deletes automatically, but soft deletes require **manual cascade logic** in your application code.

### The Fix (Recursive Soft Delete)
```java
private void softDeleteRecursively(Folder folder) {
    LocalDateTime now = LocalDateTime.now();
    folder.setDeleted(true);
    folder.setDeletedAt(now);

    // Cascade to files in this folder
    for (FileEntity file : folder.getFiles()) {
        file.setDeleted(true);
        file.setDeletedAt(now);
    }

    // Recurse into sub-folders
    for (Folder sub : folder.getSubFolders()) {
        softDeleteRecursively(sub);
    }
}
```

### How Senior Engineers Prevent This
Before implementing any state-changing operation, draw the entity graph:
```
Folder (deleted=true)
├── File A (deleted=???)    ← What should happen here?
├── File B (deleted=???)    ← And here?
└── Sub-Folder
    └── File C (deleted=???)  ← And here?
```

The answer is obvious when you visualize it: all children should inherit the deleted state.

---

## BUG-07: IDOR on Shared File Downloads 🟡 HIGH

### What Happened
The shared file download endpoint (`GET /share/download/{token}`) resolved the token and streamed the file, but:
1. It didn't check if the file had been soft-deleted
2. It didn't verify the physical file existed on disk

### Root Cause
The developer implemented the "happy path" (file exists, not deleted) but forgot the edge cases.

### Engineering Lesson
> **Always validate the complete state of a resource before serving it.** Just because a database record exists doesn't mean the physical resource is still valid.

### The Fix
```java
public ResponseEntity<Resource> downloadSharedFile(@PathVariable String token) {
    SharedLink link = sharedLinkService.resolveShareLink(token);
    FileEntity file = link.getFile();

    // Check 1: Is the file soft-deleted?
    if (file.isDeleted()) {
        throw new FileNotFoundException("This shared file has been deleted.");
    }

    // Check 2: Does the physical file exist on disk?
    Path path = Paths.get(filePath);
    if (!Files.exists(path)) {
        throw new FileNotFoundException("Shared file not found on disk.");
    }

    // Only now proceed with download...
}
```

---

## BUG-08: Self-Assign Admin Role 🔴 CRITICAL

### What Happened
The `UserRegistrationDto` had a `role` field. The registration logic checked if `dto.getRole()` was "ADMIN" and assigned `ROLE_ADMIN`. Since registration is **public** (no login required), any anonymous user could POST with `role=ADMIN` and create an admin account.

### The Exploit
```bash
# Anyone can become admin with a single curl command:
curl -X POST http://localhost:8080/register \
  -d "username=hacker&email=h@evil.com&password=pass&confirmPassword=pass&role=ADMIN"
```

### Root Cause
The developer added the `role` field for flexibility without considering that the registration endpoint is publicly accessible. This is a classic **mass assignment** vulnerability (OWASP).

### Engineering Lesson
> **Never trust user input for authorization decisions.** Public endpoints should **always** assign the lowest privilege level. Admin roles should only be assignable through authenticated admin operations (like the admin panel's toggle-role feature).

### How Senior Engineers Think
- "This is a public endpoint. What's the worst thing a user could put in every field?"
- "The `role` field in the DTO — who should be allowed to set this?"
- "What's the blast radius if exploited?" → Full admin access, data breach.

### The Fix
```java
// Remove role field from UserRegistrationDto entirely
// OR ignore it during registration:
String finalRole = "ROLE_USER";  // ALWAYS. No exceptions on public registration.
```

---

## BUG-09: Global Exception Handler Swallows Exceptions 🟠 MEDIUM

### What Happened
The catch-all `@ExceptionHandler(Exception.class)` caught every exception and redirected to `/dashboard` with a generic message — **without logging the actual exception**.

### Why This Is Devastating
In production, you get a user report: "I got an error." You check the logs. Nothing. No stack trace. No error message. No context. You have **zero information** about what went wrong.

### Engineering Lesson
> **Never catch an exception without logging it.** The user gets a friendly message; the developer gets the full stack trace in the logs. Both are necessary.

### The Fix
```java
@ExceptionHandler(Exception.class)
public String handleGenericException(Exception ex, RedirectAttributes redirectAttributes) {
    // CRITICAL: Log the full exception for debugging
    log.error("Unhandled exception caught by GlobalExceptionHandler", ex);

    // User-facing: friendly message (no stack traces!)
    redirectAttributes.addFlashAttribute("error",
        "An unexpected error occurred. Please try again.");
    return "redirect:/dashboard";
}
```

---

## BUG-10: Circular Folder Reference 🟠 MEDIUM

### What Happened
A folder could be moved into its own descendant, creating an infinite loop:
```
Before:               After (BUG):
A/                    B/
└── B/                  └── A/       ← A is now INSIDE its own child!
    └── C/                  └── B/   ← infinite loop!
                                └── C/
                                    └── A/  ← ...forever
```

The code checked `folderId.equals(targetFolderId)` (can't move A into A) but NOT if the target is a **descendant** of the source.

### Engineering Lesson
> **Graph operations require cycle detection.** Whenever you have a tree structure (folders, org charts, categories), any operation that changes parent-child relationships must check for cycles by walking the ancestor chain.

### The Fix (Walk-Up Algorithm)
```java
// Start at the target folder and walk UP to the root
// If we encounter the source folder anywhere in the chain, it's a cycle
Folder ancestor = targetFolder;
while (ancestor != null) {
    if (ancestor.getId().equals(folderId)) {
        throw new IllegalArgumentException(
            "Cannot move a folder into its own descendant — this would create a cycle");
    }
    ancestor = ancestor.getParent();  // Walk up one level
}
```

This is O(d) where d = depth of the folder tree. For a typical folder tree (depth ≤ 20), this is negligible.

---

## Standards Violations Summary

These are architectural patterns that, while not causing crashes, represent technical debt and deviation from industry best practices:

| # | Violation | Impact | Fix |
|---|-----------|--------|-----|
| 1 | No database migration tool (Flyway) | Can't track schema changes; `ddl-auto=update` is dangerous | Add Flyway, create versioned migration scripts |
| 2 | Controllers contain business logic | Hard to test, violates SRP | Extract to service classes (e.g., `AdminService`) |
| 3 | Repositories injected directly into controllers | Bypasses service layer | Route all data access through services |
| 4 | Admin template accesses lazy entity fields | `LazyInitializationException` risk | Use DTOs at the view boundary |
| 5 | `System.out.println` for logging | Bypasses logging framework | Use SLF4J |
| 6 | No API versioning | Breaking changes affect all clients | Add `/api/v1/` prefix for REST endpoints |
| 7 | No health check endpoint | Can't monitor in production | Add Spring Boot Actuator |
| 8 | No request correlation IDs | Can't trace logs across requests | MDC-based correlation |
| 9 | `ddl-auto=update` in main config | Schema mutations without tracking | Profile-specific properties |
| 10 | CDN dependencies without version pinning | `@latest` can break at any time | Pin to specific version |
| 11 | No input validation on folder names | Potential Zip Slip vulnerability | Sanitize folder names |
| 12 | Entity comments reference MySQL | Documentation is wrong | Update to say PostgreSQL |
| 13 | `formatBytes()` duplicated in 3 classes | DRY violation | ✅ Already extracted to `FormatUtils` |
| 14 | No `@Version` for optimistic locking | Concurrent writes silently overwrite | ✅ Already added to all entities |
| 15 | Exception class shadows JDK name | Confuses IDE auto-imports | Rename to `CloudNestFileNotFoundException` |

---

> **📖 Continue to Part 6** → `Learning_Guide_Part6_Refactoring_DevOps_Lessons.md`
