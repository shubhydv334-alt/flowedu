package com.flowedu;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    static final int MIN_PASSWORD_LENGTH = 8;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final Repos.UserRepo users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginAttemptLimiter limiter;

    public record LoginReq(String email, String password) {}
    public record UserReq(String email, String password, String name, Domain.Role role) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReq r, HttpServletRequest request) {
        if (r.email() == null || r.password() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Email and password are required"));
        }
        String email = r.email().trim().toLowerCase();
        String key = email + "|" + request.getRemoteAddr();
        if (limiter.isBlocked(key)) {
            return ResponseEntity.status(429)
                    .body(Map.of("error", "Too many failed sign-in attempts. Try again in a few minutes."));
        }
        var u = users.findByEmail(email).orElse(null);
        if (u == null || !encoder.matches(r.password(), u.getPasswordHash())) {
            limiter.recordFailure(key);
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
        limiter.reset(key);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("token", jwt.create(u));
        body.put("name", u.getName());
        body.put("email", u.getEmail());
        body.put("role", u.getRole().name());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/me")
    public Map<String, Object> me(Authentication auth) {
        var u = users.findByEmail(auth.getName()).orElseThrow();
        return Map.of("name", u.getName(), "email", u.getEmail(), "role", u.getRole().name());
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public List<Map<String, Object>> listUsers() {
        return users.findAll().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId());
            m.put("name", u.getName());
            m.put("email", u.getEmail());
            m.put("role", u.getRole().name());
            return m;
        }).toList();
    }

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> createUser(@RequestBody UserReq r) {
        if (r.email() == null || r.password() == null || r.name() == null || r.role() == null) {
            throw new IllegalArgumentException("email, password, name and role are required");
        }
        String email = r.email().trim().toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("Enter a valid email address");
        }
        if (r.password().length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
        }
        if (r.name().isBlank()) {
            throw new IllegalArgumentException("Name must not be blank");
        }
        var u = users.save(Domain.AppUser.builder()
                .email(email)
                .passwordHash(encoder.encode(r.password()))
                .name(r.name().trim())
                .role(r.role())
                .build());
        return Map.of("id", u.getId(), "email", u.getEmail(), "role", u.getRole().name());
    }
}
