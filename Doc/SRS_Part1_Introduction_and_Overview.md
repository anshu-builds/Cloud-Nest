# Software Requirements Specification (SRS)

## Project: CloudNest — Enterprise Distributed Cloud Storage System

### Part 1 of 4: Introduction, Scope & Overall Description

| Field             | Value                                          |
| ----------------- | ---------------------------------------------- |
| **Document ID**   | CN-SRS-2026-v2.0                               |
| **Version**       | 2.0 (Comprehensive Edition)                    |
| **Date**          | June 01, 2026                                  |
| **Author**        | Anmol Raj                                      |
| **Project Name**  | CloudNest                                       |
| **Artifact ID**   | `com.cloudnest:cloudnest:1.0.0`                |
| **Java Version**  | 21 (LTS)                                       |
| **Spring Boot**   | 3.4.5                                          |

---

## 1. Introduction

### 1.1 Purpose

The purpose of this document is to provide a **complete and authoritative Software Requirements Specification (SRS)** for **CloudNest**, a distributed cloud storage web application developed as an academic capstone project. This document is organized into four parts:

| Part | Title                                       | Coverage                                              |
| ---- | ------------------------------------------- | ----------------------------------------------------- |
| 1    | Introduction, Scope & Overall Description   | Project context, stakeholders, environment             |
| 2    | Functional Requirements                     | Every user-facing and system feature, mapped to code   |
| 3    | System Architecture & Data Models           | Architecture layers, ER diagrams, API endpoint catalog |
| 4    | Non-Functional Requirements & Appendices    | Security, performance, testing, deployment, glossary   |

This SRS conforms to the structure recommended by **IEEE 830-1998** and serves as the definitive reference for development, academic evaluation, and future maintenance.

### 1.2 Scope

CloudNest is a **secure, enterprise-grade cloud storage platform** that simulates the core functionality of services like Google Drive and Dropbox. It allows registered users to:

- Upload, download, preview, search, and organize files within a hierarchical folder system.
- Share files via time-limited, publicly accessible UUID links.
- Benefit from server-side **SHA-256 data deduplication** that eliminates redundant physical storage.
- Experience **simulated distributed storage** across configurable virtual nodes.
- Manage a **soft-delete recycle bin** with automatic 30-day purge.
- Operate within an enforced **1 GB per-user storage quota**.
- (Admin users) Monitor system-wide health, manage users, and delete files globally.

The system is **not** designed for:
- End-to-end encryption of files at rest.
- Real multi-server clustering or cross-datacenter replication (the distributed storage is simulated on the local file system).
- Mobile-native applications (the UI is responsive but browser-based).

### 1.3 Definitions, Acronyms & Abbreviations

| Term / Acronym  | Definition                                                                                     |
| ---------------- | ---------------------------------------------------------------------------------------------- |
| **SRS**          | Software Requirements Specification                                                           |
| **JPA**          | Jakarta Persistence API — Java standard for ORM (Object-Relational Mapping)                   |
| **ORM**          | Object-Relational Mapping — automatic translation between Java objects and database rows       |
| **JPQL**         | Jakarta Persistence Query Language — SQL-like language that operates on entity objects          |
| **BCrypt**       | Adaptive password-hashing function based on the Blowfish cipher                                |
| **SHA-256**      | Secure Hash Algorithm producing a 256-bit (64 hex character) digest                           |
| **UUID**         | Universally Unique Identifier — 128-bit label used for collision-free naming                  |
| **CSRF**         | Cross-Site Request Forgery — an attack that tricks a user's browser into submitting requests   |
| **MIME Type**    | Multipurpose Internet Mail Extensions type — identifies file format (e.g., `application/pdf`) |
| **DTO**          | Data Transfer Object — lightweight POJO used to transfer data between layers                  |
| **CRUD**         | Create, Read, Update, Delete — fundamental database operations                                |
| **MVC**          | Model-View-Controller — architectural pattern separating concerns                              |
| **Soft Delete**  | Marking a record as "deleted" in the database without physically removing it                  |
| **Hard Delete**  | Physically removing both the database record and the file from disk                           |
| **Deduplication**| Eliminating duplicate copies of data by storing only one physical copy                        |
| **Storage Node** | A simulated physical storage partition (directory) representing a server in a cluster          |
| **Quota**        | Maximum storage capacity allocated to a single user (default: 1 GB = 1,073,741,824 bytes)    |
| **Thymeleaf**    | Server-side Java template engine that renders dynamic HTML from model data                    |
| **Lombok**       | Java library that auto-generates boilerplate code (getters, setters, builders) via annotations |
| **Actuator**     | Spring Boot module that exposes operational endpoints (health, info) for monitoring            |

### 1.4 References

| Ref # | Document / Resource                                                      |
| ----- | ------------------------------------------------------------------------ |
| R1    | IEEE Std 830-1998 — Recommended Practice for SRS                         |
| R2    | Spring Boot 3.4.5 Official Documentation                                 |
| R3    | Spring Security Reference Documentation                                  |
| R4    | Hibernate 6.x ORM User Guide                                            |
| R5    | PostgreSQL 12+ Official Documentation                                    |
| R6    | Maven POM Reference — `pom.xml` artifact `com.cloudnest:cloudnest:1.0.0`|
| R7    | OWASP Top 10 — Web Application Security Risks                            |

---

## 2. Overall Description

### 2.1 Product Perspective

CloudNest is a **standalone, self-hosted web application** built on a classic **Client-Server monolithic architecture**. It is not a microservice; all backend logic runs within a single deployable Spring Boot JAR file with an embedded Apache Tomcat servlet container.

```
┌─────────────────────────────────────────────────────────┐
│                  CLIENT (Web Browser)                    │
│  HTML5 + CSS3 + JavaScript (ES6)                        │
│  Three.js (WebGL) · Lucide Icons · Google Fonts (Inter) │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP / HTTPS (Port 8080)
┌──────────────────────▼──────────────────────────────────┐
│              SPRING BOOT APPLICATION                     │
│  ┌──────────┐  ┌──────────┐  ┌────────────┐            │
│  │Controller│→ │ Service  │→ │ Repository │            │
│  │  Layer   │  │  Layer   │  │   Layer    │            │
│  └──────────┘  └──────────┘  └─────┬──────┘            │
│                                     │ JPA / Hibernate   │
│  ┌──────────────────────────────────▼──────────────┐    │
│  │            PostgreSQL Database                   │    │
│  │   Tables: users, folders, files, shared_links   │    │
│  └─────────────────────────────────────────────────┘    │
│                                                          │
│  ┌──────────────────────────────────────────────────┐    │
│  │        Simulated Distributed File System          │    │
│  │   storage/node1/   storage/node2/   storage/node3/│   │
│  └──────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────┘
```

**Key architectural decisions:**

1. **Server-Side Rendering (SSR):** HTML is rendered on the server by Thymeleaf, not by a client-side framework (React/Angular). This simplifies deployment and is well-suited for session-based authentication.
2. **Session-Based Authentication:** Spring Security manages HTTP sessions with `JSESSIONID` cookies. This avoids the complexity of JWT token management for a traditional web app.
3. **Simulated Distribution:** Physical files are distributed across `storage/node1/`, `storage/node2/`, `storage/node3/` directories using random node selection via `ThreadLocalRandom`, demonstrating distributed storage concepts without actual multi-server infrastructure.
4. **Deduplication via Content Hashing:** SHA-256 hashes are computed in-memory before writing to disk (hash-before-write pattern), which avoids TOCTOU (Time-of-Check-Time-of-Use) race conditions.

### 2.2 Product Functions (High-Level Summary)

| # | Function Category                    | Description                                                                     |
|---|--------------------------------------|---------------------------------------------------------------------------------|
| 1 | User Authentication & Authorization  | Registration, login (username or email), role-based access (USER / ADMIN)       |
| 2 | File Management                      | Upload (multi-file, drag-and-drop), download, preview, search, move             |
| 3 | Folder Management                    | Create nested hierarchies, navigate via breadcrumbs, move, download as ZIP      |
| 4 | Data Deduplication                   | SHA-256 content-addressed storage; identical files share one physical copy       |
| 5 | Distributed Storage Simulation       | Random node assignment across 3 configurable virtual nodes                      |
| 6 | Storage Quota Enforcement            | 1 GB per-user limit checked on every upload; human-readable usage display       |
| 7 | File Sharing                         | UUID-based public links with 7-day auto-expiration                              |
| 8 | Recycle Bin (Soft Delete)            | Trash with restore capability; 30-day auto-purge via scheduled task             |
| 9 | Administrative Console               | Global user management, file oversight, role toggle, system statistics           |
| 10| Enterprise Visualization Pages       | Node topology, deduplication center, replication view, network activity, analytics, monitoring |
| 11| Custom Error Pages                   | Branded 404 and 500 error pages instead of raw stack traces                     |

### 2.3 User Classes and Characteristics

| User Class       | Role          | Capabilities                                                                                                        | Quota  |
|------------------|---------------|---------------------------------------------------------------------------------------------------------------------|--------|
| Standard User    | `ROLE_USER`   | Register, login, upload/download/preview files, create/navigate/move/delete folders, share files, manage recycle bin | 1 GB   |
| Administrator    | `ROLE_ADMIN`  | All Standard User capabilities **plus**: access admin dashboard, view all users, toggle user roles, delete any file  | 1 GB   |
| Anonymous Visitor| (none)        | Access shared file links (`/share/{token}`), download shared files, view login/register pages                       | N/A    |

**User role assignment rules:**
- All public registrations are assigned `ROLE_USER` by default. The code explicitly enforces `String finalRole = "ROLE_USER"` in `UserService.registerUser()` to prevent privilege escalation via form manipulation.
- Admin accounts can only be created by an existing admin toggling a user's role via `POST /admin/users/toggle-role/{id}`.
- An admin cannot demote their own account (self-demotion protection).

### 2.4 Operating Environment

#### 2.4.1 Server-Side Requirements

| Component          | Requirement                                        |
| ------------------- | -------------------------------------------------- |
| Operating System    | Windows 10/11 or Linux (Ubuntu 20.04+)             |
| Java Runtime        | JDK 21+ (LTS) — required by Spring Boot 3.4.5      |
| Build Tool          | Apache Maven 3.8.x or higher                       |
| Database Server     | PostgreSQL 12 or higher                            |
| Application Server  | Embedded Apache Tomcat (bundled with Spring Boot)   |
| Default Port        | `8080` (configurable via `server.port`)             |
| IDE (Development)   | IntelliJ IDEA (Community/Ultimate) or Eclipse IDE   |

#### 2.4.2 Client-Side Requirements

| Component           | Requirement                                                |
| -------------------- | ---------------------------------------------------------- |
| Web Browser          | Any modern HTML5-compliant browser (Chrome, Firefox, Edge, Safari) |
| JavaScript           | Must be enabled for interactive features and WebGL background      |
| Screen Resolution    | Responsive design — works on mobile (320px+) and desktop           |

#### 2.4.3 Frontend Dependencies (Loaded via CDN)

| Library        | Version  | Purpose                                         |
| -------------- | -------- | ----------------------------------------------- |
| Lucide Icons   | 0.325.0  | Modern SVG icon library used throughout the UI   |
| Three.js       | (latest) | WebGL 3D animated background on login/register   |
| GSAP           | (latest) | GreenSock Animation Platform for page transitions|
| Google Fonts   | —        | Inter typeface family for consistent typography  |

### 2.5 Design and Implementation Constraints

| Constraint                       | Description                                                                                                              |
| -------------------------------- | ------------------------------------------------------------------------------------------------------------------------ |
| **Language**                     | Java 21 only — leverages modern language features (pattern matching, records)                                             |
| **Framework Lock-in**            | Spring Boot 3.4.5 with Spring Security 6 — all authentication, authorization, and web handling are framework-managed     |
| **Database Dialect**             | PostgreSQL — the schema uses `BIGSERIAL`, PostgreSQL-specific syntax; H2 (PostgreSQL mode) is used for testing           |
| **ORM Constraint**               | Hibernate ORM via Spring Data JPA — no raw JDBC calls; all queries use either derived query methods or JPQL              |
| **File Size Limit**              | 50 MB per individual file upload (configured via `spring.servlet.multipart.max-file-size`)                               |
| **Blocked File Extensions**      | `.exe`, `.bat`, `.sh`, `.ps1`, `.cmd` — rejected at upload time for security                                             |
| **Storage Quota**                | 1 GB per user (1,073,741,824 bytes) — enforced at the service layer before writing to disk                               |
| **Session Management**           | Server-side HTTP sessions; `JSESSIONID` cookie; session invalidation on logout                                           |
| **Template Engine**              | Thymeleaf 3 — all HTML views are server-rendered; no SPA framework                                                       |
| **Password Encoding**            | BCrypt with strength factor 12 — `new BCryptPasswordEncoder(12)`                                                        |
| **Scheduling**                   | `@EnableScheduling` — cron-based trash cleanup runs daily at 02:00 AM (`0 0 2 * * *`)                                   |

### 2.6 Assumptions and Dependencies

| # | Assumption / Dependency                                                                                                       |
|---|-------------------------------------------------------------------------------------------------------------------------------|
| 1 | PostgreSQL server is installed, running, and a database named `cloudnest_db` has been created before application startup.     |
| 2 | The `DB_PASSWORD` environment variable is set (or defined in `.env`) with the correct PostgreSQL password.                    |
| 3 | The application has read/write permissions to the `storage/` directory at the project root for file persistence.              |
| 4 | The host machine has JDK 21 installed and available on the system `PATH`.                                                    |
| 5 | Hibernate's `ddl-auto=update` mode will create/update database tables automatically on startup in development mode.          |
| 6 | CDN-hosted libraries (Lucide, Three.js, Google Fonts) require internet connectivity on the client browser.                   |
| 7 | The `DatabaseMigrationConfig` startup migration (`UPDATE ... SET version = 0 WHERE version IS NULL`) runs safely on both fresh and existing databases. |
| 8 | File deduplication assumes SHA-256 is collision-free for practical purposes (probability of collision is ~2⁻¹²⁸).            |
| 9 | The simulated distributed storage (3 nodes) is for academic demonstration; no actual network partitioning or replication occurs.|
| 10| The application is accessed via HTTP in development; HTTPS/TLS termination would be handled by a reverse proxy in production.|

---

*End of Part 1. Continue to **Part 2: Functional Requirements** for the complete feature specification.*
