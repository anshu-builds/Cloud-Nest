# ☁️ CloudNest — Master Learning Guide — Part 3

> **Chapters 5–6: Request Flow Diagrams & Database Deep Dive**
> Level: 🟡 Intermediate

---

# Chapter 5 — Request Flow Diagrams

Every feature in CloudNest follows a predictable path: **Browser → Security → Controller → Service → Repository → Database**. This chapter traces every major flow step-by-step.

## 5.1 User Registration Flow

```
┌──────────┐     GET /register     ┌────────────────┐     render      ┌───────────────┐
│  Browser  │ ──────────────────▶  │ AuthController  │ ─────────────▶  │ register.html  │
│           │                      │ showRegPage()   │                 │ (empty form)   │
│           │ ◀─────────────────── │ model.add(dto)  │ ◀────────────── │               │
└─────┬─────┘                      └────────────────┘                 └───────────────┘
      │
      │ User fills form, clicks "Register"
      │
      │     POST /register                ┌────────────────┐
      │ ──────────────────────────────▶   │ AuthController  │
      │     {username, email,             │ registerUser()  │
      │      password, confirmPassword}   └───────┬────────┘
      │                                           │
      │                                           ▼
      │                                   ┌───────────────┐
      │                                   │  UserService   │
      │                                   │ registerUser() │
      │                                   └───────┬───────┘
      │                                           │
      │                        ┌──────────────────┼──────────────────┐
      │                        │ Validate:         │ Encode:           │
      │                        │ • passwords match │ • BCrypt hash     │
      │                        │ • username unique  │   the password    │
      │                        │ • email unique     │                   │
      │                        └──────────────────┼──────────────────┘
      │                                           │
      │                                           ▼
      │                                   ┌────────────────┐
      │                                   │ UserRepository │
      │                                   │   .save(user)  │
      │                                   └───────┬────────┘
      │                                           │
      │                                           ▼
      │                                   ┌────────────────┐
      │     redirect: /login?success      │   PostgreSQL   │
      │ ◀────────────────────────────────  │  INSERT INTO   │
      │                                   │    users (...)  │
      │                                   └────────────────┘
```

### What Happens at Each Layer

| Step | Layer | What Happens | Could Fail Because |
|------|-------|-------------|-------------------|
| 1 | Browser | User fills form, submits POST | Network error |
| 2 | Security | CSRF token validated | Missing/invalid CSRF token |
| 3 | Controller | Receives form data, calls service | Malformed request |
| 4 | Service | Validates input, hashes password | Duplicate username/email, password mismatch |
| 5 | Repository | Saves to database | Database constraint violation |
| 6 | Database | INSERT statement executes | Connection error |
| 7 | Controller | Redirect to `/login?success` | — |

---

## 5.2 Login Flow

```
Browser ──▶ GET /login ──▶ AuthController.showLoginPage()
                                    │
                                    ▼ returns "login" template
Browser ◀── login.html (form with CSRF token)

Browser ──▶ POST /login (username + password + CSRF token)
                │
                ▼
        ┌───────────────────────────────────────────┐
        │        Spring Security Filter Chain        │
        │                                            │
        │  1. CsrfFilter: validates CSRF token       │
        │  2. UsernamePasswordAuthenticationFilter:   │
        │     calls UserService.loadUserByUsername()   │
        │  3. DaoAuthenticationProvider:              │
        │     calls BCrypt.matches(input, storedHash) │
        │                                            │
        │  SUCCESS → Create session, set JSESSIONID  │
        │  FAILURE → Redirect to /login?error=true   │
        └───────────────────┬───────────────────────┘
                            │
                            ▼
        Browser ◀── redirect: /dashboard (with JSESSIONID cookie)
```

### Session Lifecycle

```
Login:
  Server creates HttpSession → generates JSESSIONID → sets cookie
  JSESSIONID=abc123def456; Path=/; HttpOnly

Every subsequent request:
  Browser sends: Cookie: JSESSIONID=abc123def456
  Server looks up session → restores SecurityContext
  User is "remembered" without re-authenticating

Logout:
  Server destroys session → deletes cookie
  Next request → no session → redirect to /login
```

---

## 5.3 File Upload Flow

```
Browser ──▶ POST /files/upload (multipart form: file + folderId + CSRF)
                │
                ▼
        SecurityFilterChain: Is user authenticated? ✅
                │
                ▼
        FileController.uploadFiles()
                │
                ▼
        UserService.findByUsername() ──▶ UserRepository ──▶ PostgreSQL
                │ (returns User entity)
                ▼
        FileStorageService.uploadFile(file, user, folderId)
                │
                ├── 1. Validate: file not empty ✅
                ├── 2. Quota check: currentStorage + fileSize ≤ 1GB ✅
                ├── 3. Extension check: not .exe/.bat/.sh ✅
                ├── 4. Read bytes into memory
                ├── 5. Compute SHA-256 hash
                ├── 6. Check dedup: findFirstByFileHash(hash)
                │       ├── HIT → Reuse storedName + node (no disk write!)
                │       └── MISS → Generate UUID name
                │                  → StorageNodeService.selectNode()
                │                  → Write file to storage/nodeX/uuid.ext
                ├── 7. Resolve target folder (if folderId provided)
                └── 8. Save FileEntity to database
                │
                ▼
        redirect: /files?folderId=X (with success flash message)
```

### Batch Upload Error Handling
CloudNest supports multi-file upload. The controller loops through each file:
```java
for (MultipartFile file : files) {
    try {
        fileStorageService.uploadFile(file, user, folderId);
        successCount++;
    } catch (Exception e) {
        errorCount++;
        errorMessages.add(file.getOriginalFilename() + ": " + e.getMessage());
    }
}
```
**Key insight**: One failing file doesn't prevent other files from uploading. Each file is processed independently.

---

## 5.4 File Download Flow

```
Browser ──▶ GET /files/download/{id}
                │
                ▼
        SecurityFilterChain: authenticated? ✅
                │
                ▼
        FileController.downloadFile(id, principal)
                │
                ├── UserService.findByUsername() → get User
                ├── FileStorageService.getFileEntity(id, user)
                │       ├── Find by ID
                │       ├── Check not soft-deleted
                │       └── Check ownership (user.id matches)
                ├── FileStorageService.getFilePath(id, user)
                │       └── StorageNodeService.getFilePath(node, storedName)
                │           → "storage/node2/abc-123.pdf"
                └── Return ResponseEntity with:
                        Content-Type: application/octet-stream
                        Content-Disposition: attachment; filename="report.pdf"
                        Body: file bytes
                │
                ▼
        Browser ◀── File download starts
```

### Download vs Preview
| Endpoint | Content-Disposition | Behavior |
|----------|-------------------|----------|
| `/files/download/{id}` | `attachment; filename="report.pdf"` | Browser downloads the file |
| `/files/preview/{id}` | `inline; filename="report.pdf"` | Browser tries to display in-tab |

The only difference is `attachment` vs `inline` in the header. `inline` tells the browser "try to display this" — works for images, PDFs, text files.

---

## 5.5 Shared Link Flow

```
═══════════════════════════════════════════════════════════
STEP 1: Generate Link (Authenticated User)
═══════════════════════════════════════════════════════════

Browser ──▶ POST /share/generate/{fileId}
        ──▶ ShareController.generateShareLink()
        ──▶ FileStorageService.getFileEntity() → ownership check ✅
        ──▶ SharedLinkService.generateShareLink(file, user)
                ├── Generate UUID token
                ├── Create SharedLink entity:
                │     token = "a1b2c3d4-..."
                │     file  = fileEntity
                │     createdBy = user
                │     expiresAt = now + 7 days
                └── Save to database
        ◀── redirect: /files (flash message: "Link: /share/a1b2c3d4-...")

═══════════════════════════════════════════════════════════
STEP 2: Access Shared File (Anyone — No Login Required!)
═══════════════════════════════════════════════════════════

Browser ──▶ GET /share/{token}
        ──▶ SecurityConfig: /share/** → permitAll() ✅ (no auth needed)
        ──▶ ShareController.viewSharedFile(token)
        ──▶ SharedLinkService.resolveShareLink(token)
                ├── Find SharedLink by token
                ├── Check not expired (expiresAt > now)
                ├── Get linked FileEntity
                └── Check file not soft-deleted (BUG-07 fix)
        ◀── shared.html (file name, size, type, download button)

═══════════════════════════════════════════════════════════
STEP 3: Download Shared File
═══════════════════════════════════════════════════════════

Browser ──▶ GET /share/download/{token}
        ──▶ Resolve link → Verify not expired → Check file exists on disk
        ──▶ Stream file with Content-Disposition: attachment
        ◀── File download
```

---

## 5.6 Trash Flow (Soft Delete → Restore → Permanent Delete → Auto-Purge)

```
═══════════════════════════════════════════════════
SOFT DELETE (Move to Trash)
═══════════════════════════════════════════════════

POST /files/delete/{id} ──▶ FileStorageService.deleteFile()
                                ├── isDeleted = true
                                ├── deletedAt = now()
                                └── save (file stays in DB and disk)

═══════════════════════════════════════════════════
RESTORE (Take Out of Trash)
═══════════════════════════════════════════════════

POST /trash/restore/file/{id} ──▶ FileStorageService.restoreFile()
                                      ├── isDeleted = false
                                      ├── deletedAt = null
                                      └── save

═══════════════════════════════════════════════════
PERMANENT DELETE (User-Initiated)
═══════════════════════════════════════════════════

POST /trash/delete/file/{id} ──▶ FileStorageService.permanentDeleteFile()
                                      ├── Check dedup: countByStoredName() ≤ 1?
                                      │       YES → Delete physical file from disk
                                      │       NO  → Keep physical file (others use it)
                                      ├── Delete all shared links for this file
                                      └── Delete database record (hard delete)

═══════════════════════════════════════════════════
AUTO-PURGE (Runs Daily at 2 AM)
═══════════════════════════════════════════════════

TrashCleanupScheduler ──▶ @Scheduled(cron = "0 0 2 * * *")
                      ──▶ Find files: isDeleted=true AND deletedAt < 30 days ago
                      ──▶ For each: permanentDeleteFileAdmin()
                      ──▶ Find folders: isDeleted=true AND deletedAt < 30 days ago
                      ──▶ For each: permanentDeleteFolderAdmin()
```

---

## 5.7 Dashboard Flow

```
Browser ──▶ GET /dashboard
        ──▶ DashboardController.showDashboard()
                │
                ├── Get User from Principal
                ├── Query: count active files
                ├── Query: count active folders
                ├── Query: sum file sizes (storage used)
                ├── Query: count files per type (distribution)
                ├── Query: count files per node (distribution)
                ├── Query: top 5 recent files
                ├── Query: top 5 recent folders
                │
                ├── Build DashboardDto with all stats:
                │     totalFiles, totalFolders
                │     storageUsed (formatted: "25.3 MB")
                │     quotaTotal ("1.0 GB")
                │     usagePercent (2.5%)
                │     typeDistribution (map)
                │     nodeDistribution (map)
                │     recentFiles, recentFolders
                │
                └── model.addAttribute("dashboard", dto)
                │
                ▼
        dashboard.html renders with all metrics
```

### Performance Concern 🚨
Each dashboard load triggers **7+ database queries**. At scale, this should be cached (Redis or Caffeine with 5-minute TTL).

---

## 5.8 Admin Dashboard Flow

```
Browser ──▶ GET /admin/dashboard
        ──▶ SecurityFilterChain: hasRole("ADMIN")? ✅
        ──▶ AdminController.showAdminDashboard()
                │
                ├── userRepository.count()              → total users
                ├── fileRepository.countActiveFiles()    → total active files
                ├── fileRepository.sumTotalFileSize()    → total storage bytes
                ├── fileRepository.getNodeStats()        → files per node
                ├── userRepository.findAll()             → all users (for table)
                ├── fileRepository.findAll()             → all files (for table)
                │
                └── Render admin.html with system metrics + management tables
```

### Why This Is Problematic
`findAll()` loads **every user and every file into memory**. With 10,000 files, the JVM loads all 10,000 `FileEntity` objects. The admin template also accesses `file.user.username`, triggering N+1 lazy-loading queries. This needs pagination and a service layer.

---

# Chapter 6 — Database Deep Dive

## 6.1 Entity-Relationship Diagram

```
┌────────────────┐       1:N        ┌────────────────┐
│     users      │───────────────▶  │     files      │
│────────────────│                  │────────────────│
│ id (PK)        │                  │ id (PK)        │
│ username       │                  │ original_name  │
│ email          │                  │ stored_name    │
│ password       │                  │ file_type      │
│ role           │                  │ file_size      │
│ version        │                  │ storage_node   │
│ created_at     │                  │ file_hash      │
└────────┬───────┘                  │ is_deleted     │
         │                          │ deleted_at     │
         │                          │ version        │
         │        1:N               │ user_id (FK)───┘
         │ ──────────────────────▶  │ folder_id (FK)─┐
         │                          │ uploaded_at    │ │
         │                          └────────────────┘ │
         │                                             │
         │       1:N        ┌────────────────┐         │
         │───────────────▶  │    folders     │ ◀───────┘
         │                  │────────────────│
         │                  │ id (PK)        │──┐
         │                  │ name           │  │ Self-reference
         │                  │ user_id (FK)   │  │ (parent → child)
         │                  │ parent_id (FK)─┘──┘
         │                  │ is_deleted     │
         │                  │ deleted_at     │
         │                  │ version        │
         │                  │ created_at     │
         │                  └────────────────┘
         │
         │       1:N        ┌────────────────┐        N:1
         └───────────────▶  │  shared_links  │ ◀──────────────┐
                            │────────────────│                │
                            │ id (PK)        │                │
                            │ token (UNIQUE) │         ┌──────┘
                            │ file_id (FK)───┘─────────│ files.id
                            │ created_by(FK) │
                            │ expires_at     │
                            │ created_at     │
                            └────────────────┘
```

### Reading the Diagram
- **1:N** means "one-to-many". One user has many files. One folder has many files.
- **FK** means "Foreign Key" — a column that references another table's primary key.
- The `folders.parent_id → folders.id` is a **self-referencing foreign key** — a folder can be inside another folder, forming a tree.

---

## 6.2 All Tables Detailed

### `users` Table

| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Auto-increment unique ID |
| `username` | `VARCHAR(50)` | `NOT NULL UNIQUE` | Login identifier |
| `email` | `VARCHAR(100)` | `NOT NULL UNIQUE` | Contact email |
| `password` | `VARCHAR(255)` | `NOT NULL` | BCrypt hash |
| `role` | `VARCHAR(20)` | `NOT NULL DEFAULT 'ROLE_USER'` | Authorization role |
| `version` | `BIGINT` | `DEFAULT 0` | Optimistic locking |
| `created_at` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP` | Registration date |

### `files` Table

| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Auto-increment unique ID |
| `original_name` | `VARCHAR(255)` | `NOT NULL` | User's filename |
| `stored_name` | `VARCHAR(255)` | `NOT NULL` | UUID filename on disk |
| `file_type` | `VARCHAR(100)` | | MIME type |
| `file_size` | `BIGINT` | | Size in bytes |
| `storage_node` | `VARCHAR(20)` | | Which node (node1/2/3) |
| `file_hash` | `VARCHAR(64)` | | SHA-256 for dedup |
| `is_deleted` | `BOOLEAN` | `DEFAULT FALSE` | Soft delete flag |
| `deleted_at` | `TIMESTAMP` | | When trashed |
| `version` | `BIGINT` | `DEFAULT 0` | Optimistic locking |
| `user_id` | `BIGINT` | `NOT NULL FK → users(id) CASCADE` | File owner |
| `folder_id` | `BIGINT` | `FK → folders(id) SET NULL` | Parent folder |
| `uploaded_at` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP` | Upload timestamp |

### `folders` Table

| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Auto-increment unique ID |
| `name` | `VARCHAR(255)` | `NOT NULL` | Folder name |
| `user_id` | `BIGINT` | `NOT NULL FK → users(id) CASCADE` | Folder owner |
| `parent_id` | `BIGINT` | `FK → folders(id) CASCADE` | Parent folder (self-ref) |
| `is_deleted` | `BOOLEAN` | `DEFAULT FALSE` | Soft delete flag |
| `deleted_at` | `TIMESTAMP` | | When trashed |
| `version` | `BIGINT` | `DEFAULT 0` | Optimistic locking |
| `created_at` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP` | Creation date |

### `shared_links` Table

| Column | Type | Constraints | Purpose |
|--------|------|-------------|---------|
| `id` | `BIGSERIAL` | `PRIMARY KEY` | Auto-increment unique ID |
| `token` | `VARCHAR(255)` | `NOT NULL UNIQUE` | UUID share token |
| `file_id` | `BIGINT` | `NOT NULL FK → files(id) CASCADE` | Shared file |
| `created_by` | `BIGINT` | `NOT NULL FK → users(id) CASCADE` | Who created the link |
| `expires_at` | `TIMESTAMP` | | Expiration time |
| `created_at` | `TIMESTAMP` | `DEFAULT CURRENT_TIMESTAMP` | Creation time |

---

## 6.3 Foreign Key Cascading Rules

| FK | ON DELETE | What Happens |
|----|-----------|-------------|
| `files.user_id → users.id` | `CASCADE` | Deleting a user deletes ALL their files |
| `files.folder_id → folders.id` | `SET NULL` | Deleting a folder moves files to root (folder_id = NULL) |
| `folders.parent_id → folders.id` | `CASCADE` | Deleting a parent folder deletes all sub-folders |
| `folders.user_id → users.id` | `CASCADE` | Deleting a user deletes ALL their folders |
| `shared_links.file_id → files.id` | `CASCADE` | Deleting a file deletes all its share links |
| `shared_links.created_by → users.id` | `CASCADE` | Deleting a user deletes all their share links |

### Why `SET NULL` for `files.folder_id`?
When a folder is deleted, its files shouldn't be deleted too — they should move to the root level. `SET NULL` means "set `folder_id` to `NULL`", which represents "no folder" (root level).

---

## 6.4 Indexes and Why They Exist

```sql
CREATE INDEX idx_files_user_id     ON files(user_id);      -- Every file listing query
CREATE INDEX idx_files_folder_id   ON files(folder_id);     -- Folder contents query
CREATE INDEX idx_files_file_hash   ON files(file_hash);     -- Deduplication lookup
CREATE INDEX idx_files_is_deleted  ON files(is_deleted);    -- Trash queries
CREATE INDEX idx_folders_user_id   ON folders(user_id);     -- Folder listing
CREATE INDEX idx_folders_parent_id ON folders(parent_id);   -- Sub-folder navigation
CREATE INDEX idx_shared_links_file_id ON shared_links(file_id);  -- Cleanup on delete
CREATE INDEX idx_shared_links_token   ON shared_links(token);    -- Share link resolution
```

### How a B-Tree Index Works (Simplified)

Without an index, finding `WHERE user_id = 5` requires scanning every row (sequential scan):
```
Row 1: user_id=3 → skip
Row 2: user_id=5 → MATCH ✅
Row 3: user_id=1 → skip
Row 4: user_id=5 → MATCH ✅
... (scan ALL remaining rows)
```

With a B-Tree index, PostgreSQL maintains a sorted lookup structure:
```
         [5]
        /   \
      [2,3] [7,9]
     /  |    |  \
  [1] [3,4] [6] [8,10]

Lookup user_id=5: go right at root → found in 2 steps (not 10,000!)
```

### Index Performance Impact

| Operation | Without Index (1M rows) | With Index (1M rows) |
|-----------|------------------------|---------------------|
| `WHERE user_id = 5` | ~1,000,000 comparisons | ~20 comparisons |
| `WHERE file_hash = 'abc...'` | ~1,000,000 comparisons | ~20 comparisons |
| `INSERT INTO files (...)` | Fast | Slightly slower (must update index) |

> **🎓 Interview Question**: "When should you NOT add an index?"
> - When the column has very low cardinality (e.g., `is_deleted` only has TRUE/FALSE — a partial index like `WHERE is_deleted = true` would be better)
> - When the table is write-heavy and rarely read
> - When the table is very small (sequential scan is faster than index lookup for < ~1000 rows)
> - Every index slows down INSERT/UPDATE because the index must be updated too

---

## 6.5 Soft Delete Strategy

### Why `deletedAt` Matters More Than `isDeleted`
The `isDeleted` boolean tells us "is this in trash?" But `deletedAt` tells us "how long has it been in trash?" — which is critical for the 30-day auto-purge.

### Query Patterns

```sql
-- Active files (what users see in /files)
WHERE user_id = ? AND is_deleted = false

-- Trashed files (what users see in /trash)
WHERE user_id = ? AND is_deleted = true

-- Files to auto-purge (trash cleanup scheduler, daily at 2 AM)
WHERE is_deleted = true AND deleted_at < NOW() - INTERVAL '30 days'
```

### Soft Delete Implications Across the System

| Feature | Impact of Soft Delete |
|---------|---------------------|
| File listing | Must filter `is_deleted = false` everywhere |
| Search | Must exclude deleted files |
| Storage quota | Must count only active files |
| Dashboard stats | Must count only active files |
| Shared links | Must check if linked file is deleted |
| Admin panel | Shows all files (including deleted) |
| Dedup count | Must count ALL references (active + deleted) before physical delete |

---

## 6.6 Deduplication Storage Model

### Example Scenario

```
User A uploads "sunset.jpg" (SHA-256 = abc123...)
User B uploads "vacation.jpg" (same photo! SHA-256 = abc123...)

Database:
┌────┬──────────────┬──────────────────────┬─────────────┬─────────┐
│ id │ original_name│ stored_name          │ file_hash   │ user_id │
├────┼──────────────┼──────────────────────┼─────────────┼─────────┤
│  1 │ sunset.jpg   │ f7a8b9c0.jpg         │ abc123...   │ 1 (A)   │
│  2 │ vacation.jpg │ f7a8b9c0.jpg ← SAME! │ abc123...   │ 2 (B)   │
└────┴──────────────┴──────────────────────┴─────────────┴─────────┘

Disk (only ONE physical copy):
storage/node2/f7a8b9c0.jpg
```

### Deletion Safety Check

```
Scenario: User A deletes "sunset.jpg" permanently

1. countByStoredName("f7a8b9c0.jpg") → returns 2
2. Since count > 1 → DO NOT delete physical file
3. Only delete the database record for User A
4. User B's "vacation.jpg" still works!

Scenario: User B also deletes "vacation.jpg" permanently

1. countByStoredName("f7a8b9c0.jpg") → returns 1 (only this record left)
2. Since count ≤ 1 → DELETE the physical file from disk
3. Delete the database record
4. Both DB record and physical file are gone
```

---

## 6.7 Optimistic Locking with @Version

### The Problem Without Locking

```
Admin 1: GET /admin/dashboard → sees User "john" with role ROLE_USER
Admin 2: GET /admin/dashboard → sees User "john" with role ROLE_USER

Admin 1: POST /admin/users/toggle-role/5 → sets role to ROLE_ADMIN ✅
Admin 2: POST /admin/users/toggle-role/5 → sets role back to ROLE_USER ✅

Result: Admin 2 unknowingly REVERTED Admin 1's change!
This is called "lost update" or "last-write-wins"
```

### The Solution: @Version

```java
@Version
private Long version;  // Starts at 0
```

Hibernate adds a version check to every UPDATE:
```sql
-- Admin 1's update:
UPDATE users SET role = 'ROLE_ADMIN', version = 1
WHERE id = 5 AND version = 0;
-- Returns: 1 row updated ✅

-- Admin 2's update (happens later):
UPDATE users SET role = 'ROLE_USER', version = 1
WHERE id = 5 AND version = 0;
-- Returns: 0 rows updated (version is now 1, not 0!)
-- → Hibernate throws OptimisticLockingFailureException
```

Admin 2 gets an error message: "This record was modified by another user. Please refresh and try again."

### Optimistic vs Pessimistic Locking

| Aspect | Optimistic (CloudNest) | Pessimistic |
|--------|----------------------|-------------|
| Strategy | Detect conflicts at write time | Prevent conflicts by locking rows |
| Implementation | `@Version` field | `SELECT ... FOR UPDATE` |
| Performance | ✅ No locks, high concurrency | ❌ Locks reduce concurrency |
| Conflict handling | Retry on conflict | Wait for lock release |
| Best for | Low-conflict scenarios | High-conflict scenarios |

---

## 6.8 PostgreSQL-Specific Features Used

### BIGSERIAL
```sql
id BIGSERIAL PRIMARY KEY
```
Equivalent to:
```sql
id BIGINT NOT NULL DEFAULT nextval('table_id_seq')
```
Auto-creates a sequence and uses it for auto-incrementing. Supports up to 9.2 quintillion values.

### COALESCE for Null Safety
```sql
SELECT COALESCE(SUM(file_size), 0) FROM files WHERE user_id = ? AND is_deleted = false
```
If there are no files (SUM returns NULL), COALESCE returns 0 instead. Prevents NPE in Java.

### ILIKE for Case-Insensitive Search
```sql
WHERE LOWER(original_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
```
PostgreSQL's `ILIKE` would be cleaner, but the current implementation uses `LOWER()` for cross-database compatibility.

---

## 6.9 Database Anti-Patterns Found in CloudNest

| Anti-Pattern | Where | Problem | Fix |
|-------------|-------|---------|-----|
| **No FK indexes** | All FK columns | Slow JOINs and WHERE clauses | Add indexes on all FK columns |
| **N+1 queries** | AdminController | Lazy-loads user for each file | Use `JOIN FETCH` or DTO projection |
| **Load all into memory** | `findAll()` calls | OutOfMemoryError at scale | Use pagination (`Pageable`) |
| **LIKE with wildcards** | Search queries | `%term%` can't use indexes | Full-text search with `tsvector` |
| **No partial indexes** | `is_deleted` column | Full index on boolean column is wasteful | `CREATE INDEX ... WHERE is_deleted = true` |
| **ddl-auto=update** | application.properties | Can't track schema changes | Use Flyway or Liquibase |

---

## 6.10 How to Read the Database Directly

### Connect via Command Line
```bash
psql -h localhost -U postgres -d cloudnest_db
```

### Useful Diagnostic Queries
```sql
-- How many files per user?
SELECT u.username, COUNT(f.id) as file_count
FROM users u LEFT JOIN files f ON u.id = f.user_id AND f.is_deleted = false
GROUP BY u.username;

-- Which files are deduplicated?
SELECT stored_name, COUNT(*) as reference_count
FROM files WHERE is_deleted = false
GROUP BY stored_name HAVING COUNT(*) > 1;

-- Storage used per node
SELECT storage_node, COUNT(*) as files, SUM(file_size) as total_bytes
FROM files WHERE is_deleted = false
GROUP BY storage_node;

-- Expired trash items (should be auto-purged)
SELECT id, original_name, deleted_at,
       NOW() - deleted_at as time_in_trash
FROM files WHERE is_deleted = true
ORDER BY deleted_at;

-- Active shared links
SELECT sl.token, f.original_name, u.username, sl.expires_at,
       CASE WHEN sl.expires_at < NOW() THEN 'EXPIRED' ELSE 'ACTIVE' END as status
FROM shared_links sl
JOIN files f ON sl.file_id = f.id
JOIN users u ON sl.created_by = u.id;
```

---

## 5.9 Folder Creation Flow

```
Browser ──▶ POST /folders/create (name + parentId + CSRF)
                │
                ▼
        SecurityFilterChain: authenticated? ✅
                │
                ▼
        FolderController.createFolder()
                │
                ├── UserService.findByUsername(principal.getName())
                │       └── UserRepository → PostgreSQL → return User entity
                │
                ▼
        FolderService.createFolder(name, user, parentId)
                │
                ├── 1. Validate: folder name not empty/blank
                │
                ├── 2. Resolve parent (if parentId provided)
                │       └── folderRepository.findById(parentId)
                │           ├── NOT FOUND → throw "Parent folder not found"
                │           └── FOUND → verify parent.user == current user
                │                       (ownership check — prevents IDOR)
                │
                ├── 3. Duplicate check: does a folder with this name
                │       already exist under the same parent for this user?
                │       └── folderRepository.findByNameAndUserAndParent(...)
                │           ├── EXISTS → throw "Folder already exists"
                │           └── NOT EXISTS → continue ✅
                │
                ├── 4. Build Folder entity:
                │       Folder.builder()
                │           .name(name)
                │           .user(user)
                │           .parent(parentFolder)  // null for root-level
                │           .build()
                │
                └── 5. folderRepository.save(folder)
                │       └── Hibernate → INSERT INTO folders (name, user_id, parent_id, ...)
                │
                ▼
        Controller: redirect to /files (or /files?folderId=parentId)
                │
                ▼
        Browser ◀── File listing page with success flash message
```

### What Happens at Each Layer

| Step | Layer | What Happens | Could Fail Because |
|------|-------|-------------|-------------------|
| 1 | Browser | User types folder name, clicks "Create" | Empty name |
| 2 | Security | CSRF validated, user authenticated | Invalid session |
| 3 | Controller | Extract form params, call service | Malformed request |
| 4 | Service | Validate name, resolve parent, check duplicates | Duplicate name, parent not found, IDOR |
| 5 | Repository | Save to database | Constraint violation |
| 6 | Database | INSERT statement | Connection error |
| 7 | Controller | Redirect with success message | — |

### Redirect Logic
```java
// If folder was created inside another folder, redirect back to that folder
if (parentId != null) {
    return "redirect:/files?folderId=" + parentId;
}
// Otherwise redirect to root-level file listing
return "redirect:/files";
```
This keeps the user in context — they stay in the folder where they just created a sub-folder.

---

## 5.10 Admin Role Change Flow

```
Browser ──▶ POST /admin/users/toggle-role/{id} (CSRF token)
                │
                ▼
        SecurityFilterChain:
                ├── authenticated? ✅
                └── hasRole("ADMIN")? ✅ (required by SecurityConfig for /admin/**)
                │
                ▼
        AdminController.toggleUserRole(id, principal)
                │
                ├── 1. Resolve the current admin:
                │       userRepository.findByUsername(principal.getName())
                │       → currentUser (the admin performing the action)
                │
                ├── 2. Resolve the target user:
                │       userRepository.findById(id)
                │       ├── NOT FOUND → redirect with error: "User not found"
                │       └── FOUND → targetUser
                │
                ├── 3. Self-demotion guard:
                │       if (currentUser.id == targetUser.id)
                │           → redirect with error: "You cannot demote your own account"
                │           (Prevents the last admin from accidentally losing access)
                │
                ├── 4. Toggle the role:
                │       if (targetUser.role == "ROLE_ADMIN")
                │           → targetUser.setRole("ROLE_USER")   // Demote
                │       else
                │           → targetUser.setRole("ROLE_ADMIN")  // Promote
                │
                └── 5. userRepository.save(targetUser)
                        │
                        └── Hibernate generates:
                            UPDATE users
                            SET role = 'ROLE_ADMIN', version = version + 1
                            WHERE id = 5 AND version = 0
                │
                ▼
        redirect: /admin/dashboard (with success flash message)
                │
                ▼
        Browser ◀── Admin panel with updated user list
```

### Optimistic Locking in Action (@Version)

When two admins try to change the same user's role simultaneously:

```
Timeline:
┌──────────────┬──────────────────────────────────────────────────────────────┐
│ Admin Alice  │ GET /admin/dashboard → sees "john" (version=0, ROLE_USER)   │
│ Admin Bob    │ GET /admin/dashboard → sees "john" (version=0, ROLE_USER)   │
│              │                                                              │
│ Admin Alice  │ POST toggle-role/5 → UPDATE ... SET version=1 WHERE version=0│
│              │   → ✅ Success: 1 row updated. John is now ROLE_ADMIN       │
│              │                                                              │
│ Admin Bob    │ POST toggle-role/5 → UPDATE ... SET version=1 WHERE version=0│
│              │   → ❌ FAIL: 0 rows updated (version is now 1, not 0!)      │
│              │   → OptimisticLockingFailureException                        │
│              │   → "This record was modified. Please refresh."              │
└──────────────┴──────────────────────────────────────────────────────────────┘
```

### Audit Finding ⚠️
The `AdminController` injects `UserRepository` directly instead of going through `UserService`. In a refactored version, this would be `AdminService.toggleUserRole()`.

> **🎓 Interview Question**: "How do you handle concurrent updates in your project?" → "We use optimistic locking with Hibernate's `@Version` annotation. Every UPDATE includes a `WHERE version = ?` clause. If the version doesn't match (another update happened first), Hibernate throws `OptimisticLockingFailureException` and we ask the user to refresh."

---

> **📖 Continue to Part 4** → `Learning_Guide_Part4_SpringBoot_and_Security.md`
