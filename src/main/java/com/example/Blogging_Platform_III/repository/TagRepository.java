package com.example.Blogging_Platform_III.repository;

import com.example.Blogging_Platform_III.entity.Tag;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** Tags in PostgreSQL. All methods are derived queries. "Visible" means deleted_at IS NULL. */
public interface TagRepository extends JpaRepository<Tag, Long> {

    /** WHERE deleted_at IS NULL ORDER BY name, tag_id */
    List<Tag> findByDeletedAtIsNullOrderByNameAscIdAsc();

    Optional<Tag> findByIdAndDeletedAtIsNull(Long id);

    /** Uses the unique index on tags.name. */
    Optional<Tag> findByNameAndDeletedAtIsNull(String name);

    /** WHERE tag_id IN (...) AND deleted_at IS NULL. Loads all the tags of a post in one query. */
    List<Tag> findByIdInAndDeletedAtIsNull(Collection<Long> ids);
}
