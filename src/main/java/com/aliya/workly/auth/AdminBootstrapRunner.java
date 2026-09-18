package com.aliya.workly.auth;

import com.aliya.workly.user.AuthProvider;
import com.aliya.workly.user.Role;
import com.aliya.workly.user.User;
import com.aliya.workly.user.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

// Creates the first ADMIN account at startup from ADMIN_EMAIL / ADMIN_PASSWORD. Registration
// rejects the ADMIN role, so this is the only way an admin comes into existence.
//
// Does nothing when either value is unset, and never touches an email that already has an
// account — so restarts are safe and an existing user is never silently promoted.
@Component
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);
    static final int MIN_PASSWORD_LENGTH = 8; // same floor RegisterRequest enforces

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminBootstrapRunner(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            log.info("Admin bootstrap skipped: ADMIN_EMAIL and ADMIN_PASSWORD must both be set");
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException(
                    "ADMIN_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }

        String normalized = email.trim().toLowerCase();
        if (userRepository.existsByEmail(normalized)) {
            log.warn("Admin bootstrap skipped: an account for {} already exists and was left unchanged", normalized);
            return;
        }

        User admin = new User();
        admin.setEmail(normalized);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setFullName("Administrator");
        admin.setRole(Role.ADMIN);
        admin.setAuthProvider(AuthProvider.LOCAL);
        userRepository.save(admin);
        log.info("Admin bootstrap: created ADMIN account for {}", normalized);
    }
}
