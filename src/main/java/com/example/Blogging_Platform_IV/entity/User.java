package com.example.Blogging_Platform_IV.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A blog user. Hibernate creates the "users" table from this class. */
@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** Kept private: it is never sent back in an API response. */
    @JsonIgnore
    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 100)
    private String displayName;

    /** The BCrypt hash of the password. The password itself is never stored or sent back. */
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    /** One role per user. It goes into the JWT and decides which endpoints the user can call. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    public User(String username, String email, String displayName, String passwordHash, Role role) {
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.password = passwordHash;
        this.role = role;
    }
}
