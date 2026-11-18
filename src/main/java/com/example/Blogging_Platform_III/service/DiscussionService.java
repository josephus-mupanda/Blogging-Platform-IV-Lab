package com.example.Blogging_Platform_III.service;

import com.example.Blogging_Platform_III.entity.Comment;
import com.example.Blogging_Platform_III.entity.Review;
import com.example.Blogging_Platform_III.entity.User;
import com.example.Blogging_Platform_III.exception.BlogException;
import com.example.Blogging_Platform_III.repository.CommentRepository;
import com.example.Blogging_Platform_III.repository.PostRepository;
import com.example.Blogging_Platform_III.repository.ReviewRepository;
import com.example.Blogging_Platform_III.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comments and reviews (MongoDB). The post and the user are checked in PostgreSQL first.
 * Each MongoDB save changes one document, and MongoDB saves one document atomically.
 */
@Service
public class DiscussionService {
    private final CommentRepository comments;
    private final ReviewRepository reviews;
    private final PostRepository posts;
    private final UserRepository users;

    public DiscussionService(CommentRepository comments, ReviewRepository reviews, PostRepository posts,
                             UserRepository users) {
        this.comments = comments;
        this.reviews = reviews;
        this.posts = posts;
        this.users = users;
    }

    public Page<Comment> comments(Long postId, Pageable pageable) {
        requirePost(postId);
        return comments.findByPostId(postId, pageable);
    }

    public Comment comment(Long postId, String id) {
        return comments.findById(id)
                .filter(comment -> comment.getPostId() == postId)
                .orElseThrow(() -> BlogException.notFound("Comment"));
    }

    @Transactional
    public Comment addComment(Long postId, Long authorId, String body) {
        requirePost(postId);
        User author = users.findById(authorId).orElseThrow(() -> BlogException.notFound("Author"));
        return comments.save(new Comment(null, postId, author.getId(), author.getDisplayName(), body, Instant.now()));
    }

    @Transactional
    public Comment editComment(Long postId, String id, String body) {
        Comment comment = comment(postId, id);
        comment.setBody(body);
        return comments.save(comment);
    }

    @Transactional
    public void deleteComment(Long postId, String id) {
        comments.delete(comment(postId, id));
    }

    public List<Review> reviews(Long postId) {
        requirePost(postId);
        return reviews.findByPostId(postId);
    }

    @Transactional
    public Review addReview(Long postId, Long reviewerId, int rating, String text) {
        requirePost(postId);
        User reviewer = users.findById(reviewerId).orElseThrow(() -> BlogException.notFound("Reviewer"));
        if (reviews.existsByPostIdAndReviewerId(postId, reviewerId)) {
            throw BlogException.conflict("This user has already reviewed the post.");
        }
        return reviews.save(new Review(null, postId, reviewer.getId(), reviewer.getDisplayName(), rating, text,
                Instant.now()));
    }

    private void requirePost(Long postId) {
        if (!posts.existsById(postId)) {
            throw BlogException.notFound("Post");
        }
    }
}
