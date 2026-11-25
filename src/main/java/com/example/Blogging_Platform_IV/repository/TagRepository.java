package com.example.Blogging_Platform_IV.repository;

import com.example.Blogging_Platform_IV.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository extends JpaRepository<Tag, Long> {

    // Native SQL: post_tags is a join table without an entity, so JPQL cannot reach it.
    // @Modifying is needed for DELETE and UPDATE.
    @Modifying
    @Query(value = "DELETE FROM post_tags WHERE tag_id = :tagId", nativeQuery = true)
    void removeTagFromPosts(@Param("tagId") Long tagId);
}
