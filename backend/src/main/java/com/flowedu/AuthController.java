package com.flowedu;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final Repos.UserRepo users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    public record LoginReq(String email, String password) {}
    public record UserReq(String email, String password, String name, Domain.Role role) {}

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginReq r) {
        if (r.email() == null || r.password() == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Email and password are required"));
        }
        var u = users.findByEmail(r.email().trim().toLowerCase()).orElse(null);
        if (u == null || !encoder.matches(r.password(), u.getPasswordHash())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password"));
        }
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
        var u = users.save(Domain.AppUser.builder()
                .email(r.email().trim().toLowerCase())
                .passwordHash(encoder.encode(r.password()))
                .name(r.name())
                .role(r.role())
                .build());
        return Map.of("id", u.getId(), "email", u.getEmail(), "role", u.getRole().name());
    }
}
