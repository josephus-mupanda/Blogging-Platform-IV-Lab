package com.example.Blogging_Platform_III.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

/**
 * Turns on Spring Cache. After this, @Cacheable and @CacheEvict on service methods work.
 * The caches are created by Spring Boot from spring.cache.cache-names in application.properties.
 * The names are kept here as constants so the services cannot misspell them.
 */
@Configuration
@EnableCaching
public class CacheConfig {
    /** One post by ID. Key: the post ID. */
    public static final String POSTS = "posts";
    /** One user by ID. Key: the user ID. */
    public static final String USERS = "users";
    /** The list of all users. One entry. */
    public static final String USER_LIST = "userList";
    /** One tag by ID. Key: the tag ID. */
    public static final String TAGS = "tags";
    /** The list of all tags. One entry. */
    public static final String TAG_LIST = "tagList";
}
