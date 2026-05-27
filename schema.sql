-- =========================================================================
-- CloudNest — PostgreSQL Database Schema
-- =========================================================================
-- This schema matches the JPA entities and is the reference for manual setup.
-- If using spring.jpa.hibernate.ddl-auto=update, Hibernate will create/update
-- tables automatically. This script is for reference or CI/CD migrations.
--
-- BUG-02 FIX: Rewritten from MySQL syntax to PostgreSQL.
-- Added missing columns: role, file_hash, is_deleted, deleted_at.
-- Added performance indexes on foreign keys and frequently queried columns.
-- =========================================================================

-- Step 1: Users table
CREATE TABLE IF NOT EXISTS users (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    email       VARCHAR(100) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    role        VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
    version     BIGINT       DEFAULT 0,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

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

-- Step 3: Files table
CREATE TABLE IF NOT EXISTS files (
    id            BIGSERIAL PRIMARY KEY,
    original_name VARCHAR(255) NOT NULL,
    stored_name   VARCHAR(255) NOT NULL,
    file_type     VARCHAR(100),
    file_size     BIGINT,
    storage_node  VARCHAR(20),
    file_hash     VARCHAR(64),
    is_deleted    BOOLEAN DEFAULT FALSE,
    deleted_at    TIMESTAMP,
    version       BIGINT  DEFAULT 0,
    user_id       BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    folder_id     BIGINT REFERENCES folders(id) ON DELETE SET NULL,
    uploaded_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Step 4: Shared links table
CREATE TABLE IF NOT EXISTS shared_links (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255) NOT NULL UNIQUE,
    file_id     BIGINT NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    created_by  BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    expires_at  TIMESTAMP,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================================
-- Performance Indexes
-- =========================================================================
CREATE INDEX IF NOT EXISTS idx_files_user_id     ON files(user_id);
CREATE INDEX IF NOT EXISTS idx_files_folder_id   ON files(folder_id);
CREATE INDEX IF NOT EXISTS idx_files_file_hash   ON files(file_hash);
CREATE INDEX IF NOT EXISTS idx_files_is_deleted  ON files(is_deleted);
CREATE INDEX IF NOT EXISTS idx_folders_user_id   ON folders(user_id);
CREATE INDEX IF NOT EXISTS idx_folders_parent_id ON folders(parent_id);
CREATE INDEX IF NOT EXISTS idx_shared_links_file_id ON shared_links(file_id);
CREATE INDEX IF NOT EXISTS idx_shared_links_token   ON shared_links(token);
