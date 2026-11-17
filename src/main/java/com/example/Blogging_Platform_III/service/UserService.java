package com.example.Blogging_Platform_III.service;

import com.example.Blogging_Platform_III.config.CacheConfig;
import com.example.Blogging_Platform_III.entity.User;
import com.example.Blogging_Platform_III.exception.BlogException;
import com.example.Blogging_Platform_III.model.UserResponse;
import com.example.Blogging_Platform_III.repository.UserRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;

    public UserService(UserRepository users) {
        this.users = users;
    }

    /** Cached: the list is read from PostgreSQL once, then from memory until a user changes. */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.USER_LIST)
    public List<UserResponse> list() {
        return users.findByActiveTrueOrderByUsernameAscIdAsc().stream().map(UserResponse::from).toList();
    }

    /** Cached by user ID. */
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = CacheConfig.USERS, key = "#id")
    public UserResponse get(long id) {
        return UserResponse.from(findActive(id));
    }

    /** A new user changes the user list, so the cached list is removed. */
    @Transactional
    @CacheEvict(cacheNames = CacheConfig.USER_LIST, allEntries = true)
    public UserResponse create(String username, String email, String displayName) {
        try {
            // saveAndFlush sends the INSERT now, so a unique-key error is caught here.
            return UserResponse.from(users.saveAndFlush(new User(username, email, displayName)));
        } catch (DataIntegrityViolationException exception) {
            throw BlogException.duplicateValue();
        }
    }

    /** Removes the cached copy of this user and the cached list. */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.USERS, key = "#id"),
            @CacheEvict(cacheNames = CacheConfig.USER_LIST, allEntries = true)})
    public UserResponse update(long id, String email, String displayName) {
        User user = findActive(id);
        if (users.existsByEmailAndIdNot(email, id)) {
            throw BlogException.conflict("This email is already registered.");
        }
        user.setEmail(email);
        user.setDisplayName(displayName);
        try {
            return UserResponse.from(users.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw BlogException.duplicateValue();
        }
    }

    @Transactional(readOnly = true)
    public boolean usernameExists(String username) {
        return users.existsByUsername(username);
    }

    @Transactional(readOnly = true)
    public boolean emailExists(String email) {
        return users.existsByEmail(email);
    }

    /**
     * A soft delete: the user is marked inactive. Hibernate sees the changed field and writes
     * the UPDATE when the transaction commits, so no save call is needed.
     */
    @Transactional
    @Caching(evict = {
            @CacheEvict(cacheNames = CacheConfig.USERS, key = "#id"),
            @CacheEvict(cacheNames = CacheConfig.USER_LIST, allEntries = true)})
    public void delete(long id) {
        findActive(id).setActive(false);
    }

    private User findActive(long id) {
        return users.findByIdAndActiveTrue(id).orElseThrow(() -> BlogException.notFound("User"));
    }
}
