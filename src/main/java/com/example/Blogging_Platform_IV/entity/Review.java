package com.example.Blogging_Platform_IV.entity;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

/** A review (rating from 1 to 5), stored in the MongoDB "reviews" collection (as in Platform II). */
@Document("reviews")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    @Id
    private String id;

    @Indexed
    private long postId;

    private long reviewerId;
    private String reviewerName;
    private int rating;
    private String text;
    private Instant createdAt;
}
