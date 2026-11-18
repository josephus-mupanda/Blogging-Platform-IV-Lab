package com.example.Blogging_Platform_III.repository;

import com.example.Blogging_Platform_III.entity.Review;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Reviews in MongoDB. */
public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findByPostId(long postId);

    /** A user can review a post only once. */
    boolean existsByPostIdAndReviewerId(long postId, long reviewerId);

    void deleteByPostId(long postId);
}
