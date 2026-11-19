package com.example.Blogging_Platform_IV.security;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.LongAdder;
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
 *   BRUTE_FORCE_SUSPECTED key=user:admin failures=5 windowMinutes=10
 *
 * Brute-force detection: for every username and every IP address that fails to log in, the time of
 * each failure is kept in a queue. Failures older than the window are removed from the front, so
 * the queue size is the number of recent failures. When it reaches the threshold, a WARN is logged.
 */
@Component
public class SecurityEvents {
    private static final Logger AUDIT = LoggerFactory.getLogger("security.audit");
    static final int FAILURE_THRESHOLD = 5;
    static final Duration FAILURE_WINDOW = Duration.ofMinutes(10);
    private static final int MAX_TRACKED_KEYS = 10_000;

    /** Event name -> how many times it happened since the application started. */
    private final Map<String, LongAdder> counters = new ConcurrentHashMap<>();
    /** Username -> number of requests made with a valid token (access frequency). */
    private final Map<String, LongAdder> requestsPerUser = new ConcurrentHashMap<>();
    /** "user:<name>" or "ip:<address>" -> times of the recent failed logins. */
    private final Map<String, Deque<Instant>> failures = new ConcurrentHashMap<>();

    public void loginSuccess(String username, String ip) {
        count("LOGIN_SUCCESS");
        AUDIT.info("LOGIN_SUCCESS user={} ip={}", username, ip);
    }

    public void loginFailure(String username, String ip) {
        count("LOGIN_FAILURE");
        AUDIT.warn("LOGIN_FAILURE user={} ip={}", username, ip);
        recordFailure("user:" + username);
        recordFailure("ip:" + ip);
    }

    /** A request carried a valid token. Counted per user, logged at DEBUG because it happens a lot. */
    public void tokenUsed(String username, String path) {
        count("TOKEN_USED");
        requestsPerUser.computeIfAbsent(username, key -> new LongAdder()).increment();
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
        Instant limit = Instant.now().minus(FAILURE_WINDOW);
        Map<String, Integer> recentFailures = new TreeMap<>();
        failures.forEach((key, times) -> {
            long recent = times.stream().filter(time -> time.isAfter(limit)).count();
            if (recent > 0) {
                recentFailures.put(key, (int) recent);
            }
        });
        List<String> suspicious = new ArrayList<>();
        recentFailures.forEach((key, total) -> {
            if (total >= FAILURE_THRESHOLD) {
                suspicious.add(key);
            }
        });

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("events", snapshot(counters));
        report.put("requestsPerUser", snapshot(requestsPerUser));
        report.put("failedLoginsInLast" + FAILURE_WINDOW.toMinutes() + "Minutes", recentFailures);
        report.put("suspiciousSources", suspicious);
        report.put("bruteForceThreshold", FAILURE_THRESHOLD + " failed logins in " + FAILURE_WINDOW.toMinutes() + " minutes");
        report.put("revokedTokensTracked", revokedTokens);
        return report;
    }

    private void count(String event) {
        counters.computeIfAbsent(event, key -> new LongAdder()).increment();
    }

    private void recordFailure(String key) {
        Instant now = Instant.now();
        if (failures.size() > MAX_TRACKED_KEYS) {
            // An attacker can try endless random usernames: forget the sources whose failures all expired.
            failures.values().removeIf(times -> times.isEmpty() || times.peekLast().isBefore(now.minus(FAILURE_WINDOW)));
        }
        Deque<Instant> times = failures.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        times.add(now);
        while (!times.isEmpty() && times.peekFirst().isBefore(now.minus(FAILURE_WINDOW))) {
            times.pollFirst();
        }
        if (times.size() == FAILURE_THRESHOLD) {
            count("BRUTE_FORCE_SUSPECTED");
            AUDIT.warn("BRUTE_FORCE_SUSPECTED key={} failures={} windowMinutes={}",
                    key, times.size(), FAILURE_WINDOW.toMinutes());
        }
    }

    private static Map<String, Long> snapshot(Map<String, LongAdder> source) {
        Map<String, Long> copy = new TreeMap<>();
        source.forEach((key, value) -> copy.put(key, value.sum()));
        return copy;
    }
}
