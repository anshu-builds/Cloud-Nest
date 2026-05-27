# ☁️ CloudNest — Enterprise Distributed Cloud Storage System

**A Master's Level Software Engineering Project**

CloudNest is a highly sophisticated, secure, and scalable cloud storage web application built using modern Java architecture. It mimics the core functionality of enterprise platforms like Google Drive and Dropbox, introducing advanced backend concepts such as Data Deduplication, simulated Distributed Storage Nodes, and Soft-Delete capabilities.

---

## 🌟 Core Features

### 1. Advanced Storage Management
*   **Data Deduplication (SHA-256):** Prevents storing duplicate files on the hard drive. If 10 users upload the exact same file, the server only saves one physical copy and maps references to the users, saving massive amounts of disk space.
*   **Distributed Storage Simulation:** Files are dynamically routed to simulated storage "nodes" (e.g., `NODE-ALPHA`, `NODE-BETA`) based on file size, demonstrating load-balancing concepts.
*   **Storage Quotas:** Enforces a strict 1GB storage limit per user, preventing server overload. Real-time usage is calculated and displayed visually via a progress bar on the dashboard.

### 2. Comprehensive File & Folder Architecture
*   **Hierarchical Structure:** Supports infinite nesting of folders (folders within folders) using self-referencing database entities.
*   **Drag-and-Drop Multi-Upload:** Users can seamlessly drag and drop multiple files at once into the browser to upload them concurrently.
*   **Move Functionality:** Files and folders can be seamlessly moved between parent folders without moving physical files on the disk (database pointer updates).
*   **On-the-Fly ZIP Downloads:** Entire folders, including all nested sub-folders and files, are recursively zipped in-memory and streamed to the user as a single `.zip` file download.

### 3. File Lifecycle & Security
*   **Soft Delete & Recycle Bin:** Deleting a file doesn't immediately remove it from the system. It sets an `isDeleted` flag, moving it to the Recycle Bin. Users can permanently delete or restore files.
*   **Shareable Links:** Generates secure, unique UUID-based URLs for files, allowing them to be shared globally.
*   **Role-Based Security:** Fully secured endpoints using Spring Security and BCrypt password hashing. Users cannot access or modify files belonging to other users.
*   **In-Browser Previews:** Images, PDFs, and text files are streamed directly to the browser for previewing without needing to download them.

### 4. Premium User Interface (UI/UX)
*   **Glassmorphism Aesthetic:** Designed using modern UI principles featuring frosted glass effects, subtle gradients, and floating micro-animations.
*   **Dark/Light Mode Toggle:** Implements an enterprise-standard theme toggle, persisting the user's preference in the browser's `localStorage`.
*   **Custom Error Pages:** Professional 404, 403, and 500 error pages to ensure users never see an unhandled "Whitelabel" server stack trace.

---

## 🛠️ Technology Stack

### Backend Infrastructure
*   **Language:** Java 21
*   **Framework:** Spring Boot 3.2.x
*   **Security:** Spring Security (Form Login, CSRF Protection)
*   **Data Access:** Spring Data JPA (Hibernate)
*   **Database:** MySQL 8.0
*   **Build Tool:** Maven

### Frontend Stack
*   **Templating Engine:** Thymeleaf (Server-side rendering)
*   **Styling:** Custom Vanilla CSS3 + CSS Variables
*   **Framework:** Bootstrap 5.3 (Grid System & Modals)
*   **Icons:** Bootstrap Icons
*   **Scripting:** Vanilla JavaScript (ES6)

---

## 🏗️ System Architecture

CloudNest utilizes a strict **N-Tier Layered Architecture** to enforce separation of concerns:

1.  **Presentation Layer (`Controller`):** Handles incoming HTTP requests, session management, and returns Thymeleaf views (e.g., `FileController`, `AuthController`).
2.  **Business Logic Layer (`Service`):** Contains the core logic. `FileStorageService` manages deduplication hashes and physical I/O streams. `FolderService` handles the recursive logic for ZIP building and directory traversal.
3.  **Data Access Layer (`Repository`):** Spring Data JPA interfaces that execute complex derived queries (e.g., `findByUserAndParentIsNullAndIsDeletedFalse()`).
4.  **Domain Layer (`Entity`):** Represents the database schema (`User`, `FileEntity`, `Folder`).

### Database Schema (ERD Overview)
*   **`users` table:** `id`, `username`, `password`, `email`, `role`, `created_at`.
*   **`folders` table:** `id`, `name`, `parent_id` (FK to self), `user_id` (FK), `is_deleted`.
*   **`files` table:** `id`, `original_name`, `stored_name`, `file_type`, `size`, `hash` (for deduplication), `storage_node`, `folder_id` (FK), `user_id` (FK), `is_deleted`.

---

## 🚀 Setup & Execution Guide

1.  **Database Configuration:**
    *   Ensure MySQL is running.
    *   Create a blank schema: `CREATE DATABASE cloudnest;`
    *   Update `application.properties`:
        ```properties
        spring.datasource.url=jdbc:mysql://localhost:3306/cloudnest
        spring.datasource.username=root
        spring.datasource.password=YOUR_PASSWORD
        spring.jpa.hibernate.ddl-auto=update
        ```

2.  **Storage Configuration:**
    *   By default, physical files are stored in `C:/CloudNestStorage/` (or relative path defined in properties). Ensure the application has read/write permissions to this directory.

3.  **Run the Application:**
    *   Run `CloudNestApplication.java` from IntelliJ IDEA or Eclipse.
    *   Alternatively, run via Maven: `mvn spring-boot:run`
    *   Navigate to `http://localhost:8080` in any modern web browser.

---
*Developed for advanced academic demonstration. Exhibits high proficiency in Java Spring Boot, Relational Database Management, and UI/UX Design.*
