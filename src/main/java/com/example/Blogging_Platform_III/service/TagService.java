package com.example.Blogging_Platform_III.service;

import com.example.Blogging_Platform_III.entity.Tag;
import com.example.Blogging_Platform_III.exception.BlogException;
import com.example.Blogging_Platform_III.model.TagResponse;
import com.example.Blogging_Platform_III.repository.TagRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TagService {
    private final TagRepository tags;

    public TagService(TagRepository tags) {
        this.tags = tags;
    }

    @Transactional(readOnly = true)
    public List<TagResponse> list() {
        return tags.findByDeletedAtIsNullOrderByNameAscIdAsc().stream().map(TagResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TagResponse get(long id) {
        return TagResponse.from(findVisible(id));
    }

    /**
     * REQUIRED (the default, written out here): if the caller already has a transaction, this
     * method joins it; otherwise it starts a new one. When it joins, the new tag is committed
     * or rolled back together with everything else the caller does.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public TagResponse create(String name) {
        try {
            return TagResponse.from(tags.saveAndFlush(new Tag(name)));
        } catch (DataIntegrityViolationException exception) {
            throw BlogException.duplicateValue();
        }
    }

    @Transactional
    public TagResponse update(long id, String name) {
        Tag tag = findVisible(id);
        tag.setName(name);
        try {
            return TagResponse.from(tags.saveAndFlush(tag));
        } catch (DataIntegrityViolationException exception) {
            throw BlogException.duplicateValue();
        }
    }

    /** A soft delete. The tag disappears from every post. */
    @Transactional
    public void archive(long id) {
        findVisible(id).setDeletedAt(Instant.now());
    }

    private Tag findVisible(long id) {
        return tags.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> BlogException.notFound("Tag"));
    }
}
