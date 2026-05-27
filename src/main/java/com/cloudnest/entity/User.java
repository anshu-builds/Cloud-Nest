package com.cloudnest.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * User Entity — Represents a registered user in the system.
 *
 * WHY THIS CLASS EXISTS:
 * - Maps to the "users" table in PostgreSQL
 * - Spring Security uses this to authenticate and authorize users
 * - Each user owns files and folders
 *
 * LOMBOK ANNOTATIONS:
 * - @Getter / @Setter: Auto-generates getter/setter methods
 * - @NoArgsConstructor: Creates a no-argument constructor (required by JPA)
 * - @AllArgsConstructor: Creates a constructor with all fields
 * - @Builder: Enables the Builder pattern for clean object construction
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increment in PostgreSQL
    private Long id;

    @jakarta.persistence.Version
    private Long version;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    // Password is stored as a BCrypt hash (never plain text!)
    @Column(nullable = false, length = 255)
    private String password;

    // Role (ROLE_USER, ROLE_ADMIN)
    @Column(name = "role", nullable = false, length = 20, columnDefinition = "varchar(20) default 'ROLE_USER'")
    @Builder.Default
    private String role = "ROLE_USER";

    @CreationTimestamp // Hibernate automatically sets this when the row is created
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // One user can have many files (one-to-many relationship)
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Builder.Default
    private List<FileEntity> files = new ArrayList<>();

    // One user can have many folders
    @OneToMany(mappedBy = "user", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @Builder.Default
    private List<Folder> folders = new ArrayList<>();
}
