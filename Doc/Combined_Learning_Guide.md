# ☁️ CloudNest — Master Learning Documentation

> **From Zero to Senior: A Complete Engineering Guide to Your Cloud Storage System**
>
> Written for: Anmol Raj — Student Developer
> Perspective: Senior Engineer Mentoring a Junior
> Scope: Beginner → Advanced, covering every file, every annotation, every flow
> Last Updated: May 2026

---

# 📚 Document Map (6 Parts)

| Part | File | Chapters | Level |
|------|------|----------|-------|
| **→ Part 1** | `Learning_Guide_Part1_Introduction_and_TechStack.md` | 1–2 | 🟢 Beginner |
| Part 2 | `Learning_Guide_Part2_Folder_and_File_Walkthrough.md` | 3–4 | 🟡 Intermediate |
| Part 3 | `Learning_Guide_Part3_Request_Flows_and_Database.md` | 5–6 | 🟡 Intermediate |
| Part 4 | `Learning_Guide_Part4_SpringBoot_and_Security.md` | 7–8 | 🟡–🔴 |
| Part 5 | `Learning_Guide_Part5_Production_and_BugAudit.md` | 9–10 | 🔴 Advanced |
| Part 6 | `Learning_Guide_Part6_Refactoring_DevOps_Lessons.md` | 11–13 | 🔴 Advanced |

---

# Chapter 1 — Project Introduction

## 1.1 What is CloudNest?

CloudNest is a **cloud file storage application** — think of it as your own personal mini Google Drive. It allows users to:

- 📤 **Upload** files to a server
- 📁 **Organize** files into folders (with nesting)
- 📥 **Download** files back to their computer
- 🔗 **Share** files via public links (like Google Drive's "anyone with the link")
- 🗑️ **Soft-delete** files (move to trash) with 30-day auto-purge
- 🔍 **Search** files by name or type
- 👤 **Authenticate** users with secure login/registration
- 🛡️ **Authorize** with role-based access (User vs Admin)
- 💾 **Deduplicate** files to save storage space (same content = same physical file)
- 🖥️ **Simulate distributed storage** across multiple nodes

### The Analogy 🏠

Imagine you're building an apartment building (CloudNest):
- Each **tenant** (User) gets their own storage unit
- Each unit has **shelves** (Folders) that can contain **boxes** (Files)
- Tenants can **share a key** (SharedLink) to let visitors access specific boxes
- A **building manager** (Admin) can see all units and manage tenants
- The building has **three storage rooms** (Node1, Node2, Node3) where boxes are physically kept
- If two tenants store identical items, the building only keeps **one physical copy** (Deduplication)

## 1.2 Business Purpose

In a real-world context, CloudNest solves these problems:

| Problem | CloudNest Solution |
|---------|-------------------|
| Users need to store files online | Secure upload/download system |
| Files need to be organized | Hierarchical folder system |
| Files need to be shared | Token-based public links with expiry |
| Storage space is expensive | SHA-256 deduplication saves disk space |
| Data loss risk | Soft-delete with 30-day trash retention |
| Multiple servers needed | Simulated distributed storage across nodes |
| Security concerns | BCrypt passwords, session auth, CSRF, role-based access |

## 1.3 System Goals

1. **Security First**: Never store plain-text passwords. Verify ownership on every operation.
2. **Data Integrity**: Transactional operations. Soft-delete before permanent delete.
3. **Scalability Awareness**: Demonstrate distributed storage concepts (even if simulated).
4. **Clean Architecture**: Layered design — Controller → Service → Repository → Database.
5. **User Experience**: Modern UI with file previews, search, and organized navigation.

## 1.4 Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                      BROWSER (CLIENT)                    │
│  ┌─────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │
│  │ Login   │  │Dashboard │  │  Files   │  │  Admin   │ │
│  │ Page    │  │  Page    │  │  Page    │  │  Panel   │ │
│  └────┬────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘ │
└───────┼────────────┼────────────┼────────────┼──────────┘
        │            │            │            │
        ▼            ▼            ▼            ▼
┌─────────────────────────────────────────────────────────┐
│              SPRING BOOT APPLICATION                     │
│                                                          │
│  ┌──────────────────────────────────────────────────┐   │
│  │            SECURITY FILTER CHAIN                  │   │
│  │  (Authentication, Authorization, CSRF, Sessions)  │   │
│  └──────────────────────┬───────────────────────────┘   │
│                         │                                │
│  ┌──────────────────────▼───────────────────────────┐   │
│  │               CONTROLLERS (Web Layer)             │   │
│  │  AuthController | FileController | AdminController│   │
│  │  DashboardController | FolderController           │   │
│  │  ShareController | TrashController                │   │
│  └──────────────────────┬───────────────────────────┘   │
│                         │                                │
│  ┌──────────────────────▼───────────────────────────┐   │
│  │               SERVICES (Business Logic)           │   │
│  │  UserService | FileStorageService | FolderService │   │
│  │  SharedLinkService | StorageNodeService           │   │
│  │  TrashCleanupScheduler                            │   │
│  └──────────────────────┬───────────────────────────┘   │
│                         │                                │
│  ┌──────────────────────▼───────────────────────────┐   │
│  │             REPOSITORIES (Data Access)            │   │
│  │  UserRepository | FileRepository                  │   │
│  │  FolderRepository | SharedLinkRepository          │   │
│  └──────────────────────┬───────────────────────────┘   │
│                         │                                │
└─────────────────────────┼────────────────────────────────┘
                          │
           ┌──────────────┼──────────────┐
           │              │              │
     ┌─────▼─────┐  ┌────▼────┐  ┌──────▼──────┐
     │ PostgreSQL │  │ Node 1  │  │  Node 2/3   │
     │  Database  │  │ storage/│  │  storage/   │
     │  (Metadata)│  │ node1/  │  │  node2/3/   │
     └───────────┘  └─────────┘  └─────────────┘
```

### How This Compares to Real Companies

| Feature | CloudNest | Google Drive | Dropbox |
|---------|-----------|-------------|---------|
| Storage | Local filesystem | Google Cloud Storage | Amazon S3 |
| Database | PostgreSQL | Spanner | MySQL + custom |
| Auth | Session + BCrypt | OAuth2 + 2FA | OAuth2 + 2FA |
| Dedup | SHA-256 hash compare | Block-level dedup | Block-level dedup |
| Distribution | 3 simulated nodes | Thousands of servers | Global CDN |
| Sharing | UUID token links | ACL + link sharing | ACL + link sharing |
| Search | LIKE queries | Full-text + ML | Full-text + ML |

> **💡 Interview Gold**: When asked "How does your project compare to real systems?", acknowledge the scale difference but emphasize that the **architectural patterns are identical** — the difference is only in implementation scale, not design philosophy.

---

# Chapter 2 — Full Tech Stack Explanation

## 2.1 Java 21

### What It Is
Java is a **compiled, statically-typed, object-oriented programming language**. Version 21 is a **Long-Term Support (LTS)** release, meaning it receives security updates for years.

### Why We Used It
- Industry standard for enterprise backend development
- Strong type system catches bugs at compile time
- Massive ecosystem of libraries and frameworks
- Java 21 LTS means long-term stability and support
- Required by most enterprise job listings

### Where It Appears in This Project
**Everywhere.** Every `.java` file in `src/main/java/` is Java code. The version is specified in [pom.xml](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/pom.xml) line 38:
```xml
<java.version>21</java.version>
```

### Key Java 21 Features Used in CloudNest
| Feature | Example in CloudNest |
|---------|---------------------|
| Pattern Matching | `if (ex instanceof AccessDeniedException ad)` in `GlobalExceptionHandler` |
| Streams API | `files.stream().map(this::convertToDto).collect(Collectors.toList())` |
| Optional | `Optional<User> findByUsername(...)` in repositories |
| Lambda Expressions | `.orElseThrow(() -> new FileNotFoundException(...))` |
| Text Blocks | Could be used for multi-line SQL queries |

### Advantages
- Battle-tested in production at companies like Netflix, Amazon, Google
- JVM garbage collection handles memory management
- Write once, run anywhere (JVM is cross-platform)
- Excellent IDE support (IntelliJ IDEA)

### Disadvantages
- Verbose compared to Python or Kotlin (lots of boilerplate)
- Slow startup compared to Go or Rust
- Memory-hungry JVM

### Alternatives
| Language | Pros | Cons |
|----------|------|------|
| **Kotlin** | Less verbose, modern, JVM-compatible | Smaller community than Java |
| **Go** | Fast compilation, simple syntax | No generics until recently, less mature frameworks |
| **Python + Django** | Faster development | Slower runtime, dynamic typing |
| **Node.js + Express** | JavaScript everywhere | Loses type safety, callback complexity |

> **🎓 Things interviewers ask**: "Why Java over Python for a backend?" → Java's static typing catches bugs at compile time, it performs better under load (JIT compilation), and it has mature enterprise frameworks like Spring Boot. Python is better for rapid prototyping and data science.

---

## 2.2 Spring Boot

### What It Is
Spring Boot is a **framework that simplifies building Java applications**. Think of it as a "starter kit" — it provides default configurations so you don't have to wire everything manually.

### The Analogy 🏗️
If Java is the **raw building material** (bricks, cement), then Spring Boot is the **prefabricated house kit**. You still build the house, but the foundation, plumbing, and electrical wiring come pre-assembled.

### Why We Used It
- Auto-configures 80% of what you need
- Embedded Tomcat server (no separate server setup)
- Massive community and documentation
- Industry standard for Java web applications
- Version used: **3.4.5** (specified in `pom.xml` line 25)

### Where It Appears
The entry point is [CloudNestApplication.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/CloudNestApplication.java):
```java
@SpringBootApplication  // This one annotation does EVERYTHING
@EnableScheduling       // Enables the TrashCleanupScheduler
public class CloudNestApplication {
    public static void main(String[] args) {
        SpringApplication.run(CloudNestApplication.class, args);
    }
}
```

### How `@SpringBootApplication` Works Internally
This single annotation is actually **three annotations combined**:

```
@SpringBootApplication
├── @Configuration        → "This class can define beans"
├── @EnableAutoConfiguration → "Auto-configure based on dependencies in pom.xml"
└── @ComponentScan        → "Scan this package and all sub-packages for Spring components"
```

When you call `SpringApplication.run()`:
1. Spring scans `com.cloudnest` and all sub-packages
2. Finds all `@Controller`, `@Service`, `@Repository`, `@Configuration` classes
3. Creates instances (beans) of each
4. Wires them together (dependency injection)
5. Starts an embedded Tomcat server on port 8080
6. Begins listening for HTTP requests

### Advantages
- Convention over configuration — sensible defaults
- Rapid development
- Production-ready features (health checks, metrics)
- Massive ecosystem of "starters" for common needs

### Disadvantages
- "Magic" — hard to debug auto-configuration issues
- Heavy memory footprint
- Startup time can be slow for large apps
- Learning curve for understanding what happens behind the scenes

---

## 2.3 Spring MVC (Model-View-Controller)

### What It Is
Spring MVC is the **web framework** inside Spring Boot that handles HTTP requests. It follows the **MVC design pattern**:

```
┌───────────┐     ┌────────────┐     ┌──────────┐     ┌──────────┐
│  Browser  │────▶│ Controller │────▶│  Service  │────▶│  Model   │
│ (Request) │     │ (Routing)  │     │ (Logic)   │     │ (Data)   │
└───────────┘     └─────┬──────┘     └──────────┘     └──────────┘
                        │
                        ▼
                  ┌──────────┐     ┌───────────┐
                  │  Model   │────▶│   View    │────▶ Browser (HTML)
                  │ (Data)   │     │(Thymeleaf)│
                  └──────────┘     └───────────┘
```

- **Model**: Data passed to the template (e.g., `model.addAttribute("files", fileList)`)
- **View**: The HTML template (e.g., `dashboard.html`)
- **Controller**: The Java class that handles the request (e.g., `DashboardController`)

### Where It Appears
Every class annotated with `@Controller` in the `controller` package:
- [AuthController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/controller/AuthController.java) — Login/Registration pages
- [DashboardController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/controller/DashboardController.java) — Dashboard with stats
- [FileController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/controller/FileController.java) — File CRUD
- [FolderController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/controller/FolderController.java) — Folder CRUD
- [ShareController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/controller/ShareController.java) — File sharing
- [TrashController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/controller/TrashController.java) — Trash management
- [AdminController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/controller/AdminController.java) — Admin panel

### `@Controller` vs `@RestController`

| Annotation | Returns | Used For |
|-----------|---------|----------|
| `@Controller` | Template name (String) | Server-side rendered pages (Thymeleaf) |
| `@RestController` | Data (JSON/XML) | REST APIs (for SPAs, mobile apps) |

CloudNest uses `@Controller` because we render HTML pages with Thymeleaf. If we were building an API for a React frontend, we'd use `@RestController`.

---

## 2.4 Spring Security

### What It Is
Spring Security is a **framework for authentication (who are you?) and authorization (what can you do?)**. It intercepts every HTTP request before it reaches your controller.

### The Analogy 🏢
Think of Spring Security as the **building security system**:
- The **front door** checks your ID badge (authentication)
- Different floors have **different access levels** (authorization)
- Some areas are **public** (lobby = login page, shared links)
- Most areas require **employee badge** (authenticated user)
- The **top floor** is **executives only** (admin panel = ROLE_ADMIN)

### Where It Appears
[SecurityConfig.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/security/SecurityConfig.java) defines all security rules.

### Why Session-Based Auth (Not JWT)
CloudNest uses **server-side sessions**, not JWT tokens. Here's why:

| Aspect | Session Auth (CloudNest) | JWT Auth |
|--------|-------------------------|----------|
| Storage | Server-side (JSESSIONID cookie) | Client-side (token in localStorage) |
| Best for | Server-rendered pages (Thymeleaf) | SPAs (React, Angular) + APIs |
| Logout | Destroy session on server | Hard — must blacklist token |
| Simplicity | ✅ Simple with Spring Security | ❌ Complex token management |
| Scalability | ❌ Session sticky (unless Redis) | ✅ Stateless, scales easily |

---

## 2.5 Spring Data JPA

### What It Is
Spring Data JPA is a **layer on top of JPA (Java Persistence API)** that automates database operations. You declare an interface, and Spring generates the implementation at runtime.

### The Analogy 📋
Imagine you hire a personal assistant. Instead of writing SQL queries yourself, you just say:
- "Find me the user with username 'john'" → `findByUsername("john")`
- "Count all files that aren't deleted for this user" → `countByUserAndIsDeletedFalse(user)`

The assistant (Spring Data JPA) knows SQL and translates your request automatically.

### Where It Appears
All interfaces in the `repository` package:
- [UserRepository.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/repository/UserRepository.java)
- [FileRepository.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/repository/FileRepository.java)
- [FolderRepository.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/repository/FolderRepository.java)
- [SharedLinkRepository.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/repository/SharedLinkRepository.java)

### Query Derivation — How Method Names Become SQL

```java
// You write:
List<FileEntity> findByUserAndIsDeletedFalseOrderByUploadedAtDesc(User user);

// Spring generates:
// SELECT * FROM files
// WHERE user_id = ? AND is_deleted = false
// ORDER BY uploaded_at DESC
```

The method name is parsed as:
```
findBy          → SELECT ... WHERE
User            → user_id = ?
And             → AND
IsDeletedFalse  → is_deleted = false
OrderBy         → ORDER BY
UploadedAt      → uploaded_at
Desc            → DESC
```

> **🎓 Interview Gold**: When asked "How does Spring Data JPA generate queries?", explain that Spring parses the method name at startup using a naming convention. It splits the name at keywords like `And`, `Or`, `OrderBy`, `Between`, etc. and builds a JPA Criteria query. This happens in `PartTreeJpaQuery.class`.

---

## 2.6 Hibernate (ORM)

### What It Is
Hibernate is the **ORM (Object-Relational Mapping)** implementation used by JPA. It translates between Java objects and database tables.

### The Analogy 🗺️
Hibernate is like a **translator** between two languages:
- **Java speaks** in objects: `User user = new User(); user.setUsername("john");`
- **PostgreSQL speaks** in tables: `INSERT INTO users (username) VALUES ('john');`

Hibernate translates between these two worlds.

### Key Hibernate Concepts in CloudNest

| Concept | How It's Used |
|---------|--------------|
| `@Entity` | Marks a class as a database table |
| `@Table(name = "users")` | Specifies the table name |
| `@Id` + `@GeneratedValue` | Auto-incrementing primary key |
| `@Column` | Maps a field to a column |
| `@ManyToOne` / `@OneToMany` | Defines relationships between tables |
| `FetchType.LAZY` | Don't load related data until accessed |
| `@CreationTimestamp` | Auto-set timestamp when row is created |
| `@Version` | Optimistic locking for concurrency |
| `ddl-auto=update` | Hibernate auto-creates/updates tables |

### `ddl-auto` Modes Explained

| Mode | Behavior | When to Use |
|------|----------|-------------|
| `create` | Drop all tables, create new ones | Never in production |
| `create-drop` | Same as create, but drops on shutdown | Unit tests |
| `update` | Add new columns/tables, never removes | Development only |
| `validate` | Just check that schema matches entities | **Production** |
| `none` | Do nothing | Production with Flyway |

CloudNest uses `update` (set via `DDL_AUTO` env var), which is fine for development but dangerous for production because it can create columns but never removes or renames them.

---

## 2.7 PostgreSQL

### What It Is
PostgreSQL (Postgres) is an **open-source relational database management system (RDBMS)**. It's considered the most advanced open-source database.

### Why We Used It (Instead of MySQL)
| Feature | PostgreSQL | MySQL |
|---------|-----------|-------|
| ACID compliance | ✅ Full | ⚠️ Depends on engine |
| JSON support | ✅ Excellent (JSONB) | ⚠️ Basic |
| Full-text search | ✅ Built-in | ❌ Need plugin |
| Concurrency | MVCC (excellent) | Lock-based |
| Cloud support | ✅ All clouds | ✅ All clouds |
| Used by | Instagram, Reddit, Spotify | Facebook, Uber, Airbnb |

### Where It Appears
- Connection URL in [application.properties](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/resources/application.properties) line 14: `jdbc:postgresql://localhost:5432/cloudnest_db`
- Schema definition in [schema.sql](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/schema.sql) — uses PostgreSQL-specific syntax (`BIGSERIAL`, `BOOLEAN`)
- Hibernate dialect in `application.properties` line 25: `org.hibernate.dialect.PostgreSQLDialect`

---

## 2.8 Thymeleaf

### What It Is
Thymeleaf is a **server-side Java template engine**. It takes HTML files and replaces special `th:` attributes with dynamic data from your Java code.

### The Analogy 📝
Think of Thymeleaf as **Mad Libs**. You write an HTML page with blanks, and Thymeleaf fills in the blanks with data from the server:

```html
<!-- Template (with blanks) -->
<h1 th:text="${username}">Placeholder Name</h1>
<p>You have <span th:text="${dashboard.totalFiles}">0</span> files.</p>

<!-- After Thymeleaf fills in the blanks -->
<h1>john_doe</h1>
<p>You have 42 files.</p>
```

### Where It Appears
- All `.html` files in `src/main/resources/templates/`
- Configuration in `application.properties` lines 28–30

### Key Thymeleaf Syntax

| Syntax | Purpose | Example |
|--------|---------|---------|
| `th:text="${var}"` | Display text | `<span th:text="${username}">Name</span>` |
| `th:each="item : ${list}"` | Loop | `<tr th:each="file : ${files}">` |
| `th:if="${condition}"` | Conditional | `<div th:if="${error != null}">` |
| `th:href="@{/path}"` | URL generation | `<a th:href="@{/files}">` |
| `th:action="@{/endpoint}"` | Form action | `<form th:action="@{/register}">` |
| `th:object="${dto}"` | Form binding | `<form th:object="${user}">` |
| `th:field="*{field}"` | Bind to DTO field | `<input th:field="*{username}">` |

### Advantages Over SPAs (React/Angular)
- No separate frontend build step
- SEO-friendly (server-rendered HTML)
- Simpler architecture for beginners
- No CORS issues (same server serves HTML and data)

### Disadvantages
- Full page reloads (no client-side routing)
- Limited interactivity compared to React
- Tighter coupling between frontend and backend

---

## 2.9 Maven

### What It Is
Maven is a **build tool and dependency manager** for Java. It reads `pom.xml` to know what libraries to download and how to build the project.

### The Analogy 📦
Maven is like a **recipe book + grocery delivery service**:
- `pom.xml` is the recipe (lists all ingredients/dependencies)
- Maven reads the recipe and downloads all ingredients from Maven Central (the grocery store)
- Then it follows the build instructions to create the final dish (JAR file)

### Where It Appears
[pom.xml](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/pom.xml) — the project's build file. Also: `mvnw` and `mvnw.cmd` are the Maven wrapper scripts (so you don't need Maven installed globally).

### Maven Lifecycle (Build Phases)

```
mvn clean    → Delete previous build output (target/)
mvn compile  → Compile Java source code
mvn test     → Run unit tests
mvn package  → Create JAR/WAR file
mvn install  → Install to local Maven repository
mvn deploy   → Upload to remote repository
```

### Dependency Scopes

| Scope | Meaning | Example in CloudNest |
|-------|---------|---------------------|
| `compile` (default) | Available everywhere | Spring Boot Web, JPA |
| `runtime` | Only at runtime, not compile time | PostgreSQL driver |
| `test` | Only for testing | JUnit, H2 database |
| `provided` | Available at compile, not packaged | Servlet API |
| `optional` | Not transitive | Lombok |

---

## 2.10 DTO Pattern (Data Transfer Object)

### What It Is
A DTO is a **plain Java class that carries data between layers** of your application. It acts as a "data envelope" — it holds exactly the data needed for a specific operation, nothing more.

### Why DTOs Exist — The Security Problem

Without DTOs, you might pass your `User` entity directly to the template:
```java
// DANGEROUS: Exposes password hash, internal ID, database version field
model.addAttribute("user", userEntity);
```

With DTOs, you create a safe "projection" of only the data the frontend needs:
```java
// SAFE: Only contains display data
model.addAttribute("dashboard", dashboardDto);
```

### Where They Appear

| DTO | Purpose | Fields Exposed |
|-----|---------|---------------|
| [UserRegistrationDto](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/dto/UserRegistrationDto.java) | Registration form | username, email, password, confirmPassword |
| [FileDto](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/dto/FileDto.java) | File listing | id, name, type, size, node, folder |
| [FolderDto](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/dto/FolderDto.java) | Folder listing | id, name, parentId, fileCount |
| [DashboardDto](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/dto/DashboardDto.java) | Dashboard stats | totalFiles, storage, quota, distributions |

### Common Mistake ❌
Never pass entities directly to Thymeleaf templates. Entities have lazy-loaded relationships that can throw `LazyInitializationException` after the Hibernate session closes.

---

## 2.11 Repository Pattern

### What It Is
The Repository pattern **abstracts all database access behind a clean interface**. Your service layer never writes SQL — it calls repository methods.

### The Analogy 📚
A repository is like a **librarian**:
- You don't go into the stacks yourself
- You tell the librarian: "Find me all books by author X published after 2020"
- The librarian knows the filing system and retrieves the books for you

### How Spring Data JPA Implements It

You write an interface:
```java
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
```

Spring Data JPA **generates a class at runtime** that implements this interface with actual SQL queries. You never write the implementation.

> **How this works internally**: At startup, Spring scans for interfaces extending `JpaRepository`. For each one, it creates a **JDK dynamic proxy** (using `java.lang.reflect.Proxy`) that intercepts method calls and translates them into JPA `EntityManager` operations.

---

## 2.12 Service Layer Architecture

### What It Is
The Service layer contains **business logic** — the rules and workflows of your application. It sits between the Controller (HTTP handling) and the Repository (data access).

### Why It Exists — Separation of Concerns

```
Controller: "Someone wants to upload a file"
     ↓
Service: "OK, let me check the quota, compute the hash,
          check for duplicates, pick a storage node,
          save the physical file, then save the metadata"
     ↓
Repository: "I'll write this record to the database"
```

Without the service layer, all this logic would be crammed into the controller, making it impossible to test, reuse, or maintain.

### Services in CloudNest

| Service | Responsibility |
|---------|---------------|
| [UserService](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/service/UserService.java) | Registration, authentication, user lookup |
| [FileStorageService](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/service/FileStorageService.java) | Upload, download, delete, search, dedup |
| [FolderService](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/service/FolderService.java) | Folder CRUD, recursive operations, ZIP download |
| [SharedLinkService](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/service/SharedLinkService.java) | Generate and resolve share tokens |
| [StorageNodeService](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/service/StorageNodeService.java) | Select storage node, build file paths |
| [TrashCleanupScheduler](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/service/TrashCleanupScheduler.java) | Auto-purge trash items older than 30 days |

---

## 2.13 BCrypt Password Hashing

### What It Is
BCrypt is a **one-way password hashing algorithm** designed to be intentionally slow. When a user sets their password to "mypassword123", BCrypt produces something like:
```
$2a$12$LJ3m4Y5Z2k8x9w0v1u2t3.aB7cD8eF9gH0iJ1kL2mN3oP4qR5sT6
```

### Why "Intentionally Slow"?
- If hashing is fast, attackers can try billions of passwords per second
- BCrypt uses a **cost factor** (CloudNest uses 12, meaning 2^12 = 4096 iterations)
- At cost factor 12, hashing one password takes ~250ms — fast enough for login, terrible for brute-force

### Why Not SHA-256 for Passwords?
SHA-256 is fast (nanoseconds). An attacker with a GPU can try **billions** of SHA-256 hashes per second. BCrypt is intentionally slow, making brute-force impractical.

### Where It Appears
- [SecurityConfig.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/security/SecurityConfig.java) line 41: `new BCryptPasswordEncoder(12)`
- [UserService.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/service/UserService.java) line 84: `passwordEncoder.encode(dto.getPassword())`

---

## 2.14 Deduplication Architecture

### What It Is
Deduplication means **storing only one physical copy of identical files**. If User A and User B both upload the same photo, only one copy is saved on disk. Both database records point to the same physical file.

### How CloudNest Does It

```
User uploads file → Compute SHA-256 hash → Check if hash exists in DB
     │                                              │
     │                                    ┌─────────┴─────────┐
     │                                    │  Hash exists?      │
     │                                    │  YES → Reuse file  │
     │                                    │  NO → Save new file│
     │                                    └───────────────────┘
```

The implementation is in [FileStorageService.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/service/FileStorageService.java) lines 98–133.

### Important: Dedup-Aware Deletion
When deleting a file, we can't just delete the physical file — another record might point to it! That's why `permanentDeleteFile()` checks `countByStoredName()` before deleting from disk.

---

## 2.15 Soft Delete Architecture

### What It Is
Soft delete means **marking a record as deleted without actually removing it from the database**. The record stays in the table with `is_deleted = true`.

### Why Not Just Delete?
| Approach | Pros | Cons |
|----------|------|------|
| Hard delete | Saves space | Data is gone forever |
| Soft delete | Users can restore; audit trail | Must filter everywhere; complicates queries |

### How CloudNest Implements It
Both `FileEntity` and `Folder` have:
```java
@Column(name = "is_deleted")
private boolean isDeleted = false;

@Column(name = "deleted_at")
private LocalDateTime deletedAt;
```

Every query that fetches "active" data includes `AndIsDeletedFalse`:
```java
List<FileEntity> findByUserAndIsDeletedFalseOrderByUploadedAtDesc(User user);
```

The `deletedAt` timestamp is used by `TrashCleanupScheduler` to auto-purge files older than 30 days.

---

## 2.16 Shared Link System

### What It Is
Shared links allow file owners to generate a **public URL** that anyone can use to download a file — no login required.

### How It Works
1. User clicks "Share" on a file
2. System generates a UUID token: `a1b2c3d4-e5f6-7890-abcd-ef1234567890`
3. A `SharedLink` record is saved with the token, file ID, creator, and expiry (7 days)
4. The URL `/share/{token}` is returned
5. Anyone visiting that URL can view file info and download it
6. After 7 days, the link is rejected as expired

### Where It Appears
- [SharedLink.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/entity/SharedLink.java) — Entity
- [SharedLinkService.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/service/SharedLinkService.java) — Business logic
- [ShareController.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/controller/ShareController.java) — HTTP endpoints

---

## 2.17 Scheduler System

### What It Is
Spring's `@Scheduled` annotation allows you to run methods on a timer — like a cron job inside your application.

### Where It Appears
[TrashCleanupScheduler.java](file:///c:/Users/Anmol%20Raj/OneDrive/Desktop/Final%20Java%20Project/src/main/java/com/cloudnest/service/TrashCleanupScheduler.java) runs daily at 2:00 AM:

```java
@Scheduled(cron = "0 0 2 * * *")  // second minute hour day month weekday
@Transactional
public void purgeExpiredTrashItems() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
    List<FileEntity> expired = fileRepository.findByIsDeletedTrueAndDeletedAtBefore(cutoff);
    // ... permanently delete each expired file
}
```

### Cron Expression Breakdown
```
0 0 2 * * *
│ │ │ │ │ │
│ │ │ │ │ └── Day of week (any)
│ │ │ │ └──── Month (any)
│ │ │ └────── Day of month (any)
│ │ └──────── Hour (2 = 2 AM)
│ └────────── Minute (0)
└──────────── Second (0)
```

This is enabled by `@EnableScheduling` on `CloudNestApplication.java`.

---

# Appendix A — Complete Technology Comparison Matrix

The prompt required **advantages, disadvantages, and alternatives** for every technology. The main sections above covered these for Java 21, PostgreSQL, Thymeleaf, and Session Auth. Below is the complete matrix for the remaining technologies:

## Spring Boot

| Aspect | Details |
|--------|---------|
| **Advantages** | Convention over configuration; embedded Tomcat (no deployment server needed); auto-configuration detects classpath and wires beans; massive starter ecosystem; production-ready (Actuator metrics/health); huge community + docs |
| **Disadvantages** | "Magic" — hard to debug auto-configuration; heavy memory footprint (~200MB baseline); slow startup (~3-8s); learning curve for understanding what happens behind the scenes; upgrade path can be painful across major versions |
| **Alternatives** | **Quarkus** (GraalVM native, sub-second startup), **Micronaut** (compile-time DI, low memory), **Vert.x** (reactive, event-loop), **Javalin** (lightweight, minimalist), **Dropwizard** (production-ready, opinionated) |

## Spring MVC

| Aspect | Details |
|--------|---------|
| **Advantages** | Mature and battle-tested; seamless integration with Spring ecosystem (Security, Data, etc.); clean separation of concerns via MVC pattern; supports both server-rendered (Thymeleaf) and REST (JSON) responses; excellent form binding and validation support |
| **Disadvantages** | Synchronous/blocking by default (thread-per-request model); verbose compared to frameworks like Express.js; tight coupling to Servlet API; not ideal for reactive/streaming use cases |
| **Alternatives** | **Spring WebFlux** (reactive, non-blocking), **JAX-RS (Jersey)** (standard REST API framework), **Spark Java** (lightweight, minimal), **Play Framework** (Scala/Java, reactive) |

## Spring Security

| Aspect | Details |
|--------|---------|
| **Advantages** | Comprehensive — handles auth, authorization, CSRF, CORS, session management all in one; deeply integrated with Spring Boot; highly customizable filter chain; supports OAuth2, SAML, LDAP, JWT out of the box; mature and actively maintained |
| **Disadvantages** | Steep learning curve — heavily abstracted, hard to understand what filters do what; verbose configuration for simple use cases; error messages are often unhelpful ("Access Denied" with no context); debugging requires understanding the full filter chain |
| **Alternatives** | **Apache Shiro** (simpler API, less opinionated), **Keycloak** (standalone identity server), **Auth0** (managed authentication SaaS), **Pac4j** (multi-protocol security library) |

## Spring Data JPA

| Aspect | Details |
|--------|---------|
| **Advantages** | Zero boilerplate — query derivation from method names; `JpaRepository` provides 15+ CRUD methods for free; supports custom `@Query` for complex queries; pagination and sorting built-in; excellent Spring Boot auto-configuration |
| **Disadvantages** | Hides SQL — hard to debug performance issues; derived query method names can become absurdly long; limited control over generated SQL; LazyInitializationException is a common pain point; not ideal for complex reporting queries |
| **Alternatives** | **MyBatis** (SQL-first, full control over queries), **JOOQ** (type-safe SQL builder), **JDBC Template** (raw SQL with Spring support), **Exposed** (Kotlin SQL DSL) |

## Hibernate (ORM)

| Aspect | Details |
|--------|---------|
| **Advantages** | Automatic SQL generation from entity annotations; database-agnostic (switch databases by changing dialect); lazy loading reduces memory for large object graphs; first-level cache (per-session) improves read performance; mature community and extensive docs |
| **Disadvantages** | N+1 query problem (silent performance killer); complex caching layers (L1, L2, query cache); `ddl-auto=update` is dangerous for production; generated SQL can be suboptimal; learning curve for mapping complex relationships |
| **Alternatives** | **EclipseLink** (JPA reference implementation), **MyBatis** (explicit SQL mapping), **JOOQ** (type-safe SQL), **Exposed** (Kotlin DSL), **raw JDBC** (full control, no abstraction) |

## Maven

| Aspect | Details |
|--------|---------|
| **Advantages** | Industry standard for Java projects; deterministic builds via dependency locking; central repository (Maven Central) has 20M+ artifacts; well-understood lifecycle (compile → test → package); XML is verbose but explicit |
| **Disadvantages** | XML configuration is verbose and hard to read; slow dependency resolution on first build; rigid lifecycle model (hard to customize); pom.xml inheritance can be confusing; no Kotlin DSL support |
| **Alternatives** | **Gradle** (Groovy/Kotlin DSL, faster builds, more flexible), **sbt** (Scala projects), **Bazel** (Google's polyglot build tool), **Ant + Ivy** (legacy, full control) |

## DTO Pattern

| Aspect | Details |
|--------|---------|
| **Advantages** | Decouples API/view layer from database entities; prevents accidental exposure of sensitive fields (password hash); avoids LazyInitializationException in templates; allows shaping data to match what the consumer needs; enables API versioning without changing entities |
| **Disadvantages** | Boilerplate — requires creating and maintaining parallel classes; mapping code between entity ↔ DTO can be tedious; can lead to DTO explosion (one per endpoint); increases codebase size |
| **Alternatives** | **MapStruct** (compile-time DTO mapping, eliminates manual mapping), **ModelMapper** (convention-based auto-mapping), **Spring Projections** (interface-based projections, no separate class needed), **Records** (Java 16+ immutable DTOs with less boilerplate) |

## Repository Pattern

| Aspect | Details |
|--------|---------|
| **Advantages** | Clean abstraction — service layer never touches SQL; easy to mock in unit tests; centralizes all data access in one place per entity; Spring Data JPA generates implementations automatically |
| **Disadvantages** | Can hide performance problems (developers don't see the SQL); method naming conventions have limitations; custom complex queries still need `@Query`; adds a layer of indirection |
| **Alternatives** | **DAO Pattern** (Data Access Object — similar but with explicit implementations), **Active Record** (entity manages its own persistence — used in Ruby/Rails), **CQRS** (Command Query Responsibility Segregation — separate read/write models) |

## Service Layer

| Aspect | Details |
|--------|---------|
| **Advantages** | Centralizes business logic in one testable layer; enables transaction management via `@Transactional`; promotes reuse (multiple controllers can call the same service); keeps controllers thin and focused on HTTP concerns |
| **Disadvantages** | Can become a "God class" if not split properly (e.g., `FileStorageService` at 426 lines); adds indirection between controller and repository; simple CRUD pass-through services feel like unnecessary ceremony |
| **Alternatives** | **Domain-Driven Design (DDD)** — business logic lives in domain objects, not services; **Hexagonal Architecture** — ports and adapters pattern; **CQRS** — separate command/query handlers instead of monolithic services |

## BCrypt

| Aspect | Details |
|--------|---------|
| **Advantages** | Intentionally slow — prevents brute-force attacks; adaptive — cost factor can be increased as hardware improves; includes salt automatically (no separate salt column needed); industry standard, battle-tested since 1999 |
| **Disadvantages** | CPU-intensive — high cost factor can slow login under load; 72-byte password limit (truncates longer passwords silently); not memory-hard (vulnerable to GPU attacks at lower cost factors compared to Argon2) |
| **Alternatives** | **Argon2** (winner of Password Hashing Competition 2015, memory-hard), **scrypt** (memory-hard, used by Litecoin), **PBKDF2** (NIST standard, used in WPA2-WiFi), **Spring's DelegatingPasswordEncoder** (supports multiple algorithms with migration) |

## Scheduler System (`@Scheduled`)

| Aspect | Details |
|--------|---------|
| **Advantages** | Zero external dependencies — runs inside the JVM; simple annotation-based configuration; supports cron expressions and fixed-delay/rate; integrates natively with Spring's transaction management |
| **Disadvantages** | Single-instance only — if you scale to 3 instances, the job runs 3 times; no retry/failure tracking; no distributed locking; no web UI to monitor job status; no job persistence (restarts lose state) |
| **Alternatives** | **Quartz Scheduler** (persistent, clustered, retry support), **ShedLock** (distributed lock for `@Scheduled` — ensures single execution), **Spring Batch** (for large data processing jobs), **Kubernetes CronJob** (externalized scheduling) |

## Deduplication (SHA-256)

| Aspect | Details |
|--------|---------|
| **Advantages** | Saves disk space — identical files stored only once; SHA-256 is collision-resistant (probability of two different files having the same hash: 1 in 2^256); simple to implement — hash before write, check before store |
| **Disadvantages** | File-level only — doesn't detect partial duplicates (e.g., two PDFs with 90% same content); requires loading entire file into memory for hashing; race condition on concurrent uploads of same file; doesn't help with similar-but-not-identical files |
| **Alternatives** | **Block-level dedup** (Dropbox — splits files into 4KB chunks, deduplicates each chunk), **Content-Defined Chunking** (Restic — variable-size chunks based on content boundaries), **Delta encoding** (Git — stores differences between file versions) |

---

> **📖 Continue to Part 2** → `Learning_Guide_Part2_Folder_and_File_Walkthrough.md`


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


# ☁️ CloudNest — Master Learning Guide — Part 6

> **Chapters 11–13: Refactoring, DevOps & Final Engineering Lessons**
> Level: 🔴 Advanced

---

# Chapter 11 — Refactoring & Future Architecture

## 11.1 Evolving into a Real SaaS

### Current State → SaaS Transformation

| Aspect | Current (Monolith) | Target (SaaS) |
|--------|-------------------|---------------|
| **Users** | Single-tenant | Multi-tenant (organizations) |
| **Storage** | Local filesystem | AWS S3 / MinIO |
| **Auth** | Session cookies | OAuth2 + JWT |
| **Database** | Single PostgreSQL | PostgreSQL + Redis |
| **Search** | LIKE queries | Elasticsearch |
| **Frontend** | Thymeleaf SSR | React/Next.js SPA |
| **API** | HTML endpoints | REST API v1 + GraphQL |
| **Payments** | None | Stripe subscription tiers |
| **Deployment** | Manual JAR | Docker + Kubernetes |

### The Migration Path
You don't rewrite everything at once. You evolve incrementally:

```
Phase 1 (Now):     Fix bugs + add tests + add Flyway
Phase 2 (1 month): Extract admin logic to AdminService, add pagination
Phase 3 (2 months): Add REST API layer alongside Thymeleaf (dual-stack)
Phase 4 (3 months): Replace local storage with S3/MinIO
Phase 5 (6 months): Add Redis for sessions + caching
Phase 6 (1 year):  Optional: migrate frontend to React (consuming REST API)
```

---

## 11.2 Microservices vs Monolith

### When to Stay Monolith
- Team size < 10 developers
- Product is still finding its market fit
- Deployment is simple (one server)
- Current performance is sufficient

### When to Consider Microservices
- Team grows beyond 10 engineers
- Different components need different scaling (auth vs storage vs search)
- You need independent deployment of features
- Different parts need different tech stacks

### Potential Microservice Split for CloudNest

```
┌────────────┐  ┌────────────┐  ┌────────────┐  ┌────────────┐
│ Auth       │  │ File       │  │ Share      │  │ Search     │
│ Service    │  │ Service    │  │ Service    │  │ Service    │
│ (Java)     │  │ (Java)     │  │ (Go)       │  │ (Java+ES)  │
└─────┬──────┘  └─────┬──────┘  └─────┬──────┘  └─────┬──────┘
      │               │               │               │
      └───────────────┼───────────────┼───────────────┘
                      │               │
               ┌──────▼──────┐  ┌─────▼─────┐
               │ PostgreSQL  │  │  S3/MinIO  │
               └─────────────┘  └───────────┘
```

> **🎓 Interview Wisdom**: "Start with a monolith, extract microservices when pain justifies the complexity." — Martin Fowler's "MonolithFirst" pattern. Every successful microservice architecture started as a monolith (Netflix, Amazon, Uber).

---

## 11.3 Storage Abstraction (Strategy Pattern)

### Current Problem
`FileStorageService` writes directly to `java.nio.file.Files`. Switching to S3 requires rewriting the entire service.

### Solution: Strategy Pattern
```java
// Step 1: Define an interface (the "strategy")
public interface StorageBackend {
    void store(String path, byte[] data);
    byte[] retrieve(String path);
    void delete(String path);
    boolean exists(String path);
}

// Step 2: Local implementation (current behavior)
@Component
@Profile("local")  // Active when profile is "local"
public class LocalStorageBackend implements StorageBackend {
    @Override
    public void store(String path, byte[] data) {
        Files.write(Paths.get(path), data);
    }
    // ... other methods
}

// Step 3: S3 implementation (future)
@Component
@Profile("cloud")  // Active when profile is "cloud"
public class S3StorageBackend implements StorageBackend {
    private final AmazonS3 s3Client;

    @Override
    public void store(String path, byte[] data) {
        s3Client.putObject("cloudnest-bucket", path,
            new ByteArrayInputStream(data), new ObjectMetadata());
    }
    // ... other methods
}

// Step 4: FileStorageService uses the interface, not the implementation
@Service
public class FileStorageService {
    private final StorageBackend storageBackend;  // Interface, not concrete class!

    public FileStorageService(StorageBackend storageBackend) {
        this.storageBackend = storageBackend;
    }

    public void saveFile(byte[] data, String path) {
        storageBackend.store(path, data);  // Works with local OR S3!
    }
}
```

Switch backends by changing one property:
```properties
# Development: local filesystem
spring.profiles.active=local

# Production: AWS S3
spring.profiles.active=cloud
```

---

## 11.4 Event-Driven Architecture

Instead of tightly coupling operations:
```java
// Current: Everything happens synchronously in one transaction
public FileDto uploadFile(MultipartFile file, User user, Long folderId) {
    validateFile(file);
    byte[] bytes = file.getBytes();
    String hash = computeSha256(bytes);
    // ... save to disk ...
    FileEntity entity = fileRepository.save(fileEntity);
    // What if we also need to: index in Elasticsearch?
    //                          Generate a thumbnail?
    //                          Send a notification email?
    //                          Update analytics?
    // Adding each one makes this method bigger and slower.
}
```

Use Spring's event system:
```java
// Future: Publish event, multiple listeners handle independently
public FileDto uploadFile(MultipartFile file, User user, Long folderId) {
    // Core operation only
    FileEntity entity = fileRepository.save(fileEntity);

    // Publish event — fire and forget
    eventPublisher.publishEvent(new FileUploadedEvent(entity.getId(), user.getId()));

    return convertToDto(entity);
}

// Listener 1: Update dashboard cache
@EventListener
public void onFileUploaded(FileUploadedEvent event) {
    cacheManager.evict("dashboard-stats", event.getUserId());
}

// Listener 2: Index in Elasticsearch (could be @Async for non-blocking)
@Async
@EventListener
public void indexFile(FileUploadedEvent event) {
    elasticSearchService.index(event.getFileId());
}

// Listener 3: Generate thumbnail for images
@Async
@EventListener
public void generateThumbnail(FileUploadedEvent event) {
    thumbnailService.generateIfImage(event.getFileId());
}
```

Benefits:
- Each listener is **independent** — adding/removing one doesn't affect others
- `@Async` listeners run in background threads — upload response is faster
- Easy to test each listener in isolation

---

## 11.5 Distributed Storage Deep Dive

### Current State
CloudNest simulates distributed storage with 3 local directories (`storage/node1/`, `node2/`, `node3/`). `StorageNodeService.selectNode()` assigns files using a round-robin strategy.

### Real Distributed Storage Concepts

| Concept | What It Is | CloudNest Equivalent | Production Solution |
|---------|-----------|---------------------|---------|
| **Object Storage** | Files stored as objects with metadata (not in a filesystem hierarchy) | `storage/nodeX/uuid.ext` | AWS S3, MinIO, Google Cloud Storage |
| **Consistent Hashing** | Algorithm to distribute data across nodes evenly, minimizing redistribution when nodes are added/removed | Round-robin (simple) | Consistent hashing ring (e.g., Ketama algorithm) |
| **Replication** | Storing copies of data on multiple nodes for fault tolerance | None (single copy) | 3x replication (S3 stores 3+ copies across data centers) |
| **Sharding** | Splitting data across databases/storage by some key | Not implemented | Shard by `user_id` hash — users 1-1000 → shard A, 1001-2000 → shard B |
| **CDN** | Caching content at edge locations close to users | Not implemented | CloudFront, Cloudflare, Fastly |

### Consistent Hashing — How It Works

Round-robin (CloudNest) fails when nodes are added/removed — all files need redistribution. Consistent hashing minimizes this:

```
Imagine a circular ring from 0 to 360 degrees:

    0° ─────── node1 (at 90°) ─────── node2 (at 200°) ─────── node3 (at 310°) ──── 360°

To place a file:
1. Hash the file key: hash("report.pdf") → 145°
2. Walk clockwise until you hit a node → node2 (at 200°)
3. File goes to node2

Adding node4 at 150°:
- Only files between 90° and 150° need to move (from node2 → node4)
- All other files stay put!
- With round-robin, ALL files would need redistribution
```

### Implementing S3-Compatible Storage (MinIO)

MinIO is a **self-hosted S3-compatible object storage** — perfect for replacing local filesystem storage:

```java
// S3StorageBackend using AWS SDK
@Component
@Profile("cloud")
public class S3StorageBackend implements StorageBackend {
    private final AmazonS3 s3Client;
    private final String bucketName = "cloudnest-files";

    @Override
    public void store(String path, byte[] data) {
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(data.length);
        s3Client.putObject(bucketName, path,
            new ByteArrayInputStream(data), metadata);
    }

    @Override
    public byte[] retrieve(String path) {
        S3Object obj = s3Client.getObject(bucketName, path);
        return obj.getObjectContent().readAllBytes();
    }

    @Override
    public void delete(String path) {
        s3Client.deleteObject(bucketName, path);
    }
}
```

---

## 11.6 Scaling PostgreSQL

### Problem: Single Database Bottleneck
With 100,000+ users, a single PostgreSQL instance becomes the bottleneck — too many reads AND writes on one server.

### Solution 1: Read Replicas

```
                    ┌────────────────┐
                    │   Primary DB   │ ◀── All WRITES go here
                    │ (PostgreSQL)   │
                    └───────┬────────┘
                            │ Streaming Replication
                    ┌───────┼───────────┐
                    ▼       ▼           ▼
            ┌──────────┐ ┌──────────┐ ┌──────────┐
            │ Replica 1│ │ Replica 2│ │ Replica 3│  ◀── All READS go here
            └──────────┘ └──────────┘ └──────────┘
```

Spring Boot supports this with `@Transactional(readOnly = true)`:

```java
@Service
public class FileStorageService {

    @Transactional(readOnly = true)  // Routes to READ replica
    public List<FileDto> getRecentFiles(User user) {
        return fileRepository.findTop5ByUserAndIsDeletedFalse(user);
    }

    @Transactional  // Routes to PRIMARY (default)
    public FileDto uploadFile(MultipartFile file, User user, Long folderId) {
        // ... write operations ...
    }
}
```

Configure routing in `application.properties`:
```properties
# Primary (read-write)
spring.datasource.primary.url=jdbc:postgresql://primary-db:5432/cloudnest
# Replica (read-only)
spring.datasource.replica.url=jdbc:postgresql://replica-db:5432/cloudnest
```

### Solution 2: Connection Pooling (HikariCP)

Each database connection costs ~5MB of memory on the PostgreSQL server. Without pooling, 1000 concurrent users = 1000 connections = 5GB. Connection pooling **reuses** connections:

```properties
# HikariCP configuration (Spring Boot default pool)
spring.datasource.hikari.maximum-pool-size=20        # Max 20 connections
spring.datasource.hikari.minimum-idle=5               # Keep 5 idle connections ready
spring.datasource.hikari.idle-timeout=300000           # Close idle connections after 5 min
spring.datasource.hikari.connection-timeout=30000      # Wait max 30s for a connection
spring.datasource.hikari.max-lifetime=1800000          # Recycle connections every 30 min
```

For high-traffic: use **PgBouncer** (external connection pooler) in front of PostgreSQL:
```
App (100 connections) → PgBouncer (pools to 20) → PostgreSQL (only 20 actual connections)
```

### Solution 3: Table Partitioning

The `files` table will grow to millions of rows. Partition by `uploaded_at` to speed up time-based queries:

```sql
-- Convert files to a partitioned table (by month)
CREATE TABLE files (
    id            BIGSERIAL,
    original_name VARCHAR(255) NOT NULL,
    -- ... all columns ...
    uploaded_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) PARTITION BY RANGE (uploaded_at);

-- Create monthly partitions
CREATE TABLE files_2026_01 PARTITION OF files
    FOR VALUES FROM ('2026-01-01') TO ('2026-02-01');
CREATE TABLE files_2026_02 PARTITION OF files
    FOR VALUES FROM ('2026-02-01') TO ('2026-03-01');
-- ... auto-create future partitions with pg_partman
```

Benefit: `WHERE uploaded_at > '2026-05-01'` only scans the May partition, not all 10 million rows.

### Quick Reference: PostgreSQL Scaling Ladder

| Users | Strategy | Complexity |
|-------|----------|------------|
| 1–1K | Single instance, HikariCP tuning | Low |
| 1K–10K | Add read replicas + caching (Redis/Caffeine) | Medium |
| 10K–100K | PgBouncer + table partitioning + query optimization | Medium-High |
| 100K+ | Sharding (Citus) or managed service (RDS, CloudSQL) | High |

---

## 11.7 Refactoring Priorities

### Priority Matrix from the Audit Report

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

### How to Read This
- **Top-left**: High impact, low effort → Do these TODAY
- **Top-right**: High impact, high effort → Plan for next sprint
- **Bottom-left**: Low impact, low effort → Do when you have spare time
- **Bottom-right**: Low impact, high effort → Backlog (may never get to)

---

# Chapter 12 — DevOps & Deployment

## 12.1 Docker Setup

### What is Docker?
Docker packages your application and ALL its dependencies into a **container** — a lightweight, isolated environment that runs the same way everywhere (your laptop, CI server, production server).

### The Analogy 📦
Without Docker: "It works on my machine!" → breaks on the server because Java version is different, PostgreSQL is missing, etc.

With Docker: Your application ships in a sealed box with everything it needs. The box runs identically everywhere.

### Dockerfile for CloudNest
```dockerfile
# =========================================
# Stage 1: BUILD (using Maven)
# =========================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml first (for dependency caching)
COPY pom.xml .
RUN mvn dependency:go-offline    # Download all dependencies (cached if pom.xml unchanged)

# Copy source code and build
COPY src ./src
RUN mvn clean package -DskipTests   # Compile + package into JAR

# =========================================
# Stage 2: RUN (minimal JRE image)
# =========================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy only the built JAR from the build stage
COPY --from=build /app/target/cloudnest-1.0.0.jar app.jar

# Create storage directories
RUN mkdir -p /data/storage/node1 /data/storage/node2 /data/storage/node3

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### Why Multi-Stage Build?
- Build stage: ~800MB (includes Maven, JDK, source code)
- Run stage: ~200MB (only JRE + JAR)
- Production image is 4x smaller and has no build tools (smaller attack surface)

### docker-compose.yml
```yaml
version: "3.8"

services:
  app:
    build: .
    ports:
      - "8080:8080"
    environment:
      - DB_PASSWORD=${DB_PASSWORD}
      - DDL_AUTO=validate
      - SHOW_SQL=false
      - STORAGE_BASE_PATH=/data/storage
    volumes:
      - file-storage:/data/storage     # Persist uploaded files
    depends_on:
      postgres:
        condition: service_healthy     # Wait for DB to be ready

  postgres:
    image: postgres:16-alpine
    environment:
      - POSTGRES_DB=cloudnest_db
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data  # Persist database
    ports:
      - "5432:5432"
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5

volumes:
  pgdata:          # Named volume for database persistence
  file-storage:    # Named volume for uploaded files
```

### Running It
```bash
# Create .env file with secrets
echo "DB_PASSWORD=my_secure_password_123" > .env

# Build and start everything
docker-compose up --build

# Access the app
open http://localhost:8080
```

---

## 12.2 Environment Variables in Production

| Variable | Dev Value | Production Value |
|----------|-----------|-----------------|
| `DB_PASSWORD` | `#nanshu@229` | Random 32-char string from secret manager |
| `DDL_AUTO` | `update` | `validate` or `none` |
| `SHOW_SQL` | `true` | `false` |
| `STORAGE_BASE_PATH` | `storage` | `/var/cloudnest/storage` or S3 bucket |
| `JAVA_OPTS` | (default) | `-Xmx512m -Xms256m` (tune JVM memory) |
| `SERVER_PORT` | `8080` | `8080` (behind reverse proxy with HTTPS) |

---

## 12.3 Nginx Reverse Proxy

### What is a Reverse Proxy?
Nginx sits in front of your application and handles:
- **HTTPS termination** (encrypts traffic)
- **Load balancing** (distributes requests across instances)
- **Static file serving** (faster than Java for CSS/JS/images)
- **Request buffering** (protects against slow clients)

```
Internet → Nginx (:443 HTTPS) → CloudNest (:8080 HTTP)
```

### Configuration
```nginx
server {
    listen 80;
    server_name cloudnest.example.com;
    return 301 https://$host$request_uri;  # Force HTTPS
}

server {
    listen 443 ssl;
    server_name cloudnest.example.com;

    ssl_certificate     /etc/ssl/cert.pem;
    ssl_certificate_key /etc/ssl/key.pem;

    client_max_body_size 50M;  # Match Spring's max upload size

    # Serve static files directly (faster than Java)
    location /css/ {
        alias /app/static/css/;
        expires 30d;
    }

    location /js/ {
        alias /app/static/js/;
        expires 30d;
    }

    # Proxy everything else to Spring Boot
    location / {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

---

## 12.4 CI/CD with GitHub Actions

### What is CI/CD?
- **CI (Continuous Integration)**: Every push triggers automated tests
- **CD (Continuous Deployment)**: Passing tests → automatic deploy to production

### GitHub Actions Workflow
```yaml
name: CloudNest CI/CD

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:16
        env:
          POSTGRES_DB: cloudnest_test
          POSTGRES_PASSWORD: testpass
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 21
        uses: actions/setup-java@v4
        with:
          java-version: 21
          distribution: temurin
          cache: maven

      - name: Run tests
        run: mvn clean verify
        env:
          DB_PASSWORD: testpass
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/cloudnest_test

      - name: Upload test results
        if: always()
        uses: actions/upload-artifact@v4
        with:
          name: test-results
          path: target/surefire-reports/

  deploy:
    needs: test
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main' && github.event_name == 'push'
    steps:
      - uses: actions/checkout@v4

      - name: Build Docker image
        run: docker build -t cloudnest:${{ github.sha }} .

      - name: Push to registry
        run: |
          docker tag cloudnest:${{ github.sha }} registry.example.com/cloudnest:latest
          docker push registry.example.com/cloudnest:latest
```

---

## 12.5 Flyway Database Migrations

### Why Not `ddl-auto=update`?
| Problem | Example |
|---------|---------|
| Can't track changes | "When was the `role` column added?" → No record |
| Can't rollback | "Undo the last schema change" → Impossible |
| Creates but never deletes | Rename `file_hash` → creates new column, old one stays |
| Different envs diverge | Dev has columns that prod doesn't |

### How Flyway Works
Each migration is a numbered SQL file that runs exactly once:

```
src/main/resources/db/migration/
├── V1__create_users_table.sql
├── V2__create_files_table.sql
├── V3__create_folders_table.sql
├── V4__create_shared_links_table.sql
├── V5__add_file_hash_column.sql
├── V6__add_soft_delete_columns.sql
├── V7__add_indexes.sql
└── V8__add_version_columns.sql
```

Flyway tracks which migrations have been applied in a `flyway_schema_history` table:

| installed_rank | version | description | success |
|---------------|---------|-------------|---------|
| 1 | 1 | create users table | true |
| 2 | 2 | create files table | true |
| 3 | 3 | create folders table | true |

New migration `V9__add_audit_log.sql` → Flyway sees it hasn't run → executes it → records in history.

### Example Migration
```sql
-- V7__add_indexes.sql
-- Performance indexes for common query patterns

CREATE INDEX IF NOT EXISTS idx_files_user_id ON files(user_id);
CREATE INDEX IF NOT EXISTS idx_files_folder_id ON files(folder_id);
CREATE INDEX IF NOT EXISTS idx_files_file_hash ON files(file_hash);
CREATE INDEX IF NOT EXISTS idx_files_is_deleted ON files(is_deleted) WHERE is_deleted = true;
CREATE INDEX IF NOT EXISTS idx_folders_user_id ON folders(user_id);
CREATE INDEX IF NOT EXISTS idx_folders_parent_id ON folders(parent_id);
CREATE INDEX IF NOT EXISTS idx_shared_links_token ON shared_links(token);
```

### Setup
```xml
<!-- Add to pom.xml -->
<dependency>
    <groupId>org.flywaydb</groupId>
    <artifactId>flyway-core</artifactId>
</dependency>
```

```properties
# application.properties
spring.jpa.hibernate.ddl-auto=validate  # Only VALIDATE, never modify!
spring.flyway.enabled=true
spring.flyway.locations=classpath:db/migration
```

---

## 12.6 Database Backups

### Automated Backup Script
```bash
#!/bin/bash
# backup_cloudnest.sh — Run daily via cron

BACKUP_DIR=/backups/cloudnest
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

# Create backup
pg_dump -h localhost -U postgres cloudnest_db \
  | gzip > ${BACKUP_DIR}/cloudnest_${DATE}.sql.gz

# Log
echo "[$(date)] Backup created: cloudnest_${DATE}.sql.gz" >> ${BACKUP_DIR}/backup.log

# Cleanup old backups
find ${BACKUP_DIR} -name "*.sql.gz" -mtime +${RETENTION_DAYS} -delete
echo "[$(date)] Cleaned backups older than ${RETENTION_DAYS} days" >> ${BACKUP_DIR}/backup.log
```

### Cron Schedule
```bash
# Run at 3 AM daily (1 hour after trash cleanup)
0 3 * * * /scripts/backup_cloudnest.sh
```

### Restore from Backup
```bash
gunzip < /backups/cloudnest/cloudnest_20260528_030000.sql.gz \
  | psql -h localhost -U postgres cloudnest_db
```

---

## 12.7 Staging Environments

### What is a Staging Environment?
A staging environment is a **near-exact replica of production** where you test changes before they go live. It has its own database, its own storage, and its own URL — but runs the same code as production.

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│    Local     │     │   Staging   │     │ Production  │
│  (your PC)  │ ──▶ │  (test env) │ ──▶ │  (live)     │
│             │     │             │     │             │
│ ddl-auto=   │     │ ddl-auto=   │     │ ddl-auto=   │
│   update    │     │   validate  │     │   validate  │
│             │     │             │     │             │
│ H2/Postgres │     │ PostgreSQL  │     │ PostgreSQL  │
│  (local)    │     │ (separate)  │     │ (production)│
└─────────────┘     └─────────────┘     └─────────────┘
```

### Spring Profiles for Environments

Spring Profiles let you have **different configuration for each environment**:

```
src/main/resources/
├── application.properties           ← Shared config (port, app name)
├── application-dev.properties       ← Local development overrides
├── application-staging.properties   ← Staging environment overrides
└── application-prod.properties      ← Production overrides
```

#### `application-dev.properties`
```properties
# Local development — permissive, verbose
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
logging.level.com.cloudnest=DEBUG
spring.thymeleaf.cache=false
cloudnest.storage.base-path=storage
```

#### `application-staging.properties`
```properties
# Staging — mirrors production but with separate database
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
logging.level.com.cloudnest=INFO
spring.thymeleaf.cache=true

# Separate staging database
spring.datasource.url=jdbc:postgresql://staging-db:5432/cloudnest_staging
spring.datasource.password=${STAGING_DB_PASSWORD}

# Staging storage
cloudnest.storage.base-path=/var/cloudnest/staging-storage
```

#### `application-prod.properties`
```properties
# Production — strict, optimized, secure
spring.jpa.hibernate.ddl-auto=validate
spring.jpa.show-sql=false
logging.level.com.cloudnest=WARN
spring.thymeleaf.cache=true

# Production database
spring.datasource.url=jdbc:postgresql://prod-db:5432/cloudnest_prod
spring.datasource.password=${PROD_DB_PASSWORD}

# Production storage (S3 or dedicated volume)
cloudnest.storage.base-path=/var/cloudnest/prod-storage

# Security hardening
server.servlet.session.cookie.secure=true    # HTTPS-only cookies
server.servlet.session.cookie.http-only=true # No JavaScript access
```

### Activating a Profile
```bash
# Local development (default)
java -jar cloudnest.jar

# Staging
java -jar cloudnest.jar --spring.profiles.active=staging

# Production
java -jar cloudnest.jar --spring.profiles.active=prod

# Docker (set via environment variable)
docker run -e SPRING_PROFILES_ACTIVE=prod cloudnest:latest
```

### Feature Flags with Profiles

You can conditionally enable features per environment:

```java
@Component
@Profile("staging")  // Only active in staging
public class StagingDataSeeder {
    @PostConstruct
    public void seedTestData() {
        // Create test users and files for QA testing
        // This NEVER runs in production
    }
}
```

### CI/CD Pipeline with Staging

```
┌────────────────────────────────────────────────────────────────┐
│ Developer pushes to main branch                                │
│                                                                │
│  1. CI: Run unit + integration tests ──▶ ✅ Pass               │
│  2. CD: Deploy to STAGING (auto)                               │
│  3. QA: Manual testing on staging.cloudnest.com                │
│  4. Approval gate: QA clicks "Approve"                         │
│  5. CD: Deploy to PRODUCTION (auto after approval)             │
└────────────────────────────────────────────────────────────────┘
```

This ensures no untested code ever reaches production.

---

# Chapter 13 — Final Engineering Lessons

## 13.1 How Real Engineers Think

### The "5 Whys" Technique
When a bug occurs, don't stop at the surface. Keep asking "Why?":

```
Problem: Admin dashboard crashes
1. Why? → NullPointerException in getFileSize()
2. Why? → fileSize is null for some records
3. Why? → The upload didn't set the fileSize field
4. Why? → MultipartFile.getSize() returns 0 for empty files, not null
5. Why? → We don't validate file size before saving
→ Root cause: Missing input validation
```

### The "What Could Go Wrong?" Checklist

Before writing any feature, ask:
- [ ] What if the input is null?
- [ ] What if the input is empty?
- [ ] What if the input is malicious?
- [ ] What if two users do this simultaneously?
- [ ] What if the database is down?
- [ ] What if the disk is full?
- [ ] What if the network is slow?
- [ ] What if someone tries this 1000 times per second?
- [ ] What happens to child entities when a parent is modified?
- [ ] What does the user see when this fails?

---

## 13.2 How to Debug Systems

### The Systematic Approach

```
1. REPRODUCE: Can you make the bug happen consistently?
   → If not, look for race conditions or intermittent failures

2. ISOLATE: Which layer is the problem in?
   Browser? → Check browser console (F12)
   Controller? → Add log statements at entry/exit
   Service? → Check method inputs/outputs
   Repository? → Enable SQL logging (show-sql=true)
   Database? → Query the database directly with psql

3. HYPOTHESIZE: What do you think is wrong?
   → Write it down. Be specific.

4. TEST: Verify your hypothesis with minimal changes
   → Don't change 10 things at once. Change one thing.

5. FIX: Make the smallest change that fixes the issue
   → "Smallest" = least risk of breaking something else

6. VERIFY: Confirm the fix works AND doesn't break anything else
   → Run all tests. Check related features.
```

### Using Logs Effectively

```java
// BAD: Useless, no context
log.info("Entering method");
log.info("Done");

// GOOD: Tells you exactly what happened and why
log.info("Uploading file '{}' ({} bytes) for user '{}' to folder {}",
         file.getOriginalFilename(), file.getSize(), user.getUsername(), folderId);

// GOOD: Error with full context + stack trace
log.error("Failed to store file '{}' on node '{}': {}",
          originalName, node, e.getMessage(), e);
// The 'e' at the end (after the format string) logs the full stack trace
```

---

## 13.3 How to Review Code

### The Code Review Checklist (Senior Engineer Perspective)

| Category | Questions to Ask |
|----------|-----------------|
| **Correctness** | Does this do what it claims? Are edge cases handled (null, empty, max values)? |
| **Security** | Can user input be trusted? Is ownership verified? Can this be exploited? |
| **Performance** | Are there N+1 queries? Is data loaded that isn't needed? Is pagination used? |
| **Maintainability** | Will a new developer understand this in 6 months? Are names descriptive? |
| **Testing** | Is this testable? Are there unit tests? What's the test coverage? |
| **Error handling** | What happens when things go wrong? Are errors logged with context? |
| **Naming** | Do variable/method names describe their purpose accurately? |
| **DRY** | Is logic duplicated elsewhere? Should it be extracted? |
| **Transactions** | Are database operations wrapped in `@Transactional`? |
| **Concurrency** | What if two requests hit this simultaneously? |

---

## 13.4 The SOLID Principles Applied to CloudNest

| Principle | Meaning | CloudNest Example |
|-----------|---------|-------------------|
| **S**ingle Responsibility | One class = one job | `UserService` only handles users, not files |
| **O**pen/Closed | Open for extension, closed for modification | Adding a new file type doesn't change `FileStorageService` |
| **L**iskov Substitution | Subtypes should be substitutable | `FileNotFoundException` extends `RuntimeException` — works anywhere a `RuntimeException` is expected |
| **I**nterface Segregation | Don't force implementations of unused methods | Repository interfaces only declare needed query methods |
| **D**ependency Inversion | Depend on abstractions, not concretions | Services depend on `JpaRepository` interface, not the auto-generated proxy class |

---

## 13.5 Common Beginner Mistakes

| Mistake | Why It's Bad | The Fix |
|---------|-------------|---------|
| Using `System.out.println()` | Can't filter, search, or route logs | Use SLF4J (`log.info()`, `log.error()`) |
| Catching `Exception` silently | Hides bugs, impossible to debug | Always log with full stack trace |
| Not validating input | Security vulnerabilities, crashes | Validate at controller layer (`@Valid`) |
| Hardcoding values | Can't change without recompiling | Use `application.properties` + `@Value` |
| Loading all records into memory | `OutOfMemoryError` at scale | Use pagination (`Pageable`) |
| Not using transactions | Data inconsistency on failures | Add `@Transactional` to service methods |
| Exposing entities to views | `LazyInitializationException`, security leaks | Convert to DTOs before passing to templates |
| Not indexing foreign keys | Slow queries at scale | Add indexes on all FK columns |
| Ignoring the audit report | Technical debt accumulates | Address bugs by priority (critical → high → medium) |
| Writing code without tests | Regressions go unnoticed | Write unit tests for services, integration tests for controllers |

---

## 13.6 Interview Preparation — Questions About This Project

### Spring Boot Questions

| Question | Key Points to Cover |
|----------|-------------------|
| "What is dependency injection?" | IoC container creates and wires beans; constructor injection is preferred over `@Autowired` |
| "Explain `@Transactional`" | ACID properties, rollback on unchecked exceptions, proxy-based AOP |
| "How does Spring Security work?" | Filter chain, `UserDetailsService`, `BCryptPasswordEncoder`, session management |
| "What's the N+1 problem?" | Lazy loading causes extra queries; fix with `JOIN FETCH` or DTO projections |
| "Monolith vs microservices?" | Start monolith; split when team/product complexity justifies it |
| "How does `@Version` work?" | Optimistic locking — adds `WHERE version=?` to UPDATEs; throws exception on conflict |

### Database Questions

| Question | Key Points to Cover |
|----------|-------------------|
| "What is an index?" | B-tree for fast lookups; trade-off: faster reads, slower writes |
| "SQL injection prevention?" | Parameterized queries — JPA uses them by default; never concatenate user input into SQL |
| "What is normalization?" | Reduce data redundancy; CloudNest schema is in 3NF |
| "Explain ACID" | **A**tomicity (all or nothing), **C**onsistency (valid state), **I**solation (no interference), **D**urability (persisted) |
| "Soft delete vs hard delete?" | Soft = set flag, preserves data, allows restore; Hard = remove from DB, saves space |
| "What is a foreign key?" | A column that references another table's primary key, enforcing referential integrity |

### Architecture Questions

| Question | Key Points to Cover |
|----------|-------------------|
| "Explain your project architecture" | Layered MVC: Controller → Service → Repository → Database. Thymeleaf for views. |
| "How would you scale this?" | Horizontal: load balancer, shared DB, object storage (S3), Redis sessions |
| "What design patterns did you use?" | Repository, DTO, Builder (Lombok), Strategy (storage nodes), Observer (scheduler) |
| "How do you handle file deduplication?" | Content-addressable storage: SHA-256 hash before write, check DB, reuse if exists |
| "What happens when a file is deleted?" | Soft-delete first (30-day trash), then permanent delete with dedup-aware disk cleanup |
| "How does your sharing work?" | UUID token with 7-day expiry; public endpoint (`/share/**` is `permitAll()`) |

### Security Questions

| Question | Key Points to Cover |
|----------|-------------------|
| "How do you store passwords?" | BCrypt with cost factor 12 — intentionally slow to prevent brute-force |
| "What is CSRF?" | Cross-Site Request Forgery; prevented by unique token in every form submission |
| "How do you prevent privilege escalation?" | Never trust user input for roles; registration always assigns `ROLE_USER` |
| "How do you verify file ownership?" | Every file/folder query includes `user_id` filter; checked in service layer |

---

## 13.7 Your Next Steps

### Immediate (This Week)
1. ✅ Read all 6 parts of this guide
2. 📝 Add unit tests for `UserService.registerUser()` — test duplicate username, password mismatch
3. 📝 Add unit tests for `FileStorageService.uploadFile()` — test quota exceeded, dedup hit
4. 🔧 Fix BUG-08 if not already fixed (remove `role` field from `UserRegistrationDto`)

### Short-Term (This Month)
5. 📦 Add Flyway migrations and set `ddl-auto=validate`
6. 📄 Add pagination to file listing and admin panel
7. 🐳 Create a `Dockerfile` and `docker-compose.yml`
8. 🧪 Add integration tests that boot the full Spring context

### Medium-Term (Next 2–3 Months)
9. 🏗️ Extract `AdminService` and `DashboardService` from controllers
10. 🔍 Add full-text search with PostgreSQL `tsvector`
11. 💾 Add caching with Caffeine for dashboard stats
12. 📊 Add Spring Boot Actuator metrics + Prometheus integration

### Long-Term (6+ Months)
13. ☁️ Replace local storage with S3/MinIO
14. 🔐 Add OAuth2 / TOTP-based 2FA
15. ⚛️ Consider React frontend consuming a REST API
16. 📈 Add rate limiting with Bucket4j

---

# 🎓 Final Words

Congratulations on completing this guide. Here's what you should now be able to do:

✅ **Explain** every file in the CloudNest project and why it exists
✅ **Draw** the architecture diagram from memory
✅ **Trace** any request from browser to database and back
✅ **Identify** security vulnerabilities and explain how to fix them
✅ **Discuss** production engineering concepts (caching, scaling, concurrency)
✅ **Answer** common interview questions about Spring Boot, databases, and architecture
✅ **Plan** how to evolve this into a production SaaS product
✅ **Debug** issues systematically using logs, SQL, and the 5 Whys
✅ **Review** code like a senior engineer using the checklist above

> *"The best way to learn architecture is to build something, break it, understand why it broke, and build it better."*
>
> — Every senior engineer, ever.

---

*This document was generated from a complete analysis of the CloudNest codebase. Every code reference, every file path, and every architectural observation is based on the actual source code.*


