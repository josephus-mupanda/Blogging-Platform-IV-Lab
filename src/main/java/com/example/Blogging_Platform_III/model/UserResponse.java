package com.example.Blogging_Platform_III.model;

import com.example.Blogging_Platform_III.entity.User;

/** The public view of a user. The email and the active flag stay private. */
public class UserResponse {
    private long id;
    private String username;
    private String displayName;

    public UserResponse() {
    }

    public UserResponse(long id, String username, String displayName) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
    }

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName());
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}
