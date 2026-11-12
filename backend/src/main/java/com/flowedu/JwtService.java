package com.flowedu;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Set;

@Service
public class JwtService {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** Secrets that ship in application.yml and .env.example. They are public, so they must never sign production tokens. */
    private static final Set<String> PLACEHOLDER_SECRETS = Set.of(
            "change-this-secret-change-this-secret-123456",
            "change-me-to-a-random-string-at-least-32-characters");

    private final SecretKey key;
    private final long minutes;

    public JwtService(@Value("${flowedu.jwt-secret}") String secret,
                      @Value("${flowedu.jwt-minutes}") long minutes,
                      Environment env) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long");
        }
        if (PLACEHOLDER_SECRETS.contains(secret)) {
            if (env.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException("JWT_SECRET is still set to a published placeholder value. "
                        + "Generate a new one, for example with: openssl rand -base64 48");
            }
            log.warn("JWT_SECRET is a published placeholder. Fine for local development, never for production.");
        }
        this.key = Keys.hmacShaKeyFor(bytes);
        this.minutes = minutes;
    }

    public String create(Domain.AppUser u) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(u.getEmail())
                .claim("role", u.getRole().name())
                .claim("name", u.getName())
                .issuedAt(new Date(now))
                .expiration(new Date(now + minutes * 60_000))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
