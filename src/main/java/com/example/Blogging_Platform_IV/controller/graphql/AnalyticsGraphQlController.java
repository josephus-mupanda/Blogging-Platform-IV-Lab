package com.example.Blogging_Platform_IV.controller.graphql;

import com.example.Blogging_Platform_IV.service.AnalyticsService;
import java.util.Map;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class AnalyticsGraphQlController {
    private final AnalyticsService analytics;

    public AnalyticsGraphQlController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    /** GraphQL reads the fields users, posts, tags... from the map keys. */
    @QueryMapping
    public Map<String, Long> dashboard() {
        return analytics.dashboard();
    }
}
