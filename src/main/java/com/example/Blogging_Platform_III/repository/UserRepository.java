package com.example.Blogging_Platform_III.repository;

import com.example.Blogging_Platform_III.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * JpaRepository already gives save, findById, findAll, count and delete.
 * The two methods below are derived queries: Spring Data writes the SQL from the method name.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
