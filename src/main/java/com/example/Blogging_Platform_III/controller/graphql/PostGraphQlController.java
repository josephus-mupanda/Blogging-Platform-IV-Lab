package com.example.Blogging_Platform_III.controller.graphql;

import com.example.Blogging_Platform_III.dto.PostInput;
import com.example.Blogging_Platform_III.model.PostFilter;
import com.example.Blogging_Platform_III.model.PostPage;
import com.example.Blogging_Platform_III.model.PostResponse;
import com.example.Blogging_Platform_III.service.PostService;
import jakarta.validation.Valid;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class PostGraphQlController {
    private final PostService posts;

    public PostGraphQlController(PostService posts) {
        this.posts = posts;
    }

    @QueryMapping
    public PostPage posts(@Argument PostFilter filter) {
        return posts.search(filter);
    }

    @QueryMapping
    public PostResponse post(@Argument long id) {
        return posts.get(id);
    }

    @MutationMapping
    public PostResponse createPost(@Argument long authorId, @Argument @Valid PostInput input) {
        return posts.create(authorId, input.getTitle(), input.getBody(), input.getTagIds());
    }

    @MutationMapping
    public PostResponse updatePost(@Argument long id, @Argument @Valid PostInput input) {
        return posts.update(id, input.getTitle(), input.getBody(), input.getTagIds());
    }

    @MutationMapping
    public boolean deletePost(@Argument long id) {
        posts.archive(id);
        return true;
    }
}
