# Software Requirements Specification (SRS)
## Project: CloudNest - Enterprise Distributed Cloud Storage System

---

## 1. Introduction
### 1.1 Purpose
The purpose of this document is to define the Software Requirements Specification (SRS) for "CloudNest", a distributed cloud storage web application. This document outlines the functional and non-functional requirements, system architecture, and constraints for the development and academic demonstration of the project.

### 1.2 Scope
CloudNest is a secure, highly scalable cloud storage platform designed to allow users to securely upload, manage, share, and organize their files in a hierarchical folder structure. The system implements enterprise-level optimizations including Data Deduplication, simulated Distributed Node routing, and Storage Quotas.

---

## 2. Overall Description
### 2.1 Product Perspective
CloudNest operates as a standalone web application utilizing a Client-Server architecture. The frontend is a responsive web interface rendered via Thymeleaf, and the backend is powered by a Java Spring Boot RESTful monolith connected to a MySQL relational database. The file system of the host server acts as the primary simulated distributed storage mechanism.

### 2.2 User Classes and Characteristics
*   **Standard User:** Can register an account, upload files, create folders, share links, and manage their recycle bin. Subject to a 1GB storage quota.

### 2.3 Operating Environment
*   **Server OS:** Windows / Linux
*   **Database:** MySQL 8.0+
*   **Runtime Environment:** Java Runtime Environment (JRE) 21+
*   **Client:** Any modern HTML5-compliant web browser (Chrome, Firefox, Edge, Safari).

---

## 3. Functional Requirements

### 3.1 Authentication & Authorization
*   **FR-1.1:** The system shall allow users to register with a unique username and password.
*   **FR-1.2:** Passwords shall be cryptographically hashed using BCrypt before storage.
*   **FR-1.3:** The system shall authenticate users via Form Login and maintain session security.

### 3.2 File & Folder Management
*   **FR-2.1:** The system shall allow users to upload multiple files simultaneously via drag-and-drop.
*   **FR-2.2:** The system shall support the creation of nested folder hierarchies.
*   **FR-2.3:** The system shall allow users to move files and folders between different parent directories.
*   **FR-2.4:** The system shall provide a search functionality to query files by name.

### 3.3 Storage Optimization & Limits
*   **FR-3.1 (Deduplication):** The system shall compute a SHA-256 hash of all uploaded files. If a matching hash exists globally, the system shall map the new record to the existing physical file rather than duplicating it on disk.
*   **FR-3.2 (Quotas):** The system shall restrict each user to a maximum of 1GB of total stored data.
*   **FR-3.3 (Node Distribution):** The system shall allocate files to different simulated physical storage nodes (e.g., `NODE-ALPHA`, `NODE-BETA`) based on file size thresholds.

### 3.4 File Operations & Sharing
*   **FR-4.1:** The system shall generate unique, secure UUID-based URLs to allow unauthenticated downloading of shared files.
*   **FR-4.2:** The system shall allow users to download entire folders recursively compiled into a single ZIP archive on-the-fly.
*   **FR-4.3:** The system shall provide in-browser preview capabilities for supported MIME types (images, pdfs, plain text).

### 3.5 Lifecycle Management
*   **FR-5.1:** Deleted items shall be soft-deleted and moved to a "Recycle Bin".
*   **FR-5.2:** Users shall be able to restore items from the Recycle Bin to their original locations.
*   **FR-5.3:** Permanent deletion shall physically remove the file from the disk only if its global deduplication reference count reaches zero.

---

## 4. Non-Functional Requirements

### 4.1 Security
*   **NFR-1.1:** The system shall implement CSRF (Cross-Site Request Forgery) protection on all state-changing endpoints.
*   **NFR-1.2:** Database queries shall use parameterized JPA queries to prevent SQL Injection attacks.
*   **NFR-1.3:** Users shall not be able to traverse directories or access files belonging to other users (Horizontal Privilege Escalation protection).

### 4.2 Performance
*   **NFR-2.1:** Folder ZIP generation must occur via direct output stream buffering to prevent server RAM exhaustion.
*   **NFR-2.2:** The system should handle concurrent file uploads without database deadlock.

### 4.3 Usability
*   **NFR-3.1:** The User Interface shall be fully responsive and functional on both desktop and mobile devices.
*   **NFR-3.2:** The system shall provide a Light/Dark theme toggle mechanism.
*   **NFR-3.3:** The system shall provide custom user-friendly error pages (404, 403, 500) rather than exposing server stack traces.

---

## 5. System Models
*   **Data Model:** Managed via Hibernate ORM mapping to a normalized relational schema consisting of `User`, `Folder`, and `FileEntity` tables utilizing self-referencing foreign keys for hierarchical structures.
