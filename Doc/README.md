# ☁️ CloudNest — Mini Google Drive Clone

A simplified **distributed cloud file storage system** built with Java 21, Spring Boot, and MySQL. Designed as a university Masters-level project that demonstrates enterprise architecture patterns while remaining beginner-friendly.

---

## 🚀 Features

- **User Authentication** — Register, Login, Logout (session-based with Spring Security + BCrypt)
- **File Management** — Upload, Download, Delete files (max 50MB)
- **Folder Management** — Create nested folders, organize files
- **Search** — Find files by name or type
- **File Sharing** — Generate public shareable links (7-day expiry)
- **Dashboard** — Statistics: total files, storage usage, recent uploads, node distribution
- **Simulated Distributed Storage** — Files randomly stored across `storage/node1`, `node2`, `node3`

---

## 🛠️ Tech Stack

| Layer        | Technology                   |
|-------------|------------------------------|
| Language     | Java 21                      |
| Framework    | Spring Boot 3.4.5            |
| Security     | Spring Security (Sessions)   |
| Database     | MySQL + Spring Data JPA      |
| Templates    | Thymeleaf                    |
| Frontend     | Bootstrap 5.3 + Custom CSS   |
| Build Tool   | Maven                        |
| Code Gen     | Lombok                       |

---

## 📁 Architecture

```
com.cloudnest/
├── config/         → App configuration (storage paths)
├── security/       → Spring Security configuration
├── entity/         → JPA entities (User, FileEntity, Folder, SharedLink)
├── dto/            → Data Transfer Objects
├── repository/     → Spring Data JPA repositories
├── service/        → Business logic layer
├── controller/     → MVC controllers (web endpoints)
└── exception/      → Custom exceptions + global handler
```

---

## ⚡ How to Run in IntelliJ IDEA

### Step 1: Prerequisites
- **Java 21** (JDK) — [Download](https://adoptium.net/)
- **MySQL 8.0+** — [Download](https://dev.mysql.com/downloads/)
- **IntelliJ IDEA** (Community or Ultimate)
- **Maven** (bundled with IntelliJ)

### Step 2: Database Setup
1. Open MySQL Workbench or terminal
2. Run:
   ```sql
   CREATE DATABASE cloudnest_db;
   ```
3. Update `src/main/resources/application.properties` if your MySQL credentials differ:
   ```properties
   spring.datasource.username=root
   spring.datasource.password=root
   ```

### Step 3: Open in IntelliJ
1. **File → Open** → Select the project folder
2. IntelliJ will detect `pom.xml` and import as a Maven project
3. Wait for dependencies to download (check bottom progress bar)

### Step 4: Enable Lombok
1. **File → Settings → Plugins** → Search "Lombok" → Install
2. **File → Settings → Build → Compiler → Annotation Processors** → ✅ Enable

### Step 5: Run
1. Open `CloudNestApplication.java`
2. Click the green ▶ **Run** button
3. Open browser: **http://localhost:8080**

---

## 🔌 API Endpoints

| Method | URL                        | Auth     | Description              |
|--------|----------------------------|----------|--------------------------|
| GET    | `/login`                   | Public   | Login page               |
| GET    | `/register`                | Public   | Registration page        |
| POST   | `/register`                | Public   | Process registration     |
| GET    | `/dashboard`               | Required | Dashboard with stats     |
| GET    | `/files`                   | Required | File listing             |
| POST   | `/files/upload`            | Required | Upload a file            |
| GET    | `/files/download/{id}`     | Required | Download a file          |
| POST   | `/files/delete/{id}`       | Required | Delete a file            |
| GET    | `/files/search?query=...`  | Required | Search files             |
| POST   | `/folders/create`          | Required | Create folder            |
| POST   | `/folders/delete/{id}`     | Required | Delete folder            |
| POST   | `/share/generate/{fileId}` | Required | Generate share link      |
| GET    | `/share/{token}`           | Public   | View shared file         |
| GET    | `/share/download/{token}`  | Public   | Download shared file     |

---

## 🗄️ Database Tables

- **users** — Registered users with BCrypt-hashed passwords
- **files** — File metadata (name, size, type, storage node, owner)
- **folders** — Folder hierarchy with self-referencing parent
- **shared_links** — Shareable file links with expiration

See `schema.sql` for the full DDL script.

---

## 📦 Simulated Distributed Storage

Files are randomly assigned to one of three storage nodes:
```
storage/
├── node1/    ← Physical files stored here
├── node2/    ← Physical files stored here
└── node3/    ← Physical files stored here
```
This demonstrates distributed storage concepts. In production, these would be separate servers. The `StorageNodeService` handles node selection using random assignment.

---

## 🎨 UI Features

- **Dark glassmorphism theme** with gradient accents
- **Animated background shapes** on auth pages
- **Drag & drop** file upload
- **Responsive** — works on mobile and desktop
- **Micro-animations** — hover effects, slide-in cards, auto-dismiss alerts

---

## 📝 License

This project is for educational purposes. Built as a university group project.
