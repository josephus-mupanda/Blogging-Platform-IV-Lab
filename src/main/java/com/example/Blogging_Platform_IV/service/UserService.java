package com.example.Blogging_Platform_IV.service;

import com.example.Blogging_Platform_IV.entity.Comment;
import com.example.Blogging_Platform_IV.entity.Review;
import com.example.Blogging_Platform_IV.entity.User;
import com.example.Blogging_Platform_IV.enums.Role;
import com.example.Blogging_Platform_IV.exception.BlogException;
import com.example.Blogging_Platform_IV.repository.CommentRepository;
import com.example.Blogging_Platform_IV.repository.PostRepository;
import com.example.Blogging_Platform_IV.repository.ReviewRepository;
import com.example.Blogging_Platform_IV.repository.UserRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// rollbackFor = Exception.class: roll back for any exception, not only RuntimeException.
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

    // Cached: the first call reads the database, the next calls read the "users" cache.
    @Cacheable(value = "users", key = "#id")
    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> BlogException.notFound("User"));
    }

    public User getByUsername(String username) {
        return users.findByUsername(username).orElseThrow(() -> BlogException.notFound("User"));
    }

    // The password is hashed with BCrypt before it is saved. No role means READER.
    @Transactional(rollbackFor = Exception.class)
    public User create(String username, String email, String displayName, String password, Role role) {
        return users.save(new User(username, email, displayName, passwordEncoder.encode(password),
                role == null ? Role.READER : role));
    }

    // Google login: use the account with this email, or create a READER account.
    @Transactional(rollbackFor = Exception.class)
    public User findOrCreateGoogleUser(String email, String name) {
        return users.findByEmail(email).orElseGet(() -> {
            String username = email.substring(0, email.indexOf('@'));
            while (users.existsByUsername(username)) {
                username = username + "1";   // the name is already taken
            }
            String displayName = name == null ? username : name;
            // A Google user has no password here, so the account gets a random one nobody knows.
            String password = passwordEncoder.encode(UUID.randomUUID().toString());
            return users.save(new User(username, email, displayName, password, Role.READER));
        });
    }

    // @CachePut replaces the user in the cache. @CacheEvict empties the posts cache, because a cached
    // post contains its author. The name copied in the MongoDB comments and reviews is updated too.
    // saveAndFlush runs the UPDATE first, so if it fails MongoDB is not touched.
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

    // A user with posts cannot be deleted (409). PostgreSQL first (flush runs the DELETE now),
    // then the comments and reviews in MongoDB.
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
