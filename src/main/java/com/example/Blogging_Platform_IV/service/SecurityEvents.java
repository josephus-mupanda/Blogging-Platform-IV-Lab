package com.example.Blogging_Platform_IV.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Writes one log line for every security event and keeps counters for the security report.
 *
 * The lines go to the logger "security.audit" in a key=value form that is easy to search, for example:
 *   LOGIN_FAILURE user=admin ip=127.0.0.1
 *   TOKEN_REJECTED reason="The token has expired. Log in again." ip=127.0.0.1 path=/api/users
 *   ACCESS_DENIED user=bob path=/api/users
 *   BRUTE_FORCE_SUSPECTED key=user:admin failures=5
 *
 * All the counters are hash maps (ConcurrentHashMap, safe when many requests run at the same time).
 *
 * Brute-force detection: a map counts the failed logins in a row for every username and every IP
 * address. A successful login of that username or IP puts its counter back to zero. When a counter
 * reaches FAILURE_LIMIT, a warning is logged.
 */
@Component
public class SecurityEvents {
    private static final Logger AUDIT = LoggerFactory.getLogger("security.audit");
    static final int FAILURE_LIMIT = 5;
    private static final int MAX_TRACKED_SOURCES = 10_000;

    /** Event name -> how many times it happened since the application started. */
    private final Map<String, Long> events = new ConcurrentHashMap<>();
    /** Username -> number of requests made with a valid token (access frequency). */
    private final Map<String, Long> requestsPerUser = new ConcurrentHashMap<>();
    /** "user:<name>" or "ip:<address>" -> failed logins in a row. */
    private final Map<String, Integer> failedLogins = new ConcurrentHashMap<>();

    public void loginSuccess(String username, String ip) {
        count("LOGIN_SUCCESS");
        AUDIT.info("LOGIN_SUCCESS user={} ip={}", username, ip);
        failedLogins.remove("user:" + username);
        failedLogins.remove("ip:" + ip);
    }

    public void loginFailure(String username, String ip) {
        count("LOGIN_FAILURE");
        AUDIT.warn("LOGIN_FAILURE user={} ip={}", username, ip);
        if (failedLogins.size() > MAX_TRACKED_SOURCES) {
            failedLogins.clear();   // someone is trying endless random usernames: do not let the map grow forever
        }
        recordFailure("user:" + username);
        recordFailure("ip:" + ip);
    }

    /** A request carried a valid token. Counted per user, logged at DEBUG because it happens a lot. */
    public void tokenUsed(String username, String path) {
        count("TOKEN_USED");
        requestsPerUser.merge(username, 1L, Long::sum);
        AUDIT.debug("TOKEN_USED user={} path={}", username, path);
    }

    public void tokenRejected(String reason, String ip, String path) {
        count("TOKEN_REJECTED");
        AUDIT.warn("TOKEN_REJECTED reason=\"{}\" ip={} path={}", reason, ip, path);
    }

    public void tokenRevoked(String username) {
        count("TOKEN_REVOKED");
        AUDIT.info("TOKEN_REVOKED user={}", username);
    }

    /** A request without any token reached an endpoint that needs a login. */
    public void unauthenticatedRequest(String ip, String path) {
        count("UNAUTHENTICATED_REQUEST");
        AUDIT.warn("UNAUTHENTICATED_REQUEST ip={} path={}", ip, path);
    }

    /** A logged-in user called something their role does not allow. */
    public void accessDenied(String username, String path) {
        count("ACCESS_DENIED");
        AUDIT.warn("ACCESS_DENIED user={} path={}", username, path);
    }

    public void oauthLogin(String username, String email) {
        count("OAUTH_LOGIN");
        AUDIT.info("OAUTH_LOGIN user={} email={}", username, email);
    }

    /** What the admin sees at GET /api/security/report. */
    public Map<String, Object> report(int revokedTokens) {
        List<String> suspicious = new ArrayList<>();
        failedLogins.forEach((source, failures) -> {
            if (failures >= FAILURE_LIMIT) {
                suspicious.add(source);
            }
        });

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("events", new TreeMap<>(events));
        report.put("requestsPerUser", new TreeMap<>(requestsPerUser));
        report.put("failedLoginsInARow", new TreeMap<>(failedLogins));
        report.put("suspiciousSources", suspicious);
        report.put("bruteForceLimit", FAILURE_LIMIT + " failed logins in a row");
        report.put("revokedTokensTracked", revokedTokens);
        return report;
    }

    private void count(String event) {
        events.merge(event, 1L, Long::sum);
    }

    private void recordFailure(String source) {
        int failures = failedLogins.merge(source, 1, Integer::sum);
        if (failures == FAILURE_LIMIT) {
            count("BRUTE_FORCE_SUSPECTED");
            AUDIT.warn("BRUTE_FORCE_SUSPECTED key={} failures={}", source, failures);
        }
    }
}
