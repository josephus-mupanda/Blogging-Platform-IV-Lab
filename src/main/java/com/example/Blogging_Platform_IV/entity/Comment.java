package com.example.Blogging_Platform_IV.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** A comment, stored in the MongoDB "comments" collection (as in Platform II). */
@Document("comments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    private String id;

    /** Indexed because comments are always read by post. */
    @Indexed
    private long postId;

    private long authorId;
    private String authorName;
    private String body;
    private Instant createdAt;
}
