package com.example.Blogging_Platform_IV.controller.graphql;

import com.example.Blogging_Platform_IV.dto.PostInput;
import com.example.Blogging_Platform_IV.entity.Post;
import com.example.Blogging_Platform_IV.service.PostService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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

    /** One page of posts, newest first. page and size have default values in the schema. */
    @QueryMapping
    public Page<Post> posts(@Argument String keyword, @Argument int page, @Argument int size) {
        return posts.list(keyword, PageRequest.of(page, size, Sort.by("createdAt").descending()));
    }

    @QueryMapping
    public Post post(@Argument Long id) {
        return posts.get(id);
    }

    @MutationMapping
    public Post createPost(@Argument Long authorId, @Argument @Valid PostInput input) {
        return posts.create(authorId, input.getTitle(), input.getBody(), input.getTagIds());
    }

    @MutationMapping
    public Post updatePost(@Argument Long id, @Argument @Valid PostInput input) {
        return posts.update(id, input.getTitle(), input.getBody(), input.getTagIds());
    }

    @MutationMapping
    public boolean deletePost(@Argument Long id) {
        posts.delete(id);
        return true;
    }
}
