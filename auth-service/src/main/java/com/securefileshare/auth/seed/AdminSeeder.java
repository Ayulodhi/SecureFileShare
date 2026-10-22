package com.securefileshare.auth.seed;

import com.securefileshare.auth.entity.Role;
import com.securefileshare.auth.entity.User;
import com.securefileshare.auth.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Startup database seeder for initial platform administrator.
 * WHAT: Ensures exactly one initial administrator account exists in the database on startup.
 * WHY: Since the public registration endpoint prohibits self-registering the ADMIN role,
 *      this seeder initializes the initial admin account safely using environment variables.
 */
@Component
public class AdminSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final String initialUsername;
    private final String initialEmail;
    private final String initialPassword;

    public AdminSeeder(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            // Removed hardcoded fallback values for admin credentials
            // Why: Defense-in-depth: Prevents shipping default or well-known credentials into production
            @Value("${security.admin.initial-username}") String initialUsername,
            @Value("${security.admin.initial-email}") String initialEmail,
            @Value("${security.admin.initial-password}") String initialPassword) {
        // Explicitly assert that required administrative environment properties are present and non-blank
        // Why: Halts startup immediately with a clear diagnostic message if required configuration is absent
        if (initialUsername == null || initialUsername.isBlank()) {
            throw new IllegalArgumentException("Missing required initial admin username. Provide ADMIN_INITIAL_USERNAME environment variable.");
        }
        if (initialEmail == null || initialEmail.isBlank()) {
            throw new IllegalArgumentException("Missing required initial admin email. Provide ADMIN_INITIAL_EMAIL environment variable.");
        }
        if (initialPassword == null || initialPassword.isBlank()) {
            throw new IllegalArgumentException("Missing required initial admin password. Provide ADMIN_INITIAL_PASSWORD environment variable.");
        }

        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.initialUsername = initialUsername;
        this.initialEmail = initialEmail;
        this.initialPassword = initialPassword;
    }

    @Override
    public void run(String... args) {
        // Check if an administrator already exists
        // Why: Prevents overwriting or resetting administrator credentials on subsequent application restarts
        if (!userRepository.existsByRole(Role.ADMIN)) {
            log.info("No administrative account detected. Seeding default system administrator: {}", initialUsername);

            String hashedPassword = passwordEncoder.encode(initialPassword);
            User admin = new User(initialUsername, initialEmail, hashedPassword, Role.ADMIN);

            userRepository.save(admin);
            log.info("System administrator account seeded successfully.");
        } else {
            log.info("Administrative account already present. Skipping seeder.");
        }
    }
}
