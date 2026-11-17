package com.example.Blogging_Platform_III.repository;

import com.example.Blogging_Platform_III.entity.Post;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Posts in PostgreSQL. "Visible" means deleted_at IS NULL.
 * Methods that take a Pageable return one page: Spring Data adds ORDER BY, LIMIT and OFFSET
 * to the SQL and runs a second COUNT query for the total number of rows.
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    // ----- Derived queries -------------------------------------------------------------

    Optional<Post> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByIdAndDeletedAtIsNull(Long id);

    /** Posts written by one author, found by username: JOIN users ... WHERE u.username = ? */
    Page<Post> findByAuthorUsernameAndDeletedAtIsNull(String username, Pageable pageable);

    /** Posts that carry one tag, found by tag name: JOIN post_tags, tags ... WHERE t.name = ? */
    Page<Post> findByTagsNameAndDeletedAtIsNull(String tagName, Pageable pageable);

    // ----- JPQL queries ----------------------------------------------------------------

    /**
     * The post search. Every filter is optional: when a parameter is null, its condition is
     * true for every row, so the filter is switched off.
     * The pattern is already lowercase and wrapped in % signs by PostService. The lower(...)
     * expressions match the trigram indexes idx_posts_title_lower_trgm and idx_posts_body_lower_trgm.
     */
    @Query("""
            SELECT p FROM Post p
            WHERE p.deletedAt IS NULL
              AND (:pattern IS NULL
                   OR lower(p.title) LIKE :pattern ESCAPE '!'
                   OR lower(p.body) LIKE :pattern ESCAPE '!')
              AND (:authorId IS NULL OR p.author.id = :authorId)
              AND (:tagId IS NULL OR EXISTS (
                   SELECT t FROM Tag t
                   WHERE t.id = :tagId AND t.deletedAt IS NULL AND t MEMBER OF p.tags))
            """)
    Page<Post> search(@Param("pattern") String pattern, @Param("authorId") Long authorId,
                      @Param("tagId") Long tagId, Pageable pageable);

    /** One visible post with its tags. JOIN FETCH reads the post and its tags in one query. */
    @Query("SELECT p FROM Post p LEFT JOIN FETCH p.tags WHERE p.id = :id AND p.deletedAt IS NULL")
    Optional<Post> findVisibleWithTags(@Param("id") Long id);

    /**
     * Loads the tags of many posts in one query (JOIN FETCH). Without it, reading the tags of a
     * page of 10 posts costs 10 extra queries, one per post (the "N+1 queries" problem).
     */
    @Query("SELECT DISTINCT p FROM Post p LEFT JOIN FETCH p.tags WHERE p.id IN :ids")
    List<Post> findWithTags(@Param("ids") Collection<Long> ids);

    // ----- Native SQL queries ----------------------------------------------------------

    /** IDs of deleted posts. Uses the partial index idx_posts_archived. */
    @Query(value = "SELECT post_id FROM posts WHERE deleted_at IS NOT NULL", nativeQuery = true)
    List<Long> findArchivedIds();

    /** The three PostgreSQL dashboard counts in one round trip instead of three. */
    @Query(value = """
            SELECT (SELECT count(*) FROM users WHERE active) AS users,
                   (SELECT count(*) FROM posts WHERE deleted_at IS NULL) AS posts,
                   (SELECT count(*) FROM tags WHERE deleted_at IS NULL) AS tags
            """, nativeQuery = true)
    DashboardCounts countVisibleRecords();
}
