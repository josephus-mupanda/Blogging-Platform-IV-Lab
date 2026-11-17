package com.example.Blogging_Platform_III.controller.graphql;

import com.example.Blogging_Platform_III.dto.TagInput;
import com.example.Blogging_Platform_III.model.TagResponse;
import com.example.Blogging_Platform_III.service.TagService;
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

    @QueryMapping
    public List<TagResponse> tags() {
        return tags.list();
    }

    @QueryMapping
    public TagResponse tag(@Argument long id) {
        return tags.get(id);
    }

    @MutationMapping
    public TagResponse createTag(@Argument @Valid TagInput input) {
        return tags.create(input.getName());
    }

    @MutationMapping
    public TagResponse updateTag(@Argument long id, @Argument @Valid TagInput input) {
        return tags.update(id, input.getName());
    }

    @MutationMapping
    public boolean deleteTag(@Argument long id) {
        tags.archive(id);
        return true;
    }
}
