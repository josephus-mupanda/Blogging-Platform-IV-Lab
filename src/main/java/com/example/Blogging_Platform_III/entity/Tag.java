package com.example.Blogging_Platform_III.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** A row of the tags table. */
@Entity
@Table(name = "tags")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Tag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long id;

    @Setter
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /** Set when the tag is deleted. The row stays so the name remains reserved. */
    @Setter
    @Column(name = "deleted_at")
    private Instant deletedAt;

    public Tag(String name) {
        this.name = name;
    }
}
