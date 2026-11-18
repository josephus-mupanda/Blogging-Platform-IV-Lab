package com.example.Blogging_Platform_III.repository;

import com.example.Blogging_Platform_III.entity.Review;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Reviews in MongoDB. */
public interface ReviewRepository extends MongoRepository<Review, String> {

    List<Review> findByPostId(long postId);

    /** All reviews written by one user: used to update the copied reviewer name. */
    List<Review> findByReviewerId(long reviewerId);

    /** A user can review a post only once. */
    boolean existsByPostIdAndReviewerId(long postId, long reviewerId);

    void deleteByPostId(long postId);

    void deleteByReviewerId(long reviewerId);
}
