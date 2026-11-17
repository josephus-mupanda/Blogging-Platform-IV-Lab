package com.example.Blogging_Platform_III.repository;

import com.example.Blogging_Platform_III.entity.Comment;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

/**
 * Comments in MongoDB. MongoRepository extends CrudRepository, so it has the same CRUD methods
 * as the JPA repositories. "Visible" means the comment has no deletedAt value.
 */
public interface CommentRepository extends MongoRepository<Comment, String> {

    /** One page of a post's comments. Uses the index on (postId, createdAt). */
    Page<Comment> findByPostIdAndDeletedAtIsNull(long postId, Pageable pageable);

    Optional<Comment> findByIdAndPostIdAndDeletedAtIsNull(String id, long postId);

    /** Comments that do not belong to a deleted post. Used by the dashboard. */
    long countByPostIdNotInAndDeletedAtIsNull(Collection<Long> archivedPostIds);

    /**
     * Changes the body in one atomic MongoDB operation and returns how many comments changed.
     * The filter and the change happen together, so a comment that another request deleted a
     * moment ago cannot be brought back by an edit.
     */
    @Query("{ '_id': ?0, 'postId': ?1, 'deletedAt': null }")
    @Update("{ '$set': { 'body': ?2 } }")
    long updateBody(String id, long postId, String body);

    /** Hides the comment in one atomic operation. Returns 0 when it was not found or already hidden. */
    @Query("{ '_id': ?0, 'postId': ?1, 'deletedAt': null }")
    @Update("{ '$set': { 'deletedAt': ?2 } }")
    long archive(String id, long postId, Instant deletedAt);
}
