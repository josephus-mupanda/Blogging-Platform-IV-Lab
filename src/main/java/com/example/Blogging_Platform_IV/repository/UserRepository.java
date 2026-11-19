package com.example.Blogging_Platform_IV.repository;

import com.example.Blogging_Platform_IV.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JpaRepository already gives save, findById, findAll, count and delete.
 * The methods below are derived queries: Spring Data writes the SQL from the method name.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
}
