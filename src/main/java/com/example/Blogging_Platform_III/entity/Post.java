package com.example.Blogging_Platform_III.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A row of the posts table, with its author and its tags.
 * No @Data here: its equals, hashCode and toString would read the lazy author and tags and
 * send extra queries to the database.
 */
@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_id")
    private Long id;

    /** Many posts belong to one user. LAZY: the user row is only read when it is needed. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Setter
    @Column(nullable = false, length = 200)
    private String title;

    @Setter
    @Column(nullable = false, columnDefinition = "text")
    private String body;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Setter
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** Set when the post is deleted. The row stays so the title remains reserved. */
    @Setter
    @Column(name = "deleted_at")
    private Instant deletedAt;

    /** A post has many tags and a tag is used by many posts. The link rows live in post_tags. */
    @ManyToMany
    @JoinTable(name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    public Post(User author, String title, String body) {
        this.author = author;
        this.title = title;
        this.body = body;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
}
