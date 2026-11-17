package com.example.Blogging_Platform_III.model;

import com.example.Blogging_Platform_III.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** The public view of a user. The email and the active flag stay private. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private long id;
    private String username;
    private String displayName;

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getDisplayName());
    }
}
