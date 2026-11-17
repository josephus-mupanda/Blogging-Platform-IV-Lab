package com.example.Blogging_Platform_III.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

/** A review, stored in the MongoDB "reviews" collection. One review per user and post. */
@Document("reviews")
@CompoundIndex(name = "postId_1_reviewerId_1", def = "{'postId': 1, 'reviewerId': 1}", unique = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    @Id
    private String id;
    private long postId;
    private long reviewerId;
    private String reviewerName;
    private int rating;
    private String text;
    private Instant createdAt;
}
