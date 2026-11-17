package com.example.Blogging_Platform_III.repository;

import com.example.Blogging_Platform_III.entity.Review;
import java.util.Collection;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Reviews in MongoDB. The unique index on (postId, reviewerId) allows one review per user and post. */
public interface ReviewRepository extends MongoRepository<Review, String> {

    /** All reviews of a post, oldest first. */
    List<Review> findByPostIdOrderByCreatedAtAscIdAsc(long postId);

    /** Reviews that do not belong to a deleted post. Used by the dashboard. */
    long countByPostIdNotIn(Collection<Long> archivedPostIds);
}
