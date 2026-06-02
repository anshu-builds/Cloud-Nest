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
