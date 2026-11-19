package com.example.Blogging_Platform_IV.entity;

/**
 * The three roles of the platform. Spring Security turns each one into the authority "ROLE_<name>",
 * so hasRole('ADMIN') in a @PreAuthorize expression checks for ROLE_ADMIN.
 *
 * ADMIN  manages users, tags and posts and reads the security report.
 * AUTHOR writes and edits posts.
 * READER reads posts and takes part in the discussion (comments and reviews).
 */
public enum Role {
    ADMIN, AUTHOR, READER
}
