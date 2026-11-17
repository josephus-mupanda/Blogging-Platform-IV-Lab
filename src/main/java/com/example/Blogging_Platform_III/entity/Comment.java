package com.example.Blogging_Platform_III.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/** A comment, stored in the MongoDB "comments" collection. */
@Document("comments")
@CompoundIndex(name = "postId_1_createdAt_1", def = "{'postId': 1, 'createdAt': 1}")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Comment {
    @Id
    private String id;
    private long postId;
    private long authorId;
    private String authorName;
    private String body;
    private Instant createdAt;

    /** Set when the comment is hidden. It is internal, so it stays out of the API response. */
    @JsonIgnore
    private Instant deletedAt;
}
