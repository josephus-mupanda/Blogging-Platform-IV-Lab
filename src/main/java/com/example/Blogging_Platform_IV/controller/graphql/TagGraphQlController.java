package com.example.Blogging_Platform_IV.controller.graphql;

import org.springframework.security.access.prepost.PreAuthorize;
import com.example.Blogging_Platform_IV.dto.TagInput;
import com.example.Blogging_Platform_IV.entity.Tag;
import com.example.Blogging_Platform_IV.service.TagService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class TagGraphQlController {
    private final TagService tags;

    public TagGraphQlController(TagService tags) {
        this.tags = tags;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AUTHOR', 'READER')")
    @QueryMapping
    public List<Tag> tags() {
        return tags.list();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AUTHOR', 'READER')")
    @QueryMapping
    public Tag tag(@Argument Long id) {
        return tags.get(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AUTHOR')")
    @MutationMapping
    public Tag createTag(@Argument @Valid TagInput input) {
        return tags.create(input.getName());
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AUTHOR')")
    @MutationMapping
    public Tag updateTag(@Argument Long id, @Argument @Valid TagInput input) {
        return tags.update(id, input.getName());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @MutationMapping
    public boolean deleteTag(@Argument Long id) {
        tags.delete(id);
        return true;
    }
}
