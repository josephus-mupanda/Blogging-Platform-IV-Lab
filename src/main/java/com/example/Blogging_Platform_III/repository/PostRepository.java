package com.example.Blogging_Platform_III.repository;

import com.example.Blogging_Platform_III.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * JpaRepository already gives save, findById, findAll(pageable), count and delete.
 * A Pageable parameter adds ORDER BY, LIMIT and OFFSET to the query, and the result is a Page.
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    // Derived queries: Spring Data reads the method name and writes the SQL.

    /** Posts of one author: ... JOIN users u ... WHERE u.username = ? */
    Page<Post> findByAuthorUsername(String username, Pageable pageable);

    /** Posts that have one tag: ... JOIN post_tags ... JOIN tags t ... WHERE t.name = ? */
    Page<Post> findByTagsName(String tagName, Pageable pageable);

    boolean existsByAuthorId(Long authorId);

    // JPQL query: written with the entity names (Post, p.title), not the table names.

    /** Posts whose title or body contains the keyword, ignoring upper and lower case. */
    @Query("SELECT p FROM Post p "
            + "WHERE LOWER(p.title) LIKE LOWER(CONCAT('%', :keyword, '%')) "
            + "OR LOWER(p.body) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    Page<Post> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
