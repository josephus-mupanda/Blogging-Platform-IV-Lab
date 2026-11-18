package com.example.Blogging_Platform_III.service;

import com.example.Blogging_Platform_III.entity.Comment;
import com.example.Blogging_Platform_III.entity.Review;
import com.example.Blogging_Platform_III.entity.User;
import com.example.Blogging_Platform_III.exception.BlogException;
import com.example.Blogging_Platform_III.repository.CommentRepository;
import com.example.Blogging_Platform_III.repository.PostRepository;
import com.example.Blogging_Platform_III.repository.ReviewRepository;
import com.example.Blogging_Platform_III.repository.UserRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * rollbackFor = Exception.class: by default Spring only rolls back for RuntimeException.
 * With this, any exception thrown by the method undoes all of its database changes.
 */
@Service
public class UserService {
    private final UserRepository users;
    private final PostRepository posts;
    private final CommentRepository comments;
    private final ReviewRepository reviews;

    public UserService(UserRepository users, PostRepository posts, CommentRepository comments,
                       ReviewRepository reviews) {
        this.users = users;
        this.posts = posts;
        this.comments = comments;
        this.reviews = reviews;
    }

    public List<User> list() {
        return users.findAll(Sort.by("username"));
    }

    /** Cached: the first call reads the database, the next calls read the "users" cache. */
    @Cacheable(value = "users", key = "#id")
    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> BlogException.notFound("User"));
    }

    @Transactional(rollbackFor = Exception.class)
    public User create(String username, String email, String displayName) {
        return users.save(new User(username, email, displayName));
    }

    /**
     * Keeps every cached or copied value of this user up to date:
     * - @CachePut puts the new user in the "users" cache.
     * - @CacheEvict empties the "posts" cache, because each cached post contains its author.
     *   Without it, a post would still show the old display name.
     * - The comments and reviews in MongoDB keep a copy of the name, so they are updated too.
     * saveAndFlush runs the UPDATE first: if it fails (email already used), the method stops
     * before MongoDB is touched, the transaction is rolled back and nothing is cached.
     */
    @Transactional(rollbackFor = Exception.class)
    @Caching(put = @CachePut(value = "users", key = "#id"),
            evict = @CacheEvict(value = "posts", allEntries = true))
    public User update(Long id, String email, String displayName) {
        User user = users.findById(id).orElseThrow(() -> BlogException.notFound("User"));
        user.setEmail(email);
        user.setDisplayName(displayName);
        User saved = users.saveAndFlush(user);

        List<Comment> userComments = comments.findByAuthorId(id);
        userComments.forEach(comment -> comment.setAuthorName(displayName));
        comments.saveAll(userComments);

        List<Review> userReviews = reviews.findByReviewerId(id);
        userReviews.forEach(review -> review.setReviewerName(displayName));
        reviews.saveAll(userReviews);

        return saved;
    }

    /**
     * A user who still has posts cannot be deleted (409). Otherwise the user is deleted in
     * PostgreSQL first (flush runs the DELETE now), and only then are their comments and reviews
     * deleted in MongoDB. If the PostgreSQL delete fails, MongoDB is never touched.
     */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "users", key = "#id")
    public void delete(Long id) {
        User user = users.findById(id).orElseThrow(() -> BlogException.notFound("User"));
        if (posts.existsByAuthorId(id)) {
            throw BlogException.conflict("This user still has posts. Delete the posts first.");
        }
        users.delete(user);
        users.flush();
        comments.deleteByAuthorId(id);
        reviews.deleteByReviewerId(id);
    }

    public boolean usernameExists(String username) {
        return users.existsByUsername(username);
    }

    public boolean emailExists(String email) {
        return users.existsByEmail(email);
    }
}
