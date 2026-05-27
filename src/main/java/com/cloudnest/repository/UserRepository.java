package com.cloudnest.repository;

import com.cloudnest.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * UserRepository — Data access interface for User entities.
 *
 * WHY THIS EXISTS:
 * - Spring Data JPA generates the implementation automatically at runtime
 * - We just declare method signatures, and Spring creates the SQL queries
 * - JpaRepository provides: save(), findById(), findAll(), delete(), etc.
 *
 * HOW IT WORKS:
 * - Spring reads the method name and generates a query:
 *   findByUsername("john") → SELECT * FROM users WHERE username = 'john'
 * - This is called "Query Derivation" — one of Spring Data's most powerful features
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Find by username OR email (case-insensitive for better UX)
    Optional<User> findByUsernameIgnoreCaseOrEmailIgnoreCase(String username, String email);

    // Legacy method for existing references
    Optional<User> findByUsername(String username);

    // SELECT * FROM users WHERE email = ?
    Optional<User> findByEmail(String email);

    // Returns true if a user with this username already exists
    boolean existsByUsername(String username);

    // Returns true if a user with this email already exists
    boolean existsByEmail(String email);
}
