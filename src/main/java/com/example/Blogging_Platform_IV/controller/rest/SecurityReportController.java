package com.example.Blogging_Platform_IV.controller.rest;

import static com.example.Blogging_Platform_IV.controller.rest.ApiResponse.ok;

import com.example.Blogging_Platform_IV.security.SecurityEvents;
import com.example.Blogging_Platform_IV.security.TokenBlacklist;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/security")
public class SecurityReportController {
    private final SecurityEvents events;
    private final TokenBlacklist blacklist;

    public SecurityReportController(SecurityEvents events, TokenBlacklist blacklist) {
        this.events = events;
        this.blacklist = blacklist;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/report")
    @Operation(summary = "Security event report (ADMIN)",
            description = "Counts of logins, failures, token uses, rejected tokens and denied requests, the number of "
                    + "requests per user, and the sources with too many failed logins (possible brute force).")
    public ApiResponse<Map<String, Object>> report() {
        return ok("Security report calculated.", events.report(blacklist.size()));
    }
}
