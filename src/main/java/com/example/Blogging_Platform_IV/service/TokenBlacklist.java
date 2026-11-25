package com.example.Blogging_Platform_IV.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

// The tokens that were logged out. A JWT is valid until it expires, so the server remembers the revoked ones.
// It is a hash map: checking a token is one lookup. The key is the SHA-256 hash of the token (not the
// token itself) and the value is the expiry date, so expired entries can be removed.
// The map is in memory: it is empty after a restart.
@Component
public class TokenBlacklist {
    private final Map<String, Instant> revoked = new ConcurrentHashMap<>();

    public void revoke(String token, Instant expiresAt) {
        revoked.values().removeIf(expiry -> expiry.isBefore(Instant.now()));   // clean the expired ones
        revoked.put(sha256(token), expiresAt);
    }

    public boolean isRevoked(String token) {
        return revoked.containsKey(sha256(token));
    }

    public int size() {
        return revoked.size();
    }

    private String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
