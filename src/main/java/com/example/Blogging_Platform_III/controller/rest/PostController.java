package com.example.Blogging_Platform_III.controller.rest;

import static com.example.Blogging_Platform_III.controller.rest.ApiResponse.ok;

import com.example.Blogging_Platform_III.dto.PostInput;
import com.example.Blogging_Platform_III.entity.Post;
import com.example.Blogging_Platform_III.service.PostService;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pageable is filled by Spring from ?page=0&size=10&sort=createdAt,desc.
 * @PageableDefault gives the values used when the client sends none: 10 newest posts.
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService posts;

    public PostController(PostService posts) {
        this.posts = posts;
    }

    @GetMapping
    ApiResponse<Page<Post>> list(@RequestParam(required = false) String keyword,
                                 @ParameterObject @PageableDefault(size = 10, sort = "createdAt",
                                         direction = Sort.Direction.DESC) Pageable pageable) {
        return ok("Posts found.", posts.list(keyword, pageable));
    }

    @GetMapping("/author/{username}")
    ApiResponse<Page<Post>> byAuthor(@PathVariable String username,
                                     @ParameterObject @PageableDefault(size = 10, sort = "createdAt",
                                             direction = Sort.Direction.DESC) Pageable pageable) {
        return ok("Posts found.", posts.byAuthor(username, pageable));
    }

    @GetMapping("/tag/{name}")
    ApiResponse<Page<Post>> byTag(@PathVariable String name,
                                  @ParameterObject @PageableDefault(size = 10, sort = "createdAt",
                                          direction = Sort.Direction.DESC) Pageable pageable) {
        return ok("Posts found.", posts.byTag(name, pageable));
    }

    @GetMapping("/{id}")
    ApiResponse<Post> get(@PathVariable Long id) {
        return ok("Post found.", posts.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<Post> create(@RequestParam Long authorId, @Valid @RequestBody PostInput input) {
        return ok("Post created.", posts.create(authorId, input.getTitle(), input.getBody(), input.getTagIds()));
    }

    @PutMapping("/{id}")
    ApiResponse<Post> update(@PathVariable Long id, @Valid @RequestBody PostInput input) {
        return ok("Post updated.", posts.update(id, input.getTitle(), input.getBody(), input.getTagIds()));
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> delete(@PathVariable Long id) {
        posts.delete(id);
        return ok("Post deleted.", null);
    }
}
