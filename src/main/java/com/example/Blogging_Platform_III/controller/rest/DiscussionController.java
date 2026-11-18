package com.example.Blogging_Platform_III.controller.rest;

import static com.example.Blogging_Platform_III.controller.rest.ApiResponse.ok;

import com.example.Blogging_Platform_III.dto.CommentInput;
import com.example.Blogging_Platform_III.dto.ReviewInput;
import com.example.Blogging_Platform_III.entity.Comment;
import com.example.Blogging_Platform_III.entity.Review;
import com.example.Blogging_Platform_III.service.DiscussionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

@RestController
@RequestMapping("/api/posts/{postId}")
public class DiscussionController {
    private final DiscussionService discussion;

    public DiscussionController(DiscussionService discussion) {
        this.discussion = discussion;
    }

    /** One page of comments, oldest first by default. */
    @GetMapping("/comments")
    ApiResponse<Page<Comment>> comments(@PathVariable Long postId,
                                        @ParameterObject @PageableDefault(size = 10, sort = "createdAt")
                                        Pageable pageable) {
        return ok("Comments found.", discussion.comments(postId, pageable));
    }

    @GetMapping("/comments/{id}")
    ApiResponse<Comment> comment(@PathVariable Long postId, @PathVariable String id) {
        return ok("Comment found.", discussion.comment(postId, id));
    }

    @PostMapping("/comments")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<Comment> createComment(@PathVariable Long postId, @RequestParam Long authorId,
                                       @Valid @RequestBody CommentInput input) {
        return ok("Comment created.", discussion.addComment(postId, authorId, input.getBody()));
    }

    @PutMapping("/comments/{id}")
    ApiResponse<Comment> updateComment(@PathVariable Long postId, @PathVariable String id,
                                       @Valid @RequestBody CommentInput input) {
        return ok("Comment updated.", discussion.editComment(postId, id, input.getBody()));
    }

    @DeleteMapping("/comments/{id}")
    ApiResponse<Void> deleteComment(@PathVariable Long postId, @PathVariable String id) {
        discussion.deleteComment(postId, id);
        return ok("Comment deleted.", null);
    }

    @GetMapping("/reviews")
    ApiResponse<List<Review>> reviews(@PathVariable Long postId) {
        return ok("Reviews found.", discussion.reviews(postId));
    }

    @PostMapping("/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<Review> createReview(@PathVariable Long postId, @RequestParam Long reviewerId,
                                     @Valid @RequestBody ReviewInput input) {
        return ok("Review created.", discussion.addReview(postId, reviewerId, input.getRating(), input.getText()));
    }
}
