package com.example.Blogging_Platform_IV.service;

import com.example.Blogging_Platform_IV.entity.Tag;
import com.example.Blogging_Platform_IV.exception.BlogException;
import com.example.Blogging_Platform_IV.repository.TagRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {
    private final TagRepository tags;

    public TagService(TagRepository tags) {
        this.tags = tags;
    }

    /** Cached: the tag list is read from the database once, then from the "tags" cache. */
    @Cacheable("tags")
    public List<Tag> list() {
        return tags.findAll(Sort.by("name"));
    }

    public Tag get(Long id) {
        return tags.findById(id).orElseThrow(() -> BlogException.notFound("Tag"));
    }

    /** A new tag changes the tag list, so the cached list is removed. */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "tags", allEntries = true)
    public Tag create(String name) {
        return tags.save(new Tag(name));
    }

    /** Cached posts show tag names, so the "posts" cache is cleared too. */
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"tags", "posts"}, allEntries = true)
    public Tag update(Long id, String name) {
        Tag tag = get(id);
        tag.setName(name);
        return tags.save(tag);
    }

    // Two statements, one transaction: remove the tag from the posts (native query), then delete it.
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"tags", "posts"}, allEntries = true)
    public void delete(Long id) {
        Tag tag = get(id);
        tags.removeTagFromPosts(id);
        tags.delete(tag);
    }
}
