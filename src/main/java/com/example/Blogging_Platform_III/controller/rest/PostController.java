package com.example.Blogging_Platform_III.controller.rest;

import static com.example.Blogging_Platform_III.controller.rest.ApiResponse.ok;

import com.example.Blogging_Platform_III.dto.PostInput;
import com.example.Blogging_Platform_III.model.PostFilter;
import com.example.Blogging_Platform_III.model.PostPage;
import com.example.Blogging_Platform_III.model.PostResponse;
import com.example.Blogging_Platform_III.model.Sort;
import com.example.Blogging_Platform_III.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
public class PostController {
    private final PostService posts;

    public PostController(PostService posts) {
        this.posts = posts;
    }

    @GetMapping
    @Operation(summary = "Search posts",
            description = "Optional keyword, authorId and tagId filters, with page, size and sort. "
                    + "Runs the JPQL query PostRepository.search.")
    ApiResponse<PostPage> list(@ParameterObject @ModelAttribute PostFilter filter) {
        return ok("Posts found.", posts.search(filter));
    }

    @GetMapping("/author/{username}")
    @Operation(summary = "Posts of one author",
            description = "Runs the derived query PostRepository.findByAuthorUsernameAndDeletedAtIsNull.")
    ApiResponse<PostPage> byAuthor(@PathVariable String username,
                                   @RequestParam(defaultValue = "0") Integer page,
                                   @RequestParam(defaultValue = "10") Integer size,
                                   @RequestParam(defaultValue = "NEWEST") Sort sort) {
        return ok("Posts found.", posts.byAuthor(username, page, size, sort));
    }

    @GetMapping("/tag/{name}")
    @Operation(summary = "Posts with one tag",
            description = "Runs the derived query PostRepository.findByTagsNameAndDeletedAtIsNull.")
    ApiResponse<PostPage> byTag(@PathVariable String name,
                                @RequestParam(defaultValue = "0") Integer page,
                                @RequestParam(defaultValue = "10") Integer size,
                                @RequestParam(defaultValue = "NEWEST") Sort sort) {
        return ok("Posts found.", posts.byTag(name, page, size, sort));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Read one post", description = "Cached: repeated reads come from the 'posts' cache.")
    ApiResponse<PostResponse> get(@PathVariable long id) {
        return ok("Post found.", posts.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<PostResponse> create(@RequestParam long authorId, @Valid @RequestBody PostInput input) {
        return ok("Post created.", posts.create(authorId, input.getTitle(), input.getBody(), input.getTagIds()));
    }

    @PutMapping("/{id}")
    ApiResponse<PostResponse> update(@PathVariable long id, @Valid @RequestBody PostInput input) {
        return ok("Post updated.", posts.update(id, input.getTitle(), input.getBody(), input.getTagIds()));
    }

    @DeleteMapping("/{id}")
    ApiResponse<Void> archive(@PathVariable long id) {
        posts.archive(id);
        return ok("Post deleted.", null);
    }
}
