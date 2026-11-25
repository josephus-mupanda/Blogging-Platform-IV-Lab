package com.example.Blogging_Platform_IV.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A blog post. Hibernate creates the "posts" table, the "post_tags" join table and the indexes
 * listed below from this class.
 */
@Entity
@Table(name = "posts", indexes = {
        // Posts are listed newest first by default, so the sort column is indexed.
        @Index(name = "idx_posts_created_at", columnList = "created_at"),
        // Used when we look for the posts of one author.
        @Index(name = "idx_posts_author_id", columnList = "author_id")
})
@Getter
@Setter
@NoArgsConstructor
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String title;

    @Column(nullable = false, length = 10000)
    private String body;

    /** Many posts belong to one user (column author_id). */
    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    // Many tags per post and many posts per tag (join table post_tags).
    // EAGER: the tags are loaded with the post. BatchSize: the tags of 50 posts are loaded in one query.
    @ManyToMany(fetch = FetchType.EAGER)
    @BatchSize(size = 50)
    @JoinTable(name = "post_tags",
            joinColumns = @JoinColumn(name = "post_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id"),
            indexes = @Index(name = "idx_post_tags_tag_id", columnList = "tag_id"))
    private Set<Tag> tags = new HashSet<>();

    /** Filled by Hibernate when the post is inserted. */
    @CreationTimestamp
    private LocalDateTime createdAt;

    /** Filled by Hibernate every time the post is updated. */
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Optimistic locking: if two people save the same post at the same time, the second save is refused.
    @Version
    @JsonIgnore
    private Long version;

    public Post(User author, String title, String body) {
        this.author = author;
        this.title = title;
        this.body = body;
    }
}
