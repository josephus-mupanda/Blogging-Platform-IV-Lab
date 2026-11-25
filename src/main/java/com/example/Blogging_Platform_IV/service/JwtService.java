package com.example.Blogging_Platform_IV.service;

import com.example.Blogging_Platform_IV.entity.Role;
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

/**
 * Creates and checks the JSON Web Tokens.
 *
 * A JWT has three parts separated by dots: header.payload.signature. The payload carries the claims:
 *   sub   the username
 *   roles the role of the user, for example ["AUTHOR"]
 *   iat   when the token was issued
 *   exp   when it stops being valid
 *   jti   a unique id for the token
 * The signature is an HMAC SHA-256 of header.payload made with a secret key that only the server
 * knows. Anyone can read the payload (it is only Base64 encoded), but nobody can change it without
 * the signature check failing.
 */
@Service
public class JwtService {
    private final SecretKey key;
    private final Duration lifetime;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-minutes}") long expirationMinutes) {
        // HS256 needs a key of at least 256 bits, which is 32 characters here.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.lifetime = Duration.ofMinutes(expirationMinutes);
    }

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

    /**
     * Checks the signature and the expiry date, then returns the claims.
     * Throws a JwtException when the token is tampered with, malformed, unsigned or expired
     * (ExpiredJwtException), and the caller turns that into a 401.
     */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long lifetimeSeconds() {
        return lifetime.toSeconds();
    }
}
