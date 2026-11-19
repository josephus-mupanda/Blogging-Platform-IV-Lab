package com.example.Blogging_Platform_IV.controller.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import static com.example.Blogging_Platform_IV.controller.rest.ApiResponse.ok;

import com.example.Blogging_Platform_IV.service.AnalyticsService;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AnalyticsController {
    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard")
    public ApiResponse<Map<String, Long>> dashboard() {
        return ok("Dashboard calculated.", analytics.dashboard());
    }
}
