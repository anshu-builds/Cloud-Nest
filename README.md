<div align="center">

# ☁️ CloudNest

### A Distributed Cloud File Storage Platform

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Thymeleaf](https://img.shields.io/badge/Thymeleaf-3.1-005F0F?style=for-the-badge&logo=thymeleaf&logoColor=white)](https://www.thymeleaf.org/)
[![License](https://img.shields.io/badge/License-MIT-blue?style=for-the-badge)](LICENSE)

<br/>

**CloudNest** is a full-stack distributed cloud storage system inspired by Google Drive. It features a premium dark glassmorphism UI, simulated multi-node file distribution, folder management, file sharing, trash recovery, and an admin dashboard — all built with enterprise-grade Java patterns.

<br/>

<img src="https://img.shields.io/badge/⚡_Live_Demo-Not_Available-gray?style=for-the-badge" alt="Demo"/>

</div>

---

## ✨ Key Features

<table>
<tr>
<td width="50%">

### 📁 File Management
- **Upload & Download** — Drag-and-drop with 50MB max
- **Nested Folders** — Full folder hierarchy with breadcrumbs
- **Smart Search** — Filter by name, type, or extension
- **Trash & Recovery** — Soft delete with 30-day auto-cleanup

</td>
<td width="50%">

### 🔗 Sharing & Collaboration
- **Shareable Links** — Token-based public links with 7-day expiry
- **Public Downloads** — No auth required for shared files
- **Link Management** — Generate, view, and revoke links

</td>
</tr>
<tr>
<td width="50%">

### 🖥️ Dashboard & Analytics
- **Real-time Stats** — File count, storage usage, node distribution
- **Recent Activity** — Track recent uploads and actions
- **Storage Quota** — Visual usage meters per user
- **Admin Panel** — User management and system monitoring

</td>
<td width="50%">

### 🌐 Distributed Storage
- **3-Node Simulation** — Files distributed across `node1`, `node2`, `node3`
- **Node Health Monitoring** — Visual status for each storage node
- **Replication Awareness** — Designed for multi-node architecture
- **Network Topology View** — Visualize node interconnections

</td>
</tr>
</table>

---

## 🛠️ Tech Stack

| Layer | Technology | Purpose |
|-------|-----------|---------|
| **Language** | Java 21 | Core application logic |
| **Framework** | Spring Boot 3.4.5 | Application framework & DI |
| **Security** | Spring Security | Auth, CSRF, session management |
| **ORM** | Spring Data JPA + Hibernate | Database abstraction |
| **Database** | PostgreSQL | Persistent data storage |
| **Templating** | Thymeleaf | Server-side HTML rendering |
| **Frontend** | Custom CSS + Three.js + Lucide | Premium dark UI with WebGL |
| **Build** | Maven | Dependency management |
| **Testing** | JUnit 5 + Mockito + H2 | Unit & integration tests |

---

## 🏗️ Architecture

```
com.cloudnest/
│
├── CloudNestApplication.java         # Spring Boot entry point
│
├── config/                            # App & migration configuration
│   ├── AppConfig.java
│   └── DatabaseMigrationConfig.java
│
├── security/                          # Spring Security setup
│   └── SecurityConfig.java
│
├── entity/                            # JPA domain models
│   ├── User.java
│   ├── FileEntity.java
│   ├── Folder.java
│   └── SharedLink.java
│
├── dto/                               # Data Transfer Objects
│   ├── RegisterRequest.java
│   ├── FileSearchResult.java
│   ├── StorageStats.java
│   └── UserStats.java
│
├── repository/                        # Spring Data JPA repositories
│   ├── UserRepository.java
│   ├── FileRepository.java
│   ├── FolderRepository.java
│   └── SharedLinkRepository.java
│
├── service/                           # Business logic layer
│   ├── UserService.java
│   ├── FileStorageService.java
│   ├── FolderService.java
│   ├── SharedLinkService.java
│   ├── StorageNodeService.java
│   └── TrashCleanupScheduler.java
│
├── controller/                        # MVC web controllers
│   ├── AuthController.java
│   ├── DashboardController.java
│   ├── FileController.java
│   ├── FolderController.java
│   ├── ShareController.java
│   ├── TrashController.java
│   └── AdminController.java
│
├── exception/                         # Custom exceptions & handlers
│   ├── FileNotFoundException.java
│   ├── StorageException.java
│   └── GlobalExceptionHandler.java
│
└── util/                              # Utility classes
    └── FileUtils.java
```

**Simulated Distributed Storage:**
```
storage/
├── node1/    ← Randomly assigned storage
├── node2/    ← Randomly assigned storage
└── node3/    ← Randomly assigned storage
```

---

## 🚀 Quick Start

### Prerequisites

| Tool | Version | Link |
|------|---------|------|
| **Java JDK** | 21+ | [Download](https://adoptium.net/) |
| **PostgreSQL** | 12+ | [Download](https://www.postgresql.org/download/) |
| **Maven** | 3.9+ | [Download](https://maven.apache.org/) (or use bundled `mvnw`) |
| **IntelliJ IDEA** | Any | [Download](https://www.jetbrains.com/idea/) |

### 1️⃣ Clone the Repository

```bash
git clone https://github.com/anshu-builds/Cloud-Nest.git
cd Cloud-Nest
```

### 2️⃣ Set Up the Database

```sql
CREATE DATABASE cloudnest_db;
```

### 3️⃣ Configure Environment

Create a `.env` file in the project root (or update `application.properties`):

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/cloudnest_db
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 4️⃣ Enable Lombok (IntelliJ)

1. **File → Settings → Plugins** → Install `Lombok`
2. **File → Settings → Build → Compiler → Annotation Processors** → ✅ Enable

### 5️⃣ Run the Application

```bash
# Using Maven wrapper
./mvnw spring-boot:run

# Or via IntelliJ
# Open CloudNestApplication.java → Click ▶ Run
```

### 6️⃣ Open in Browser

```
http://localhost:8080
```

---

## 🔌 API Reference

### Authentication
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/login` | Public | Login page |
| `GET` | `/register` | Public | Registration page |
| `POST` | `/register` | Public | Process registration |
| `POST` | `/logout` | User | End session |

### File Operations
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/files` | User | List all files |
| `POST` | `/files/upload` | User | Upload a file (max 50MB) |
| `GET` | `/files/download/{id}` | User | Download a file |
| `POST` | `/files/delete/{id}` | User | Move file to trash |
| `GET` | `/files/search?query=...` | User | Search files |

### Folder Operations
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/folders/create` | User | Create a folder |
| `POST` | `/folders/delete/{id}` | User | Delete a folder |
| `GET` | `/files?folderId={id}` | User | Browse folder contents |

### Sharing
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `POST` | `/share/generate/{fileId}` | User | Generate shareable link |
| `GET` | `/share/{token}` | Public | View shared file |
| `GET` | `/share/download/{token}` | Public | Download shared file |

### Trash & Recovery
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/trash` | User | View trashed files |
| `POST` | `/trash/restore/{id}` | User | Restore from trash |
| `POST` | `/trash/permanent-delete/{id}` | User | Permanently delete |

### Admin
| Method | Endpoint | Auth | Description |
|--------|----------|------|-------------|
| `GET` | `/admin` | Admin | Admin dashboard |
| `GET` | `/dashboard` | User | User dashboard & stats |

---

## 🗄️ Database Schema

```
┌──────────────┐     ┌──────────────────┐     ┌──────────────┐
│    users      │     │   file_entity    │     │   folders     │
├──────────────┤     ├──────────────────┤     ├──────────────┤
│ id (PK)      │◄────│ user_id (FK)     │     │ id (PK)      │
│ username     │     │ id (PK)          │     │ name         │
│ email        │     │ file_name        │     │ user_id (FK) │──► users
│ password     │     │ file_size        │     │ parent_id    │──► folders (self)
│ role         │     │ content_type     │     │ created_at   │
│ created_at   │     │ storage_node     │     └──────────────┘
└──────────────┘     │ storage_path     │
                     │ folder_id (FK)   │──► folders
                     │ trashed          │
                     │ trashed_at       │     ┌──────────────────┐
                     │ uploaded_at      │     │  shared_links     │
                     └──────────────────┘     ├──────────────────┤
                                              │ id (PK)          │
                                              │ token (unique)   │
                                              │ file_id (FK)     │──► file_entity
                                              │ created_at       │
                                              │ expires_at       │
                                              └──────────────────┘
```

---

## 🧪 Testing

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=AuthIntegrationTest

# Run with verbose output
./mvnw test -X
```

**Test Coverage:**

| Category | Tests | Description |
|----------|-------|-------------|
| Integration — Auth | `AuthIntegrationTest` | Registration, login, session handling |
| Integration — Dashboard | `DashboardAndAdminIntegrationTest` | Stats, admin access control |
| Integration — Files | `FileIntegrationTest` | Upload, download, delete, search |
| Integration — Folders | `FolderIntegrationTest` | Create, delete, nested folders |
| Integration — Sharing | `ShareIntegrationTest` | Link generation, public access |
| Integration — Trash | `TrashIntegrationTest` | Soft delete, restore, permanent delete |

---

## 🎨 UI Design

The frontend uses a **custom enterprise design system** with:

- 🌑 **Dark glassmorphism theme** — Semi-transparent surfaces with backdrop blur
- ✨ **Neon glow effects** — Blue, purple, and cyan accent glows
- 🎬 **WebGL background** — Three.js animated particle scene
- 📐 **Responsive layout** — Collapsible sidebar, mobile-optimized
- ⚡ **Micro-interactions** — Hover effects, slide-in animations, smooth transitions
- 🔤 **Premium typography** — Inter + JetBrains Mono via Google Fonts

**Pages:** Login · Register · Dashboard · Files · Nodes · Monitoring · Analytics · Network · Replication · Deduplication · Trash · Shared Files · Admin Panel

---

## 📂 Project Structure

```
Cloud-Nest/
├── src/
│   ├── main/
│   │   ├── java/com/cloudnest/       # Java source code
│   │   └── resources/
│   │       ├── application.properties # App configuration
│   │       ├── static/                # CSS, JS, images
│   │       │   ├── css/
│   │       │   │   ├── design-system.css   # Full design system
│   │       │   │   └── animations.css      # Animation library
│   │       │   ├── js/
│   │       │   │   └── webgl-scene.js      # Three.js background
│   │       │   └── images/
│   │       │       └── logo.svg
│   │       └── templates/             # Thymeleaf HTML templates
│   └── test/                          # JUnit 5 test suites
├── schema.sql                         # Database DDL script
├── pom.xml                            # Maven configuration
├── mvnw / mvnw.cmd                    # Maven wrapper
└── README.md
```

## 👥 Team

| Name | Role | Contributions |
|------|------|---------------|
| **Anshu Jaiswal** | 👑 Team Lead · Backend Developer · Frontend Designer | Project architecture, Spring Boot backend, REST APIs, database design, security, UI/UX design system, deployment |
| **Anmol Raj** | 🎨 Frontend Developer | Frontend development, Thymeleaf template implementation |
| **Javasingh** | 🧪 QA Tester | Testing, quality assurance, bug tracking, integration test suites |

---

## 🤝 Contributing

1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/amazing-feature`
3. **Commit** your changes: `git commit -m "feat: add amazing feature"`
4. **Push** to the branch: `git push origin feature/amazing-feature`
5. **Open** a Pull Request

---

## 📝 License

This project is open source and available under the [MIT License](LICENSE).

---

<div align="center">

**Built with ❤️ using Spring Boot & Java 21**

[![GitHub](https://img.shields.io/badge/GitHub-anshu--builds-181717?style=flat-square&logo=github)](https://github.com/anshu-builds)

</div>
