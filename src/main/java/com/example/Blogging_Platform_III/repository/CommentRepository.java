package com.example.Blogging_Platform_III.repository;

import com.example.Blogging_Platform_III.entity.Comment;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

/** Comments in MongoDB. MongoRepository extends CrudRepository, like the JPA repositories. */
public interface CommentRepository extends MongoRepository<Comment, String> {

    /** One page of the comments of a post (derived query). */
    Page<Comment> findByPostId(long postId, Pageable pageable);

    /** All comments written by one user: used to update the copied author name. */
    List<Comment> findByAuthorId(long authorId);

    void deleteByPostId(long postId);

    void deleteByAuthorId(long authorId);
}
