package com.example.Blogging_Platform_IV.service;

import com.example.Blogging_Platform_IV.enums.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

// Creates and checks the JWTs. The token is signed with HMAC SHA-256 and a secret key.
@Service
public class JwtService {
    private final SecretKey key;
    private final Duration lifetime;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));   // at least 32 characters
        this.lifetime = Duration.ofMinutes(expirationMinutes);
    }

    // Claims: id, subject (username), roles, issued at, expiry.
    public String generate(String username, Role role) {
        Instant now = Instant.now();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("roles", List.of(role.name()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(lifetime)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // Checks the signature and the expiry, then returns the claims.
    // Throws a JwtException if the token is changed, malformed or expired.
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long lifetimeSeconds() {
        return lifetime.toSeconds();
    }
}
