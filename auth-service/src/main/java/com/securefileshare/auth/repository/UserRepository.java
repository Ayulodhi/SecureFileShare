package com.securefileshare.auth.repository;

import com.securefileshare.auth.entity.Role;
import com.securefileshare.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the User entity.
 * WHAT: Provides CRUD operations and custom query methods for USERS table.
 * WHY: Enables authentication lookups by username or email.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /**
     * Finds a user by their unique username.
     *
     * @param username Target username
     * @return Optional containing the User entity if located
     */
    Optional<User> findByUsername(String username);

    /**
     * Checks if a user already exists with the given username.
     *
     * @param username Target username
     * @return True if existing user exists
     */
    boolean existsByUsername(String username);

    /**
     * Checks if a user already exists with the given email address.
     *
     * @param email Target email address
     * @return True if existing user exists
     */
    boolean existsByEmail(String email);

    /**
     * Checks if any user exists holding the specified role (used for admin seeding checks).
     *
     * @param role Target role
     * @return True if at least one user with the role exists
     */
    boolean existsByRole(Role role);
}
