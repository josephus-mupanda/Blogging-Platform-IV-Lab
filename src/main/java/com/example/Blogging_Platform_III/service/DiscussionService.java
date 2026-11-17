package com.example.Blogging_Platform_III.service;

import com.example.Blogging_Platform_III.entity.Comment;
import com.example.Blogging_Platform_III.entity.Review;
import com.example.Blogging_Platform_III.entity.User;
import com.example.Blogging_Platform_III.exception.BlogException;
import com.example.Blogging_Platform_III.model.CommentPage;
import com.example.Blogging_Platform_III.model.Discussion;
import com.example.Blogging_Platform_III.repository.CommentRepository;
import com.example.Blogging_Platform_III.repository.PostRepository;
import com.example.Blogging_Platform_III.repository.ReviewRepository;
import com.example.Blogging_Platform_III.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Comments and reviews. The post and the user are checked in PostgreSQL, inside the
 * @Transactional method; the comment or review itself is written to MongoDB.
 * Every MongoDB write here changes a single document, and MongoDB applies a single-document
 * write atomically, so a write is either fully done or not done at all.
 */
@Service
public class DiscussionService {
    /** Oldest comment first; the ID keeps comments with the same time in a fixed order. */
    private static final Sort OLDEST_FIRST = Sort.by("createdAt", "id");

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

    /** One page of comments. Page and size are optional (defaults 0 and 10). */
    @Transactional(readOnly = true)
    public CommentPage comments(long postId, Integer page, Integer size) {
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? 10 : size;
        if (pageNumber < 0 || pageSize < 1 || pageSize > 100) {
            throw BlogException.badRequest("Use a nonnegative page and a page size between 1 and 100.");
        }
        requireVisible(postId);
        return new CommentPage(comments.findByPostIdAndDeletedAtIsNull(postId,
                PageRequest.of(pageNumber, pageSize, OLDEST_FIRST)));
    }

    @Transactional(readOnly = true)
    public Comment comment(long postId, String id) {
        requireVisible(postId);
        return findComment(postId, id);
    }

    @Transactional
    public Comment addComment(long postId, long authorId, String body) {
        User author = users.findByIdAndActiveTrue(authorId).orElseThrow(() -> BlogException.notFound("Author"));
        requireVisible(postId);
        return comments.insert(new Comment(null, postId, author.getId(), author.getDisplayName(),
                body, Instant.now(), null));
    }

    @Transactional
    public Comment editComment(long postId, String id, String body) {
        requireVisible(postId);
        if (comments.updateBody(id, postId, body) == 0) {
            throw BlogException.notFound("Comment");
        }
        return findComment(postId, id);
    }

    @Transactional
    public void archiveComment(long postId, String id) {
        requireVisible(postId);
        if (comments.archive(id, postId, Instant.now()) == 0) {
            throw BlogException.notFound("Comment");
        }
    }

    @Transactional(readOnly = true)
    public Discussion reviews(long postId) {
        requireVisible(postId);
        List<Review> found = reviews.findByPostIdOrderByCreatedAtAscIdAsc(postId);
        return new Discussion(found, found.stream().mapToInt(Review::getRating).average().orElse(0));
    }

    @Transactional
    public Review addReview(long postId, long reviewerId, int rating, String text) {
        User reviewer = users.findByIdAndActiveTrue(reviewerId).orElseThrow(() -> BlogException.notFound("Reviewer"));
        requireVisible(postId);
        try {
            return reviews.insert(new Review(null, postId, reviewer.getId(), reviewer.getDisplayName(),
                    rating, text, Instant.now()));
        } catch (DuplicateKeyException exception) {
            throw BlogException.conflict("This user has already reviewed the post.");
        }
    }

    private Comment findComment(long postId, String id) {
        return comments.findByIdAndPostIdAndDeletedAtIsNull(id, postId)
                .orElseThrow(() -> BlogException.notFound("Comment"));
    }

    private void requireVisible(long postId) {
        if (!posts.existsByIdAndDeletedAtIsNull(postId)) {
            throw BlogException.notFound("Post");
        }
    }
}
