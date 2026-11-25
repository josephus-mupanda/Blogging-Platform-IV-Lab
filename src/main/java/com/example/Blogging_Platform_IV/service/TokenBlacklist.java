package com.example.Blogging_Platform_IV.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * The tokens that were revoked by a logout. A JWT cannot be cancelled by the server on its own,
 * because it is valid until its expiry date, so the server remembers the revoked ones.
 *
 * Data structure: a hash map. Checking a token is one lookup, O(1) on average, however many tokens
 * are revoked. It is a ConcurrentHashMap because many requests read it while a logout writes to it.
 *
 * Hashing: the map key is the SHA-256 hash of the token, not the token. A memory dump or a log of
 * this map then does not contain a usable token.
 *
 * Cleaning: a revoked token stops being dangerous when it expires anyway, so each entry keeps the
 * expiry date of its token and the expired entries are removed. The map never grows without limit.
 *
 * The map lives in memory: it is emptied when the application restarts, and it is not shared
 * between several servers. For a real deployment it would move to a shared store such as Redis.
 */
@Component
public class TokenBlacklist {
    /** SHA-256 of the token -> when the token expires. */
    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    public void revoke(String token, Instant expiresAt) {
        Instant now = Instant.now();
        revoked.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
        revoked.put(sha256(token), expiresAt);
    }

    public boolean isRevoked(String token) {
        Instant expiresAt = revoked.get(sha256(token));
        return expiresAt != null && expiresAt.isAfter(Instant.now());
    }

    public int size() {
        return revoked.size();
    }

    static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is always available in Java", e);
        }
    }
}
