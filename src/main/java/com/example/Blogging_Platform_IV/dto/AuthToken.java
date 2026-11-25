package com.example.Blogging_Platform_IV.dto;

import com.example.Blogging_Platform_IV.enums.Role;

/** What /auth/login and the Google login return. The client sends the token as "Authorization: Bearer <token>". */
public record AuthToken(String token, String tokenType, long expiresInSeconds, String username, Role role) {
}
