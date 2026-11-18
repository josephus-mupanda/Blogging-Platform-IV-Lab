package com.example.Blogging_Platform_III.service;

import com.example.Blogging_Platform_III.entity.User;
import com.example.Blogging_Platform_III.exception.BlogException;
import com.example.Blogging_Platform_III.repository.PostRepository;
import com.example.Blogging_Platform_III.repository.UserRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final PostRepository posts;

    public UserService(UserRepository users, PostRepository posts) {
        this.users = users;
        this.posts = posts;
    }

    public List<User> list() {
        return users.findAll(Sort.by("username"));
    }

    /** Cached: the first call reads the database, the next calls read the "users" cache. */
    @Cacheable(value = "users", key = "#id")
    public User get(Long id) {
        return users.findById(id).orElseThrow(() -> BlogException.notFound("User"));
    }

    @Transactional
    public User create(String username, String email, String displayName) {
        return users.save(new User(username, email, displayName));
    }

    /**
     * @CachePut always runs the method, then puts the returned user in the "users" cache.
     * So the cache holds the new values and the next read still comes from the cache.
     * saveAndFlush sends the UPDATE now: if it fails (email already used), the method throws
     * and nothing is put in the cache.
     */
    @Transactional
    @CachePut(value = "users", key = "#id")
    public User update(Long id, String email, String displayName) {
        User user = users.findById(id).orElseThrow(() -> BlogException.notFound("User"));
        user.setEmail(email);
        user.setDisplayName(displayName);
        return users.saveAndFlush(user);
    }

    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void delete(Long id) {
        User user = users.findById(id).orElseThrow(() -> BlogException.notFound("User"));
        if (posts.existsByAuthorId(id)) {
            throw BlogException.conflict("This user still has posts. Delete the posts first.");
        }
        users.delete(user);
    }

    public boolean usernameExists(String username) {
        return users.existsByUsername(username);
    }

    public boolean emailExists(String email) {
        return users.existsByEmail(email);
    }
}
