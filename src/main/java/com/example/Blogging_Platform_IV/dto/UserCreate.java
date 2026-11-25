package com.example.Blogging_Platform_IV.dto;

import com.example.Blogging_Platform_IV.enums.Role;
import com.example.Blogging_Platform_IV.validation.UniqueAccount;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@UniqueAccount
public class UserCreate {
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Email
    @Pattern(regexp = ".+@.+\\..+", message = "must be a valid email address")
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 100)
    private String displayName;

    // BCrypt only uses the first 72 bytes of a password.
    @NotBlank
    @Size(min = 8, max = 72)
    private String password;

    // optional: READER when it is missing
    private Role role;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim();
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email == null ? null : email.trim();
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName == null ? null : displayName.trim();
    }
}
