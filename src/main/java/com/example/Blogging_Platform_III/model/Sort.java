package com.example.Blogging_Platform_III.model;

import org.springframework.data.domain.Sort.Order;

/** The sort options a client can choose for posts. */
public enum Sort {
    NEWEST, OLDEST, TITLE_ASC, TITLE_DESC;

    /**
     * The Spring Data sort that goes into the Pageable. The post ID is always the last key,
     * so two posts with the same date or title keep a fixed order and pages never overlap.
     * ignoreCase() makes Spring Data sort by lower(title).
     */
    public org.springframework.data.domain.Sort toSpringSort() {
        return switch (this) {
            case NEWEST -> org.springframework.data.domain.Sort.by(Order.desc("createdAt"), Order.desc("id"));
            case OLDEST -> org.springframework.data.domain.Sort.by(Order.asc("createdAt"), Order.asc("id"));
            case TITLE_ASC -> org.springframework.data.domain.Sort.by(Order.asc("title").ignoreCase(), Order.asc("id"));
            case TITLE_DESC -> org.springframework.data.domain.Sort.by(Order.desc("title").ignoreCase(), Order.desc("id"));
        };
    }
}
