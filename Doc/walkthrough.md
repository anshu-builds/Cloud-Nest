# ☁️ CloudNest — Complete Project Walkthrough

## 📁 Complete File Structure (30 files)

### Backend — Java (22 files)
| File | Purpose |
|------|---------|
| `CloudNestApplication.java` | Main entry point — bootstraps Spring Boot |
| **Config** | |
| `config/AppConfig.java` | Creates storage/node1-3 directories on startup |
| **Security** | |
| `security/SecurityConfig.java` | Session-based auth, BCrypt, login/logout rules |
| **Entities (JPA → PostgreSQL)** | |
| `entity/User.java` | Users table — username, email, hashed password |
| `entity/FileEntity.java` | Files table — name, size, type, storage node |
| `entity/Folder.java` | Folders table — self-referencing hierarchy |
| `entity/SharedLink.java` | Share links — UUID token with expiration |
| **DTOs** | |
| `dto/UserRegistrationDto.java` | Registration form data + validation |
| `dto/FileDto.java` | File display data + formatted size helper |
| `dto/FolderDto.java` | Folder display data |
| `dto/DashboardDto.java` | Dashboard statistics aggregation |
| **Repositories** | |
| `repository/UserRepository.java` | User lookup by username/email |
| `repository/FileRepository.java` | File search, stats, node distribution queries |
| `repository/FolderRepository.java` | Folder hierarchy queries |
| `repository/SharedLinkRepository.java` | Token-based share link resolution |
| **Services** | |
| `service/UserService.java` | Registration + Spring Security auth |
| `service/FileStorageService.java` | Upload/download/delete with node simulation |
| `service/FolderService.java` | Folder CRUD + breadcrumb navigation |
| `service/SharedLinkService.java` | Generate/resolve share links (7-day expiry) |
| `service/StorageNodeService.java` | Random node selection (distributed storage sim) |
| **Exceptions** | |
| `exception/FileNotFoundException.java` | Custom 404 for files |
| `exception/StorageException.java` | Custom I/O error wrapper |
| `exception/GlobalExceptionHandler.java` | Catches all exceptions → user-friendly messages |

### Controllers (5 files)
| Controller | Endpoints |
|-----------|-----------|
| `AuthController` | `GET/POST /login`, `GET/POST /register`, `GET /` |
| `DashboardController` | `GET /dashboard` |
| `FileController` | `GET /files`, `POST /files/upload`, `GET /files/download/{id}`, `POST /files/delete/{id}`, `GET /files/search` |
| `FolderController` | `POST /folders/create`, `POST /folders/delete/{id}` |
| `ShareController` | `POST /share/generate/{fileId}`, `GET /share/{token}`, `GET /share/download/{token}` |

### Frontend (8 files)
| File | Description |
|------|-------------|
| `templates/fragments/header.html` | Navbar with search + user dropdown |
| `templates/fragments/footer.html` | Footer + Bootstrap JS |
| `templates/login.html` | Login with animated bg shapes |
| `templates/register.html` | Registration with field validation |
| `templates/dashboard.html` | Stats cards, node chart, recent files |
| `templates/files.html` | File table, folder grid, upload/create modals |
| `templates/shared.html` | Public shared file download page |
| `static/css/style.css` | Premium dark glassmorphism theme |
| `static/js/app.js` | Password toggle, drag-drop, clipboard copy |

---

## 🚀 How to Run

### 1. Database Setup
```sql
CREATE DATABASE cloudnest_db;
```
Update `application.properties` if your PostgreSQL password differs from `root`.

### 2. Run in IntelliJ IDEA
1. **File → Open** → select project folder
2. Wait for Maven to download dependencies
3. Enable **Lombok plugin** (Settings → Plugins → Lombok)
4. Run `CloudNestApplication.java`
5. Open **http://localhost:8080**

### 3. First Use
1. Go to `/register` → create an account
2. Login → redirected to Dashboard
3. Upload files, create folders, share links!

---

## 🏗️ How Each Layer Works

```
Browser → Controller → Service → Repository → PostgreSQL
                ↓
           Thymeleaf
           Templates
```

1. **User clicks a button** → browser sends HTTP request
2. **Controller** receives request, calls the Service
3. **Service** contains business logic, calls Repository
4. **Repository** executes SQL via Spring Data JPA
5. **Controller** puts data into Model → Thymeleaf renders HTML
6. **Browser** displays the rendered page
