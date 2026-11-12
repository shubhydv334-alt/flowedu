package com.flowedu;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the first administrator from ADMIN_EMAIL and ADMIN_PASSWORD when demo seeding is turned off.
 * Idempotent: nothing happens when the variables are unset or the account already exists.
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class AdminBootstrap implements CommandLineRunner {
    static final int MIN_PASSWORD_LENGTH = 12;

    private final Repos.UserRepo users;
    private final PasswordEncoder encoder;

    @Value("${flowedu.bootstrap-admin.email:}")
    private String email;

    @Value("${flowedu.bootstrap-admin.password:}")
    private String password;

    @Override
    public void run(String... args) {
        if (email == null || email.isBlank()) return;
        String normalized = email.trim().toLowerCase();
        if (users.findByEmail(normalized).isPresent()) {
            log.info("Bootstrap admin {} already exists, leaving it unchanged", normalized);
            return;
        }
        if (password == null || password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_PASSWORD must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        users.save(Domain.AppUser.builder()
                .email(normalized)
                .passwordHash(encoder.encode(password))
                .name("Administrator")
                .role(Domain.Role.ADMIN)
                .build());
        log.info("Created bootstrap admin {}", normalized);
    }
}
