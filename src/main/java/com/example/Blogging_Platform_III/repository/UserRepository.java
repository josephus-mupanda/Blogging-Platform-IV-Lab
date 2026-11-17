package com.example.Blogging_Platform_III.repository;

import com.example.Blogging_Platform_III.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Users in PostgreSQL. JpaRepository already gives save, findById, findAll, count and delete.
 * The methods below are derived queries: Spring Data reads the method name and writes the SQL.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** WHERE user_id = ? AND active = true */
    Optional<User> findByIdAndActiveTrue(Long id);

    /** WHERE username = ? AND active = true */
    Optional<User> findByUsernameAndActiveTrue(String username);

    /** WHERE active = true ORDER BY username, user_id */
    List<User> findByActiveTrueOrderByUsernameAscIdAsc();

    /** Checks every user, deleted ones too, because a deleted username stays reserved. */
    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    /** Used when a user changes their email: is it used by somebody else? */
    boolean existsByEmailAndIdNot(String email, Long id);
}
