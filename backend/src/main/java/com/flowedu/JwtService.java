package com.flowedu;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {
    private final SecretKey key;
    private final long minutes;

    public JwtService(@Value("${flowedu.jwt-secret}") String secret,
                      @Value("${flowedu.jwt-minutes}") long minutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
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
