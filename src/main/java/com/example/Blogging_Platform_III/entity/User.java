package com.example.Blogging_Platform_III.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A row of the users table.
 * The columns password_hash, role and created_at are not mapped, so PostgreSQL fills them
 * with their default values when a user is inserted.
 * Lombok: getters for every field, setters only for the fields that can change, and the empty
 * constructor that JPA needs (protected, so other code uses the constructor below).
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Setter
    @Column(nullable = false, unique = true)
    private String email;

    @Setter
    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    /** False once the user is deleted. The row stays so their posts keep an author. */
    @Setter
    @Column(nullable = false)
    private boolean active = true;

    public User(String username, String email, String displayName) {
        this.username = username;
        this.email = email;
        this.displayName = displayName;
    }
}
