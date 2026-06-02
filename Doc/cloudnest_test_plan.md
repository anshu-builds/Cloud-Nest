# ☁️ CloudNest — Exhaustive Test Plan (Basic → Advanced)

> **Goal**: Verify that every single feature of this project works **flawlessly and beautifully**. This plan is ruthless — it covers happy paths, error paths, edge cases, security, performance, and UI polish. Nothing is skipped.

---

## 🔧 Section 0 — Prerequisites & Environment Setup

Before testing anything, make sure these are in place:

| # | Check | How to Verify | Expected Result |
|---|-------|---------------|-----------------|
| 0.1 | **PostgreSQL is running** | Open pgAdmin or run `psql -U postgres -c "SELECT 1"` | Returns `1` — database server is alive |
| 0.2 | **`cloudnest_db` database exists** | `psql -U postgres -c "\l"` or check pgAdmin | `cloudnest_db` appears in the list |
| 0.3 | **`DB_PASSWORD` environment variable is set** | `echo $env:DB_PASSWORD` in PowerShell | Shows your PostgreSQL password (not blank) |
| 0.4 | **Application starts without errors** | Run `$env:DB_PASSWORD="yourpass"; ./mvnw spring-boot:run` | Console shows `Started CloudNestApplication` with no stack traces |
| 0.5 | **Port 8080 is accessible** | Open `http://localhost:8080` in browser | Redirects to `/login` page |
| 0.6 | **Storage directories exist** | Check `storage/node1`, `storage/node2`, `storage/node3` in project root | All 3 directories exist (auto-created by `AppConfig`) |
| 0.7 | **Health endpoint works** | Open `http://localhost:8080/actuator/health` | Returns `{"status":"UP"}` |

---

## 🔐 Section 1 — User Registration (7 Tests)

### 1.1 — Show Registration Page
- **Steps**: Navigate to `http://localhost:8080/register`
- **Expected**: Registration form renders with Username, Email, Password, Confirm Password fields. Dark glassmorphism theme with animated background shapes is visible. Google Font "Inter" is loaded.

### 1.2 — Successful Registration
- **Steps**: Fill in Username: `testuser1`, Email: `test1@cloudnest.com`, Password: `password123`, Confirm Password: `password123` → Click **Register**
- **Expected**: Redirects to `/login` with green success toast: "Registration successful! Please log in."

### 1.3 — Registration with Mismatched Passwords
- **Steps**: Fill in Username: `testuser2`, Email: `test2@cloudnest.com`, Password: `password123`, Confirm Password: `password456` → Click **Register**
- **Expected**: Stays on `/register` with error message: "Passwords do not match"

### 1.4 — Registration with Duplicate Username
- **Steps**: Register `testuser1` again with a different email
- **Expected**: Stays on `/register` with error: "Username 'testuser1' is already taken"

### 1.5 — Registration with Duplicate Email
- **Steps**: Register a different username with email `test1@cloudnest.com`
- **Expected**: Stays on `/register` with error: "Email 'test1@cloudnest.com' is already registered"

### 1.6 — Registration with Invalid Email Format
- **Steps**: Fill in Email: `not-an-email` → Submit
- **Expected**: Stays on `/register` with validation error: "Please provide a valid email address"

### 1.7 — Registration with Short Username/Password
- **Steps**: Fill in Username: `ab` (2 chars), Password: `123` (3 chars) → Submit
- **Expected**: Validation errors for both fields (min 3 for username, min 6 for password)

---

## 🔑 Section 2 — Login & Logout (8 Tests)

### 2.1 — Show Login Page
- **Steps**: Navigate to `http://localhost:8080/login`
- **Expected**: Login form renders with Username/Email and Password fields. Animated background shapes, glassmorphism card visible.

### 2.2 — Successful Login with Username
- **Steps**: Enter Username: `testuser1`, Password: `password123` → Click **Login**
- **Expected**: Redirects to `/dashboard`. Username appears in the navbar/sidebar.

### 2.3 — Successful Login with Email
- **Steps**: Enter `test1@cloudnest.com` in the username field, Password: `password123` → Login
- **Expected**: Logs in successfully (case-insensitive email/username matching)

### 2.4 — Login with Wrong Password
- **Steps**: Enter Username: `testuser1`, Password: `wrongpassword` → Login
- **Expected**: Stays on `/login` with error: "Invalid username or password" (or similar Spring Security error)

### 2.5 — Login with Non-Existent User
- **Steps**: Enter Username: `nonexistent`, Password: `anything` → Login
- **Expected**: Stays on `/login` with error message. Does not reveal whether the username exists.

### 2.6 — Case-Insensitive Login
- **Steps**: Login with `TESTUSER1` (uppercase) or `Test1@cloudnest.COM`
- **Expected**: Login succeeds — matching is case-insensitive

### 2.7 — Logout
- **Steps**: While logged in, click **Logout** in the navigation
- **Expected**: Session is destroyed. Redirects to `/login?logout=true` with logout success message. Clicking browser Back button does NOT show dashboard (session invalidated).

### 2.8 — Access Protected Page Without Login
- **Steps**: Open a new incognito window. Navigate to `http://localhost:8080/dashboard`
- **Expected**: Redirects to `/login` (Spring Security intercepts the request)

---

## 📊 Section 3 — Dashboard (9 Tests)

### 3.1 — Dashboard Loads Successfully
- **Steps**: Log in → go to `/dashboard`
- **Expected**: Dashboard renders with stats cards, charts, and recent files section. No errors.

### 3.2 — Stats Cards Show Correct Data (Fresh User)
- **Steps**: Register a brand new user → Log in → Go to Dashboard
- **Expected**: Total Files: `0`, Storage Used: `0 B`, Total Folders: `0`, Quota bar at 0%

### 3.3 — Stats Update After Upload
- **Steps**: Upload a 1KB text file → Return to Dashboard
- **Expected**: Total Files: `1`, Storage Used: `~1 KB`, Quota bar shows a tiny percentage. Recent files section shows the uploaded file.

### 3.4 — Node Distribution Chart
- **Steps**: Upload 10+ files → Check Dashboard
- **Expected**: Node distribution chart shows files distributed across node1, node2, node3 (random distribution)

### 3.5 — File Type Distribution Chart
- **Steps**: Upload a `.pdf`, a `.jpg`, a `.txt`, and a `.zip` → Dashboard
- **Expected**: File type distribution chart shows "PDF", "Images", "Text", "Archives" categories

### 3.6 — Recent Files Shows Last 5
- **Steps**: Upload 7 files → Dashboard
- **Expected**: Recent files section shows only the 5 most recent uploads, ordered newest-first

### 3.7 — Quota Percentage Calculation
- **Steps**: Upload files totaling ~100MB → Dashboard
- **Expected**: Quota bar shows ~10% (quota is 1GB). Formatted storage shows correct "100 MB / 1 GB"

### 3.8 — Dashboard Does NOT Count Deleted Files
- **Steps**: Upload 5 files → Delete 3 → Dashboard
- **Expected**: Total Files shows `2` (only active files). Storage used reflects only the 2 active files.

### 3.9 — Sidebar Navigation Active State
- **Steps**: Navigate to Dashboard via sidebar
- **Expected**: "Dashboard" menu item is highlighted/active in the sidebar

---

## 📤 Section 4 — File Upload (12 Tests)

### 4.1 — Upload Single File via Button
- **Steps**: Go to `/files` → Click upload button → Select a `.txt` file → Submit
- **Expected**: File appears in the file list with correct name, size, type, and upload date. Success toast: "1 file(s) uploaded successfully!"

### 4.2 — Upload Multiple Files at Once
- **Steps**: Select 3 files at once in the upload dialog → Submit
- **Expected**: All 3 files appear. Success toast: "3 file(s) uploaded successfully!"

### 4.3 — Upload via Drag & Drop
- **Steps**: Drag a file from Windows Explorer onto the drop zone on the `/files` page
- **Expected**: File uploads successfully. Visual drag-over indicator appears while dragging.

### 4.4 — Upload Empty File
- **Steps**: Create a 0-byte file → Upload it
- **Expected**: Error toast: "Cannot upload an empty file"

### 4.5 — Upload into a Folder
- **Steps**: Navigate into a folder → Upload a file
- **Expected**: File appears inside that folder (not at root level). `folderId` is preserved in redirect.

### 4.6 — Upload Blocked Extension (.exe)
- **Steps**: Try to upload `malware.exe`
- **Expected**: Error toast: "File type not allowed for security reasons: .exe". File is NOT saved.

### 4.7 — Upload Blocked Extension (.bat, .sh, .ps1, .cmd)
- **Steps**: Try to upload files with each blocked extension
- **Expected**: Each one is rejected with the same security error

### 4.8 — Upload File Exceeding 50MB Limit
- **Steps**: Create a file larger than 50MB → Upload
- **Expected**: Spring Boot rejects it (configured `spring.servlet.multipart.max-file-size=50MB`). Error message shown.

### 4.9 — Upload Exceeding 1GB Quota
- **Steps**: Upload files totaling close to 1GB → Upload one more file that exceeds the quota
- **Expected**: Error: "Storage quota exceeded. You have reached your 1GB limit."

### 4.10 — Data Deduplication (Same Content, Different Name)
- **Steps**: Upload `file_a.txt` with content "Hello World" → Upload `file_b.txt` with identical content "Hello World"
- **Expected**: Both files appear in the file list with different names. On disk, only ONE physical file exists (check `storage/node*/` — both DB records point to the same `storedName`). Quota only counts the bytes once.

### 4.11 — Upload Various File Types
- **Steps**: Upload one of each: `.pdf`, `.jpg`, `.png`, `.mp4`, `.docx`, `.xlsx`, `.zip`, `.txt`, `.csv`
- **Expected**: All upload successfully. File type icons/labels display correctly in the file list.

### 4.12 — Upload File with Special Characters in Name
- **Steps**: Upload a file named `my report (final) [v2].txt`
- **Expected**: File uploads and displays with original name preserved. Download works correctly.

---

## 📁 Section 5 — File Management (10 Tests)

### 5.1 — File Listing at Root
- **Steps**: Go to `/files` (no folderId)
- **Expected**: Shows only root-level files and folders. No breadcrumbs visible.

### 5.2 — Download a File
- **Steps**: Click the download button/icon on any file
- **Expected**: Browser downloads the file. Filename matches the original upload name. Content is identical to the original.

### 5.3 — Preview a File (Image)
- **Steps**: Upload a `.jpg` or `.png` → Click the preview button
- **Expected**: Image opens inline in the browser (Content-Disposition: inline). Correct MIME type header.

### 5.4 — Preview a File (PDF)
- **Steps**: Upload a `.pdf` → Click preview
- **Expected**: PDF renders in-browser via the browser's built-in viewer

### 5.5 — Soft-Delete a File
- **Steps**: Click delete on a file
- **Expected**: File disappears from the active file list. Success toast: "File deleted successfully!" File appears in Trash.

### 5.6 — Move a File to a Folder
- **Steps**: Create folder "Documents" → On a root file, click "Move" → Select "Documents" → Confirm
- **Expected**: File disappears from root. Navigating into "Documents" shows the file.

### 5.7 — Move a File to Root
- **Steps**: Navigate into a folder with files → Move a file to "Root" (no folder)
- **Expected**: File appears at root level

### 5.8 — Move Dropdown Shows ALL Folders (Not Just Root)
- **Steps**: Create folders: "A" → inside "A" create "B" → inside "B" create "C". Go to root file → click "Move"
- **Expected**: Dropdown shows A, B, and C (all 3 folders, not just root folder A)

### 5.9 — Download Soft-Deleted File via Direct URL
- **Steps**: Note a file's ID → Soft-delete it → Try `http://localhost:8080/files/download/{id}` directly
- **Expected**: Returns "File not found" error (NOT the file). Soft-deleted files must NOT be downloadable.

### 5.10 — Preview Soft-Deleted File via Direct URL
- **Steps**: Same as above but with `/files/preview/{id}`
- **Expected**: Returns "File not found" error. Soft-deleted files must NOT be previewable.

---

## 📂 Section 6 — Folder Management (14 Tests)

### 6.1 — Create Root Folder
- **Steps**: On `/files` root, click "Create Folder" → Enter name "Projects" → Submit
- **Expected**: Folder "Projects" appears in the folder grid. Success toast.

### 6.2 — Create Nested Folder
- **Steps**: Navigate into "Projects" → Create folder "Spring Boot"
- **Expected**: "Spring Boot" appears inside "Projects". Breadcrumbs show: Home > Projects

### 6.3 — Create Deeply Nested Folders (3+ Levels)
- **Steps**: Create: Level1 → Level2 → Level3 → Level4
- **Expected**: Breadcrumbs show full path: Home > Level1 > Level2 > Level3 > Level4

### 6.4 — Duplicate Folder Name (Same Level)
- **Steps**: Create "Documents" at root → Try creating "Documents" again at root
- **Expected**: Error: "A folder named 'Documents' already exists here"

### 6.5 — Duplicate Folder Name (Different Level)
- **Steps**: Create "Documents" at root → Navigate into "Projects" → Create "Documents" inside
- **Expected**: Succeeds — same name is allowed in different parent folders

### 6.6 — Folder Name with Invalid Characters
- **Steps**: Try creating a folder named `../etc/passwd`, or `folder\test`, or containing null bytes
- **Expected**: Error: "Folder name contains invalid characters"

### 6.7 — Folder Name Too Long (>255 chars)
- **Steps**: Enter a name with 256+ characters
- **Expected**: Error: "Folder name contains invalid characters"

### 6.8 — Empty Folder Name
- **Steps**: Submit the create folder form with an empty name
- **Expected**: Error: "Folder name cannot be empty"

### 6.9 — Soft-Delete a Folder
- **Steps**: Create a folder with 2 files inside → Delete the folder
- **Expected**: Folder disappears from file listing. Both files inside also disappear (cascade soft-delete). All appear in Trash.

### 6.10 — Download Folder as ZIP
- **Steps**: Create folder "Downloads" with 3 files → Click download folder button
- **Expected**: Browser downloads `Downloads.zip`. Extract it → all 3 files are present with correct content.

### 6.11 — ZIP Download Excludes Soft-Deleted Items
- **Steps**: In folder "A" put 3 files → soft-delete 1 file → download folder as ZIP
- **Expected**: ZIP contains only 2 files. The deleted file is NOT in the archive.

### 6.12 — Move Folder
- **Steps**: Create folder "Source" and folder "Destination" → Move "Source" into "Destination"
- **Expected**: "Source" disappears from root. Navigating into "Destination" shows "Source" as a subfolder.

### 6.13 — Move Folder into Itself (Blocked)
- **Steps**: Try moving folder "A" into folder "A"
- **Expected**: Error: "Cannot move a folder into itself"

### 6.14 — Move Folder into Own Descendant (Cycle Prevention)
- **Steps**: Create A → B inside A → C inside B → Try moving A into C
- **Expected**: Error: "Cannot move a folder into its own descendant"

---

## 🔍 Section 7 — Search (5 Tests)

### 7.1 — Search by File Name
- **Steps**: Upload "quarterly_report.pdf" → Search for "quarterly"
- **Expected**: "quarterly_report.pdf" appears in results

### 7.2 — Search by File Type
- **Steps**: Upload multiple files → Search for "pdf"
- **Expected**: All PDF files appear in results

### 7.3 — Case-Insensitive Search
- **Steps**: Upload "MyDocument.PDF" → Search for "mydocument"
- **Expected**: File is found (search is case-insensitive)

### 7.4 — Search with No Results
- **Steps**: Search for "zzzznonexistent"
- **Expected**: Empty results page. No errors. UI indicates no files found.

### 7.5 — Search Does Not Return Deleted Files
- **Steps**: Upload "secret.txt" → Delete it → Search for "secret"
- **Expected**: No results (soft-deleted files are excluded from search)

---

## 🔗 Section 8 — File Sharing (9 Tests)

### 8.1 — Generate Share Link
- **Steps**: On a file, click "Share" button
- **Expected**: Success toast: "Share link generated! It will expire in 7 days." A link `/share/{token}` is displayed.

### 8.2 — View Shared File Page (Unauthenticated)
- **Steps**: Copy the share link → Open in incognito/private window (NOT logged in)
- **Expected**: Shows file info page: filename, file size, file type, uploaded date, shared by username, expiration date. Download button visible. No login required.

### 8.3 — Download Shared File (Unauthenticated)
- **Steps**: On the shared file page → Click "Download"
- **Expected**: File downloads correctly. Content matches original.

### 8.4 — Expired Share Link
- **Steps**: Manually update the `expires_at` column in the `shared_links` table to a past date → Try accessing the share link
- **Expected**: Error: "This share link has expired"

### 8.5 — Invalid/Fake Share Token
- **Steps**: Navigate to `http://localhost:8080/share/fake-uuid-12345`
- **Expected**: Error: "Share link not found or invalid"

### 8.6 — Share Link for Soft-Deleted File
- **Steps**: Generate a share link → Soft-delete the file → Access the share link
- **Expected**: Error: "This shared file has been deleted by its owner."

### 8.7 — Download Shared File That Was Soft-Deleted
- **Steps**: Same setup → Try `http://localhost:8080/share/download/{token}`
- **Expected**: Error: "This shared file has been deleted."

### 8.8 — Multiple Share Links for Same File
- **Steps**: Generate 2 different share links for the same file
- **Expected**: Both links work independently. Each has its own token and expiration.

### 8.9 — Share Links Cleaned Up on Permanent Delete
- **Steps**: Generate share link → Permanently delete the file from Trash → Check `shared_links` table
- **Expected**: The SharedLink DB record is also deleted (via `deleteLinksForFile` call)

---

## 🗑️ Section 9 — Trash (10 Tests)

### 9.1 — View Trash Page
- **Steps**: Navigate to `/trash`
- **Expected**: Shows all soft-deleted files and folders. Displays file name, size, type, and deletion date.

### 9.2 — Restore a File from Trash
- **Steps**: In Trash → Click "Restore" on a file
- **Expected**: File reappears in the original file listing. Disappears from Trash. Success toast.

### 9.3 — Restore a Folder from Trash (Cascade Restore)
- **Steps**: Delete a folder containing 3 files → Go to Trash → Restore the folder
- **Expected**: Folder AND all 3 files inside are restored. All disappear from Trash and reappear in file listing.

### 9.4 — Permanently Delete a File
- **Steps**: In Trash → Click "Permanent Delete" on a file
- **Expected**: File is removed from Trash AND from the database. Physical file is deleted from `storage/node*/` (if no deduplication references). Cannot be recovered.

### 9.5 — Permanently Delete a Folder
- **Steps**: In Trash → Click "Permanent Delete" on a folder
- **Expected**: Folder AND all its contents (files, subfolders) are permanently removed from DB and disk.

### 9.6 — Permanent Delete with Deduplication
- **Steps**: Upload identical file twice → Delete both → Permanently delete the first one
- **Expected**: Physical file still exists on disk (because the second copy still references it). Permanently delete the second one → Physical file is now deleted from disk.

### 9.7 — Empty Trash Shows Clean State
- **Steps**: Make sure no deleted items exist → Go to `/trash`
- **Expected**: Shows empty state UI (no items, possibly a "Your trash is empty" message)

### 9.8 — Trash Shows Both Files and Folders
- **Steps**: Delete 2 files and 1 folder → Go to Trash
- **Expected**: Both types appear separated (files section and folders section, or clearly labeled)

### 9.9 — Deleted Items NOT in Active File Count
- **Steps**: Upload 5 files → Delete 2 → Check Dashboard
- **Expected**: Dashboard shows Total Files: `3` (not 5)

### 9.10 — Auto-Purge Scheduler (30-Day Retention)
- **Steps**: In the DB, manually set a file's `deleted_at` to 31 days ago → Wait for the 2:00 AM cron job to fire (or invoke `TrashCleanupScheduler.purgeExpiredTrashItems()` manually via tests)
- **Expected**: File is permanently deleted from DB and disk automatically

---

## 👑 Section 10 — Admin Panel (9 Tests)

### 10.1 — Admin Dashboard Access (As Admin)
- **Steps**: Manually set a user's role to `ROLE_ADMIN` in DB → Log in → Navigate to `/admin/dashboard`
- **Expected**: Admin dashboard loads showing: Total Users, Total Files (active only), Total Storage, Node Distribution, User list, File list

### 10.2 — Admin Dashboard Blocked for Regular User
- **Steps**: Log in as `ROLE_USER` → Navigate to `/admin/dashboard`
- **Expected**: Returns **403 Forbidden**. Access denied.

### 10.3 — Admin Dashboard Stats Accuracy
- **Steps**: As admin, check stats
- **Expected**: Total Files counts only active (non-deleted) files. Storage reflects only active files. Node counts match actual distribution.

### 10.4 — Promote User to Admin
- **Steps**: As admin → Find a `ROLE_USER` in the user list → Click "Toggle Role"
- **Expected**: User's role changes to `ROLE_ADMIN`. Success toast. That user can now access `/admin/dashboard`.

### 10.5 — Demote Admin to User
- **Steps**: As admin → Find another `ROLE_ADMIN` → Click "Toggle Role"
- **Expected**: User's role changes to `ROLE_USER`. They can no longer access admin panel.

### 10.6 — Cannot Demote Yourself
- **Steps**: As admin → Try to toggle your own role
- **Expected**: Error: "You cannot demote your own administrator account." Role unchanged.

### 10.7 — Admin Delete Any File
- **Steps**: As admin → In the file list → Click delete on any user's file
- **Expected**: File is permanently deleted from DB and disk. No ownership check (admin override). Success toast.

### 10.8 — Toggle Role for Non-Existent User
- **Steps**: Manually craft `POST /admin/users/toggle-role/99999` (non-existent ID)
- **Expected**: Error: "User not found."

### 10.9 — New User ALWAYS Gets ROLE_USER
- **Steps**: Register a new user normally
- **Expected**: User's role in DB is `ROLE_USER` (never `ROLE_ADMIN` from registration)

---

## 🌐 Section 11 — Infrastructure Visualization Pages (7 Tests)

### 11.1 — Nodes Page
- **Steps**: Navigate to `/nodes`
- **Expected**: Shows 3 storage nodes with file count, storage used, and capacity percentage bars. Data is accurate.

### 11.2 — Deduplication Page
- **Steps**: Navigate to `/deduplication`
- **Expected**: Page loads with deduplication visualization. No errors.

### 11.3 — Replication Page
- **Steps**: Navigate to `/replication`
- **Expected**: Animated cross-node replication view loads. No errors.

### 11.4 — Network Page
- **Steps**: Navigate to `/network`
- **Expected**: Real-time network activity dashboard loads. No errors.

### 11.5 — Analytics Page
- **Steps**: Navigate to `/analytics`
- **Expected**: Enterprise storage analytics page loads. No errors.

### 11.6 — Monitoring Page
- **Steps**: Navigate to `/monitoring`
- **Expected**: Shows JVM memory health ring (real data), total files count, and deduplication savings.

### 11.7 — All Infrastructure Pages Require Login
- **Steps**: Try accessing `/nodes`, `/deduplication`, `/replication`, `/network`, `/analytics`, `/monitoring` in incognito
- **Expected**: All redirect to `/login`

---

## 🛡️ Section 12 — Security Tests (12 Tests)

### 12.1 — CSRF Protection
- **Steps**: Inspect any POST form in DevTools → Check for `_csrf` hidden input
- **Expected**: Every POST form includes a CSRF token. Submitting a request without CSRF token returns 403.

### 12.2 — IDOR — Access Another User's File
- **Steps**: Upload a file as User A (note the ID) → Log in as User B → Try `GET /files/download/{id}`
- **Expected**: "File not found" error. User B CANNOT access User A's files.

### 12.3 — IDOR — Delete Another User's File
- **Steps**: Log in as User B → Try `POST /files/delete/{userA_file_id}`
- **Expected**: "File not found" error. Deletion is blocked.

### 12.4 — IDOR — Move Another User's File
- **Steps**: Log in as User B → Try `POST /files/move/{userA_file_id}`
- **Expected**: "File not found" error

### 12.5 — IDOR — Access Another User's Folder
- **Steps**: Log in as User B → Try `GET /files?folderId={userA_folder_id}`
- **Expected**: "Folder not found" error

### 12.6 — IDOR — Delete Another User's Folder
- **Steps**: Log in as User B → Try `POST /folders/delete/{userA_folder_id}`
- **Expected**: "Folder not found" error

### 12.7 — IDOR — Generate Share Link for Another User's File
- **Steps**: Log in as User B → Try `POST /share/generate/{userA_file_id}`
- **Expected**: "File not found" error

### 12.8 — Password Stored as BCrypt Hash
- **Steps**: Check DB: `SELECT password FROM users WHERE username='testuser1'`
- **Expected**: Password starts with `$2a$12$...` (BCrypt with 12 rounds). NOT plaintext.

### 12.9 — Session Invalidation on Logout
- **Steps**: Log in → Copy the `JSESSIONID` cookie → Log out → Try to use the old session cookie
- **Expected**: Old session is invalid. Request redirects to login.

### 12.10 — Path Traversal in Folder Names
- **Steps**: Try creating folders named `../../../etc`, `..\\windows\\system32`, `\0malicious`
- **Expected**: All rejected: "Folder name contains invalid characters"

### 12.11 — Blocked Executable Upload Bypass Attempt (Case Variation)
- **Steps**: Try uploading `virus.EXE`, `script.Bat`, `hack.PS1`
- **Expected**: All blocked (extension check is case-insensitive via `.toLowerCase()`)

### 12.12 — Direct Admin URL Access Without Admin Role
- **Steps**: Log in as regular user → Try: `POST /admin/files/delete/1`, `POST /admin/users/toggle-role/1`
- **Expected**: Returns **403 Forbidden** for all admin endpoints

---

## ⚡ Section 13 — Performance & Stress Tests (6 Tests)

### 13.1 — Bulk Upload (20+ Files)
- **Steps**: Select 20 small text files → Upload all at once
- **Expected**: All 20 upload successfully. Page responds within reasonable time (<10 seconds). Success toast shows "20 file(s) uploaded successfully!"

### 13.2 — Large File Upload (49MB)
- **Steps**: Upload a single 49MB file (just under the 50MB limit)
- **Expected**: Upload completes. File is accessible for download. No timeout.

### 13.3 — Dashboard with Many Files
- **Steps**: Upload 50+ files → Navigate to Dashboard
- **Expected**: Page loads quickly (<3 seconds). Stats are accurate. No `OutOfMemoryError` (aggregation queries prevent this).

### 13.4 — Admin Dashboard with Many Users/Files
- **Steps**: Create 10+ users each with 20+ files → Load `/admin/dashboard`
- **Expected**: Page loads without lag. Stats use aggregation queries, not `findAll()` streams.

### 13.5 — Deduplication at Scale
- **Steps**: Upload the same 5MB file 10 times with different names
- **Expected**: Only ~5MB of disk space consumed (not 50MB). All 10 files downloadable independently.

### 13.6 — Deep Folder Nesting
- **Steps**: Create 10 levels of nested folders: A > B > C > D > E > F > G > H > I > J
- **Expected**: Navigation works. Breadcrumbs render correctly for all 10 levels. No stack overflow.

---

## 🎨 Section 14 — UI/UX Visual Polish (12 Tests)

### 14.1 — Dark Glassmorphism Theme
- **Steps**: Inspect all pages
- **Expected**: Consistent dark theme with glass-blur effects, gradient accents, smooth transitions. No jarring bright-white elements.

### 14.2 — Typography (Google Fonts)
- **Steps**: Inspect body font in DevTools
- **Expected**: Font-family includes `Inter` (or `Roboto`). NOT browser defaults like Times New Roman.

### 14.3 — Responsive Layout — Mobile
- **Steps**: Open DevTools → Toggle device toolbar → Select iPhone 12 (390px width)
- **Expected**: All pages render correctly. Sidebar collapses into a hamburger menu. File list becomes scrollable. No horizontal overflow.

### 14.4 — Responsive Layout — Tablet
- **Steps**: Set viewport to 768px width
- **Expected**: Layout adapts. Cards stack as needed. Everything remains readable.

### 14.5 — Hover Effects & Micro-Animations
- **Steps**: Hover over file cards, buttons, sidebar items
- **Expected**: Smooth hover transitions (opacity, scale, shadow changes). Interactive elements feel alive.

### 14.6 — Toast/Alert Auto-Dismiss
- **Steps**: Perform an action that triggers a success/error toast
- **Expected**: Toast appears with smooth animation → Auto-dismisses after 3-5 seconds

### 14.7 — Loading States
- **Steps**: Upload a large file or navigate to a page
- **Expected**: No blank/white flash during page loads. Content appears smoothly.

### 14.8 — File Size Formatting
- **Steps**: Upload files of various sizes → Check display
- **Expected**: Sizes shown as: `1.5 KB`, `3.2 MB`, `1.1 GB` (not raw bytes like `1536`)

### 14.9 — Lucide Icons Render
- **Steps**: Check all pages for icons
- **Expected**: Icons (file, folder, download, trash, share, etc.) render from Lucide CDN. No broken/missing icon boxes.

### 14.10 — Three.js WebGL Background
- **Steps**: Check login/register pages
- **Expected**: Animated 3D background renders smoothly. No WebGL errors in console.

### 14.11 — Form Validation Visual Feedback
- **Steps**: Submit registration with invalid fields
- **Expected**: Invalid fields are highlighted with red borders/error text. Valid fields remain normal.

### 14.12 — Empty States
- **Steps**: View file listing with no files, trash with no items, search with no results
- **Expected**: Each shows a friendly empty state UI (icon + message like "No files yet — upload your first file!"). NOT a blank white page.

---

## 🧪 Section 15 — Edge Cases & Boundary Tests (10 Tests)

### 15.1 — Upload File with No Extension
- **Steps**: Upload a file named `README` (no extension)
- **Expected**: Uploads successfully. Type shown as unknown/other. Download works.

### 15.2 — Upload File with Multiple Dots in Name
- **Steps**: Upload `my.backup.file.v2.2024.tar.gz`
- **Expected**: Uploads with full name preserved. Extension detection picks `.gz`.

### 15.3 — Folder with Same Name as File
- **Steps**: Upload a file called "Report" → Create a folder called "Report"
- **Expected**: Both coexist. Files and folders are separate entities.

### 15.4 — Delete a Folder Then Recreate with Same Name
- **Steps**: Create folder "Archive" → Delete it → Create "Archive" again
- **Expected**: New folder creates successfully. No duplicate name error (old one is soft-deleted).

### 15.5 — Concurrent Browser Tabs
- **Steps**: Open `/files` in 2 tabs → Upload a file in tab 1 → Refresh tab 2
- **Expected**: Tab 2 shows the newly uploaded file. No CSRF errors.

### 15.6 — Very Long Filename (200+ chars)
- **Steps**: Upload a file with a 200-character filename
- **Expected**: Uploads and displays correctly. Name may be truncated in UI but full name preserved in DB and download.

### 15.7 — Unicode/Emoji in Folder Names
- **Steps**: Create a folder named `📁 My Docs` or `日本語フォルダ`
- **Expected**: Folder creates and displays correctly. Breadcrumbs render the Unicode properly.

### 15.8 — Download Folder as ZIP with Nested Empty Folders
- **Steps**: Create folder A → inside A create folder B (empty) → Download A as ZIP
- **Expected**: ZIP contains folder B as an empty directory entry

### 15.9 — Restore Folder Then Delete Again
- **Steps**: Delete folder → Restore from Trash → Delete again → Check Trash
- **Expected**: Folder appears in Trash again. Each cycle works correctly.

### 15.10 — File with Null/Unknown MIME Type
- **Steps**: Upload a file with an unknown type (e.g., `.xyz` custom extension)
- **Expected**: Uploads. Preview falls back to `application/octet-stream`. Download still works.

---

## 🏥 Section 16 — Health & Operational Tests (4 Tests)

### 16.1 — Actuator Health Endpoint
- **Steps**: `GET http://localhost:8080/actuator/health`
- **Expected**: Returns `{"status":"UP"}` with HTTP 200

### 16.2 — Actuator Info Endpoint
- **Steps**: `GET http://localhost:8080/actuator/info`
- **Expected**: Returns HTTP 200 (may be empty object `{}` if no info configured)

### 16.3 — Actuator Endpoints NOT Accessible for Sensitive Data
- **Steps**: Try `GET /actuator/env`, `GET /actuator/beans`, `GET /actuator/mappings`
- **Expected**: Either 404 or 401/403 (only `/health` and `/info` are exposed and public)

### 16.4 — Graceful Error Handling
- **Steps**: Navigate to `http://localhost:8080/nonexistent-page`
- **Expected**: Custom error page or redirect — NOT a raw Spring Boot stack trace (Whitelabel Error Page)

---

## ✅ Summary — Total Test Count

| Section | Tests |
|---------|-------|
| 0. Prerequisites | 7 |
| 1. Registration | 7 |
| 2. Login & Logout | 8 |
| 3. Dashboard | 9 |
| 4. File Upload | 12 |
| 5. File Management | 10 |
| 6. Folder Management | 14 |
| 7. Search | 5 |
| 8. File Sharing | 9 |
| 9. Trash | 10 |
| 10. Admin Panel | 9 |
| 11. Infrastructure Pages | 7 |
| 12. Security | 12 |
| 13. Performance & Stress | 6 |
| 14. UI/UX Polish | 12 |
| 15. Edge Cases | 10 |
| 16. Health & Operational | 4 |
| **TOTAL** | **151** |

> After passing all 151 tests, this project is **certified 100% production-ready, bug-free, secure, and beautiful**.
