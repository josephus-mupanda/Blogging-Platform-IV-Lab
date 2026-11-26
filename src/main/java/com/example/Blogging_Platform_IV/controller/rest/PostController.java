package com.example.Blogging_Platform_IV.controller.rest;

import org.springframework.security.access.prepost.PreAuthorize;
import static com.example.Blogging_Platform_IV.controller.rest.ApiResponse.ok;

import com.example.Blogging_Platform_IV.dto.PostInput;
import com.example.Blogging_Platform_IV.entity.Post;
import com.example.Blogging_Platform_IV.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
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

    @PreAuthorize("hasAnyRole('ADMIN', 'AUTHOR', 'READER')")
    @GetMapping
    @Operation(summary = "List posts, paginated and sorted",
            description = "Without keyword: findAll(pageable). With keyword: the JPQL query PostRepository.searchByKeyword.")
    public ApiResponse<Page<Post>> list(@RequestParam(required = false) String keyword,
                                 @ParameterObject @PageableDefault(size = 10, sort = "createdAt",
                                         direction = Sort.Direction.DESC) Pageable pageable) {
        return ok("Posts found.", posts.list(keyword, pageable));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AUTHOR', 'READER')")
    @GetMapping("/author/{username}")
    @Operation(summary = "Posts of one author", description = "Derived query PostRepository.findByAuthorUsername.")
    public ApiResponse<Page<Post>> byAuthor(@PathVariable String username,
                                     @ParameterObject @PageableDefault(size = 10, sort = "createdAt",
                                             direction = Sort.Direction.DESC) Pageable pageable) {
        return ok("Posts found.", posts.byAuthor(username, pageable));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AUTHOR', 'READER')")
    @GetMapping("/tag/{name}")
    @Operation(summary = "Posts with one tag", description = "Derived query PostRepository.findByTagsName.")
    public ApiResponse<Page<Post>> byTag(@PathVariable String name,
                                  @ParameterObject @PageableDefault(size = 10, sort = "createdAt",
                                          direction = Sort.Direction.DESC) Pageable pageable) {
        return ok("Posts found.", posts.byTag(name, pageable));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AUTHOR', 'READER')")
    @GetMapping("/{id}")
    @Operation(summary = "Read one post", description = "Cached: the second read comes from the posts cache.")
    public ApiResponse<Post> get(@PathVariable Long id) {
        return ok("Post found.", posts.get(id));
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('AUTHOR') and @ownership.isUser(#authorId, authentication.name))")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Post> create(@RequestParam Long authorId, @Valid @RequestBody PostInput input) {
        return ok("Post created.", posts.create(authorId, input.getTitle(), input.getBody(), input.getTagIds()));
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('AUTHOR') and @ownership.isPostOwner(#id, authentication.name))")
    @PutMapping("/{id}")
    public ApiResponse<Post> update(@PathVariable Long id, @Valid @RequestBody PostInput input) {
        return ok("Post updated.", posts.update(id, input.getTitle(), input.getBody(), input.getTagIds()));
    }

    @PreAuthorize("hasRole('ADMIN') or (hasRole('AUTHOR') and @ownership.isPostOwner(#id, authentication.name))")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        posts.delete(id);
        return ok("Post deleted.", null);
    }
}
