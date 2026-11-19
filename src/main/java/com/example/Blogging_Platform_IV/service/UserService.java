package com.example.Blogging_Platform_IV.service;

import com.example.Blogging_Platform_IV.entity.Comment;
import com.example.Blogging_Platform_IV.entity.Review;
import com.example.Blogging_Platform_IV.entity.Role;
import com.example.Blogging_Platform_IV.entity.User;
import com.example.Blogging_Platform_IV.exception.BlogException;
import com.example.Blogging_Platform_IV.repository.CommentRepository;
import com.example.Blogging_Platform_IV.repository.PostRepository;
import com.example.Blogging_Platform_IV.repository.ReviewRepository;
import com.example.Blogging_Platform_IV.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository users, PostRepository posts, CommentRepository comments,
                       ReviewRepository reviews, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.posts = posts;
        this.comments = comments;
        this.reviews = reviews;
        this.passwordEncoder = passwordEncoder;
    }

    public List<User> list() {
        return users.findAll(Sort.by("username"));
    }

    /** Cached: the first call reads the database, the next calls read the "users" cache. */
    @Cacheable(value = "users", key = "#id")
    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> BlogException.notFound("User"));
    }

    /** The password is hashed with BCrypt here, so only the hash reaches the database. No role means READER. */
    @Transactional(rollbackFor = Exception.class)
    public User create(String username, String email, String displayName, String password, Role role) {
        return users.save(new User(username, email, displayName, passwordEncoder.encode(password),
                role == null ? Role.READER : role));
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

    /**
     * Google login: the account is found by its email. A person who logs in with Google for the first
     * time gets a new account with the READER role (assigned automatically), their Google name as
     * display name and a username made from the start of their email address. A Google account has no
     * password here, so the account gets a long random one that nobody knows (BCrypt-hashed as usual).
     * An existing account with the same email is reused as it is, with the role it already has.
     */
    @Transactional(rollbackFor = Exception.class)
    public User findOrCreateGoogleUser(String email, String name) {
        return users.findByEmail(email).orElseGet(() -> {
            String username = freeUsername(email);
            String displayName = name == null || name.isBlank() ? username : name.trim();
            if (displayName.length() > 100) {
                displayName = displayName.substring(0, 100);
            }
            String unusablePassword = UUID.randomUUID() + UUID.randomUUID().toString();
            return users.save(new User(username, email, displayName,
                    passwordEncoder.encode(unusablePassword), Role.READER));
        });
    }

    /** "jane.doe@gmail.com" gives "jane.doe", or "jane.doe4821" when that name is already taken. */
    private String freeUsername(String email) {
        String base = email.substring(0, email.indexOf('@')).replaceAll("[^A-Za-z0-9._-]", "");
        if (base.length() > 40) {
            base = base.substring(0, 40);
        }
        if (base.length() < 3) {
            base = base + "user";
        }
        String candidate = base;
        while (users.existsByUsername(candidate)) {
            candidate = base + ThreadLocalRandom.current().nextInt(1000, 10000);
        }
        return candidate;
    }

    public boolean usernameExists(String username) {
        return users.existsByUsername(username);
    }

    public boolean emailExists(String email) {
        return users.existsByEmail(email);
    }
}
