# ☁️ CloudNest — Master Learning Guide — Part 4

> **Chapters 7–8: Spring Boot Concepts & Security Deep Dive**
> Level: 🟡 Intermediate to 🔴 Advanced

---

# Chapter 7 — Spring Boot Concepts Used

## 7.1 Dependency Injection & IoC Container

### The Problem (Without DI)
```java
// BAD: Creating dependencies manually
public class FileController {
    private FileStorageService fileService = new FileStorageService(
        new FileRepository(),      // How do you create this?
        new FolderRepository(),    // And this?
        new StorageNodeService(new AppConfig())  // And this chain?
    );
}
```

### The Solution (With DI)
```java
// GOOD: Spring injects dependencies automatically
public class FileController {
    private final FileStorageService fileStorageService;

    // Spring sees this constructor and automatically provides the beans
    public FileController(FileStorageService fileStorageService, ...) {
        this.fileStorageService = fileStorageService;
    }
}
```

### How IoC (Inversion of Control) Works

**Without IoC**: Your code creates objects → You control the lifecycle  
**With IoC**: Spring creates objects → The framework controls the lifecycle

```
                    ┌─────────────────────────────────┐
                    │      Spring IoC Container        │
                    │                                  │
                    │  Creates and manages beans:      │
                    │  ┌─────────────────────────┐     │
                    │  │ UserService              │     │
                    │  │ FileStorageService       │     │
                    │  │ FolderService            │     │
                    │  │ UserRepository (proxy)   │     │
                    │  │ FileRepository (proxy)   │     │
                    │  │ PasswordEncoder          │     │
                    │  │ SecurityFilterChain      │     │
                    │  │ ...                      │     │
                    │  └─────────────────────────┘     │
                    │                                  │
                    │  Wires dependencies:              │
                    │  UserService needs:               │
                    │    ← UserRepository               │
                    │    ← PasswordEncoder               │
                    │  FileController needs:             │
                    │    ← FileStorageService             │
                    │    ← FolderService                  │
                    │    ← UserService                    │
                    └─────────────────────────────────┘
```

## 7.2 Key Annotations in CloudNest

### Stereotype Annotations (Bean Detection)

| Annotation | Purpose | Used On |
|-----------|---------|---------|
| `@Controller` | HTTP request handler (returns template names) | `AuthController`, `FileController`, etc. |
| `@Service` | Business logic layer | `UserService`, `FileStorageService`, etc. |
| `@Repository` | Data access layer | `UserRepository`, `FileRepository`, etc. |
| `@Component` | Generic Spring-managed bean | `TrashCleanupScheduler` |
| `@Configuration` | Defines beans and settings | `SecurityConfig`, `AppConfig` |

All of these are specializations of `@Component`. The real difference is **intent**: they tell developers (and Spring) what role the class plays.

### Configuration Annotations

| Annotation | Purpose | Example |
|-----------|---------|---------|
| `@Bean` | Creates a bean from a method's return value | `passwordEncoder()` in `SecurityConfig` |
| `@Value("${prop}")` | Injects a property value | `@Value("${cloudnest.storage.base-path}")` |
| `@PostConstruct` | Runs once after bean creation | `AppConfig.initStorageDirectories()` |
| `@EnableScheduling` | Enables `@Scheduled` methods | `CloudNestApplication` |

### JPA Annotations

| Annotation | Purpose | Example |
|-----------|---------|---------|
| `@Entity` | "This class is a database table" | `User`, `FileEntity`, `Folder` |
| `@Table(name = "...")` | Specifies the table name | `@Table(name = "users")` |
| `@Id` | "This field is the primary key" | `private Long id;` |
| `@GeneratedValue` | "Auto-increment this field" | `strategy = GenerationType.IDENTITY` |
| `@Column` | Column constraints | `@Column(nullable = false, unique = true)` |
| `@ManyToOne` | Many records → one parent | `FileEntity.user` → `User` |
| `@OneToMany` | One parent → many children | `User.files` → `List<FileEntity>` |
| `@JoinColumn` | Specifies the FK column name | `@JoinColumn(name = "user_id")` |
| `@Version` | Optimistic locking field | `private Long version;` |
| `@CreationTimestamp` | Auto-set on INSERT | `private LocalDateTime createdAt;` |

### `@Transactional` — Database Transaction Management

```java
@Service
@Transactional  // All methods in this class run inside a transaction
public class FileStorageService {
    public FileDto uploadFile(...) {
        // If ANY of these operations fail, ALL changes are rolled back
        fileRepository.save(entity);    // 1. Save metadata
        folderRepository.save(folder);  // 2. Update folder
        // If step 2 fails, step 1 is UNDONE automatically
    }
}
```

**Why this matters**: Without `@Transactional`, if step 1 succeeds but step 2 fails, you'd have inconsistent data — a file record without a proper folder reference. Transactions ensure **atomicity** (all or nothing).

> **🎓 Interview Question**: "What isolation level does `@Transactional` use by default?" → `READ_COMMITTED` (PostgreSQL default). This means you won't see uncommitted changes from other transactions, but you might see different results if you read the same row twice in the same transaction (non-repeatable read).

## 7.3 How `@Autowired` vs Constructor Injection Work

CloudNest uses **constructor injection** everywhere (recommended best practice):

```java
// Constructor injection (PREFERRED)
public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
}
```

Why not `@Autowired`?
```java
// Field injection (NOT recommended)
@Autowired
private UserRepository userRepository;
```

| Aspect | Constructor Injection | Field Injection |
|--------|---------------------|-----------------|
| Immutability | ✅ Fields can be `final` | ❌ Must be mutable |
| Testability | ✅ Easy to pass mocks | ❌ Requires reflection |
| Null safety | ✅ Fails at startup if missing | ❌ Fails at runtime (NPE) |
| Explicit deps | ✅ Clear in constructor | ❌ Hidden |

---

# Chapter 8 — Security Deep Dive

## 8.1 Authentication Flow (Complete)

```
1. Browser sends: POST /login (username=john, password=secret, _csrf=abc123)

2. CsrfFilter: Is the CSRF token valid?
   ├── YES → continue
   └── NO  → 403 Forbidden

3. UsernamePasswordAuthenticationFilter: Extract credentials from request
   → Create UsernamePasswordAuthenticationToken(username, password)

4. AuthenticationManager delegates to DaoAuthenticationProvider

5. DaoAuthenticationProvider calls: UserService.loadUserByUsername("john")
   → UserRepository.findByUsernameIgnoreCaseOrEmailIgnoreCase("john", "john")
   → Returns UserDetails(username="john", password="$2a$12$...", role="ROLE_USER")

6. DaoAuthenticationProvider calls: BCryptPasswordEncoder.matches("secret", "$2a$12$...")
   ├── MATCH    → Authentication SUCCESS
   └── NO MATCH → Authentication FAILURE → redirect /login?error

7. On SUCCESS:
   → Create SecurityContext with authenticated principal
   → Create HTTP session, assign JSESSIONID cookie
   → redirect /dashboard

8. Subsequent requests:
   → Browser sends JSESSIONID cookie
   → SessionManagementFilter looks up session on server
   → Restores SecurityContext from session
   → User is "remembered" without re-authenticating
```

## 8.2 Authorization

### How `hasRole("ADMIN")` Works

When `SecurityConfig` says:
```java
.requestMatchers("/admin/**").hasRole("ADMIN")
```

Spring Security:
1. Checks the authenticated user's authorities
2. Looks for `ROLE_ADMIN` (Spring prepends "ROLE_" to the role name)
3. Since `User.role` already stores `"ROLE_ADMIN"`, it matches directly

### Current Role System

| Role | Access |
|------|--------|
| `ROLE_USER` | Dashboard, files, folders, trash, share links |
| `ROLE_ADMIN` | Everything above + `/admin/**` (user management, global file deletion) |

## 8.3 CSRF Protection

### What is CSRF?
**Cross-Site Request Forgery**: An attacker tricks a logged-in user into making an unwanted request.

**Example Attack**: You're logged into CloudNest. You visit a malicious site that has:
```html
<img src="https://cloudnest.com/files/delete/1" />
```
Your browser sends the request WITH your session cookie, deleting your file!

### How CloudNest Prevents It
Spring Security generates a **CSRF token** for each session. Every POST form must include this token:
```html
<form th:action="@{/files/delete/{id}(id=${file.id})}" method="post">
    <!-- Thymeleaf automatically includes a hidden CSRF token field -->
    <input type="hidden" name="_csrf" value="abc123-random-token" />
    <button type="submit">Delete</button>
</form>
```

The attacker's malicious site doesn't know the CSRF token, so their forged request is rejected by the `CsrfFilter` before it even reaches the controller.

## 8.4 Security Weaknesses in the Current Project

| Weakness | Risk | Mitigation |
|----------|------|------------|
| No rate limiting | Brute-force login attacks | Add `bucket4j-spring-boot-starter` |
| No account lockout | Unlimited login attempts | `AuthenticationFailureHandler` with counter |
| No 2FA | Account compromise if password leaked | Add TOTP (Google Authenticator) |
| Session fixation | Possible in older configs | Spring Security handles by default (new session on login) |
| No HTTPS enforcement | Passwords sent in plain text | Configure SSL/TLS in production |
| No password complexity rules | Weak passwords allowed | Add `@Pattern` validation |
| `Content-Disposition` header injection | Malicious filenames | Sanitize `originalName` before using in headers |

## 8.5 How Enterprise Systems Secure File Storage

| Practice | CloudNest | Google Drive | Enterprise Standard |
|----------|-----------|-------------|-------------------|
| Encryption at rest | ❌ None | ✅ AES-256 | ✅ AES-256 + KMS |
| Encryption in transit | ❌ HTTP | ✅ HTTPS/TLS | ✅ TLS 1.3 |
| Access control | RBAC (2 roles) | ACL per file | RBAC + ABAC + ACLs |
| Audit logging | ❌ None | ✅ Full audit trail | ✅ + SIEM integration |
| Data residency | Single server | Regional data centers | Geo-fencing |
| Virus scanning | ❌ None | ✅ On upload | ✅ ClamAV or similar |
| DLP (Data Loss Prevention) | ❌ None | ✅ Built-in | ✅ Content inspection |

---

> **📖 Continue to Part 5** → `Learning_Guide_Part5_Production_and_BugAudit.md`
