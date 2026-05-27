# 🧠 CloudNest — Deep Dive Understanding Guide

*This document is written specifically for you to understand exactly how the internal code of CloudNest works, so you can confidently answer any questions during your presentation or viva.*

---

## 1. The Core Architecture (How the code is structured)
Your project follows a standard **Spring Boot N-Tier Architecture**. This means the code is divided into specific layers, and they only talk to each other in one direction:
`Client (Browser)` ➡️ `Controller` ➡️ `Service` ➡️ `Repository` ➡️ `Database`

*   **Controllers (`@Controller`):** These act as the "traffic cops". When you click a button in the browser, the request hits a Controller. The controller doesn't do heavy lifting; it just passes the request to the Service.
*   **Services (`@Service`):** This is the "brain". All your complex logic (hashing, zipping, checking quotas) lives here.
*   **Repositories (`@Repository`):** These talk directly to the MySQL database. They use Spring Data JPA, meaning you don't write raw SQL queries. Instead, you write method names like `findByUserAndIsDeletedFalse()` and Spring writes the SQL for you automatically.
*   **Entities (`@Entity`):** These are Java classes that exactly match your database tables (`User`, `FileEntity`, `Folder`).

---

## 2. Feature Deep-Dive: How Does it Actually Work?

### A. Data Deduplication (Saving Hard Drive Space)
**What it is:** If two users upload the exact same 50MB PDF, the server shouldn't waste 100MB of space. It should only save it once (50MB).
**How your code does it (`FileStorageService.uploadFile`):**
1. When a file is uploaded, the system reads the raw bytes and calculates a **SHA-256 Hash** (a unique digital fingerprint).
2. It queries the database: *"Does any file already exist with this hash?"*
3. **If NO:** It saves the physical file to the hard drive and creates a new database record.
4. **If YES:** It **skips** saving the file to the hard drive! It just creates a new database record pointing to the already existing physical file. 

### B. Distributed Node Simulation
**What it is:** Enterprise systems (like AWS S3) don't store files on one computer. They distribute them across many servers based on size or load.
**How your code does it (`StorageNodeService.java`):**
1. Before saving a file, the `StorageNodeService` looks at the file size.
2. If it's a small file (< 1MB), it assigns it to `NODE-ALPHA`. If it's a large file, it assigns it to `NODE-BETA`. 
3. In your code, these "Nodes" are just different root folders on your local hard drive (e.g., `C:/CloudNestStorage/node-alpha/`), but the architectural concept is exactly how distributed systems work.

### C. On-The-Fly ZIP Downloads
**What it is:** When a user clicks "Download Folder", they get a `.zip` file containing all files and nested sub-folders.
**How your code does it (`FolderService.downloadFolderAsZip`):**
1. It does **not** create a temporary zip file on the hard drive (which would waste space and be slow).
2. Instead, it opens a `ZipOutputStream` directly connected to the user's browser download stream.
3. It uses a **Recursive Function** (`zipFolder`). It looks at the folder, streams its files into the Zip, then looks at its sub-folders, calls itself again, and streams those files, maintaining the exact folder structure in memory.

### D. Soft Delete & The Recycle Bin
**What it is:** When a user clicks delete, the file isn't immediately destroyed.
**How your code does it:**
1. Your database tables (`files` and `folders`) have a boolean column called `is_deleted`.
2. When a user clicks delete, the Service layer just changes `is_deleted = true`.
3. All your Repositories have their queries strictly defined to only fetch items where `isDeleted = false` (e.g., `findByUserAndIsDeletedFalse()`). Because of this, the deleted files instantly disappear from the main UI.
4. The **TrashController** runs queries explicitly looking for `isDeleted = true` to populate the Recycle Bin.
5. If the user clicks **Permanent Delete**, the system finally deletes the database record. *Crucially*, because of deduplication, it checks: *"Are any other users referencing this physical file?"* If no, it deletes the file from the hard drive. If yes, it keeps the physical file but deletes the user's database record.

### E. Storage Quotas
**What it is:** Each user is restricted to 1GB of total space.
**How your code does it:**
1. When an upload starts, `FileStorageService.calculateTotalStorageUsed(user)` asks the database to sum up the `size` of all files owned by the user.
2. If `Total Used + New File Size > 1,073,741,824 bytes (1GB)`, it throws an Exception and blocks the upload.

---

## 3. Frontend Mechanics (UI/UX)
*   **Thymeleaf:** Your HTML files use `th:text` and `th:each`. When the Controller passes a `List<FileDto>` to the view, Thymeleaf loops through the list on the server and generates standard HTML before sending it to the browser.
*   **Glassmorphism:** Achieved in `style.css` using `background: rgba(...)` combined with `backdrop-filter: blur(10px)`. This tells the browser to blur whatever is behind the element.
*   **Theme Toggle:** When you click the moon/sun icon, Javascript changes an attribute on the `<html>` tag (`data-theme="light"`). `style.css` has a special block `[data-theme="light"]` that instantly overrides all the dark color variables with light color variables.

---

## 4. Possible Questions & Answers for Viva

**Q: Why didn't you store files inside the MySQL database as BLOBs?**
**A:** Storing large binary files in a relational database bloats the database, drastically slowing down standard queries and making backups enormous. It is best practice to store physical files on a File System (or Object Storage like AWS S3) and only store the file path and metadata in the database.

**Q: How did you handle security?**
**A:** I used Spring Security. Passwords are never stored as plain text; they are hashed using the BCrypt hashing algorithm. Furthermore, every endpoint checks the `Principal` (the currently logged-in user session) to ensure User A can never fetch or manipulate an ID belonging to User B.

**Q: What happens if a file upload fails halfway?**
**A:** The `FileStorageService` methods are annotated with `@Transactional`. This means if the file writes to the disk but the database crashes, the entire transaction is rolled back, preventing orphaned data.
