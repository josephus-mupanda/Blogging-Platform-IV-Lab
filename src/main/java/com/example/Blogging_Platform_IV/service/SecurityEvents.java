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

// Logs the security events (logins, rejected tokens, denied requests) and counts them for the report.
// Counters are hash maps. A brute force is 5 failed logins in a row for the same username or IP.
@Component
public class SecurityEvents {
    private static final Logger LOG = LoggerFactory.getLogger("security.audit");
    private static final int FAILURE_LIMIT = 5;

    private final Map<String, Long> events = new ConcurrentHashMap<>();           // event name -> count
    private final Map<String, Long> requestsPerUser = new ConcurrentHashMap<>();  // username -> requests
    private final Map<String, Integer> failedLogins = new ConcurrentHashMap<>();  // "user:x" or "ip:x" -> failures in a row

    public void loginSuccess(String username, String ip) {
        count("LOGIN_SUCCESS");
        LOG.info("LOGIN_SUCCESS user={} ip={}", username, ip);
        failedLogins.remove("user:" + username);   // a good login starts the count again
        failedLogins.remove("ip:" + ip);
    }

    public void loginFailure(String username, String ip) {
        count("LOGIN_FAILURE");
        LOG.warn("LOGIN_FAILURE user={} ip={}", username, ip);
        recordFailure("user:" + username);
        recordFailure("ip:" + ip);
    }

    public void tokenUsed(String username, String path) {
        count("TOKEN_USED");
        requestsPerUser.merge(username, 1L, Long::sum);
        LOG.debug("TOKEN_USED user={} path={}", username, path);
    }

    public void tokenRejected(String reason, String ip, String path) {
        count("TOKEN_REJECTED");
        LOG.warn("TOKEN_REJECTED reason=\"{}\" ip={} path={}", reason, ip, path);
    }

    public void tokenRevoked(String username) {
        count("TOKEN_REVOKED");
        LOG.info("TOKEN_REVOKED user={}", username);
    }

    public void unauthenticatedRequest(String ip, String path) {
        count("UNAUTHENTICATED_REQUEST");
        LOG.warn("UNAUTHENTICATED_REQUEST ip={} path={}", ip, path);
    }

    public void accessDenied(String username, String path) {
        count("ACCESS_DENIED");
        LOG.warn("ACCESS_DENIED user={} path={}", username, path);
    }

    public void oauthLogin(String username, String email) {
        count("OAUTH_LOGIN");
        LOG.info("OAUTH_LOGIN user={} email={}", username, email);
    }

    // Used by GET /api/security/report.
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
            LOG.warn("BRUTE_FORCE_SUSPECTED key={} failures={}", source, failures);
        }
    }
}
