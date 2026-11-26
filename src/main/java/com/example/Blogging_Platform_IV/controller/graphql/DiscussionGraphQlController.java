package com.example.Blogging_Platform_IV.controller.graphql;

import org.springframework.security.access.prepost.PreAuthorize;
import com.example.Blogging_Platform_IV.dto.CommentInput;
import com.example.Blogging_Platform_IV.dto.ReviewInput;
import com.example.Blogging_Platform_IV.entity.Comment;
import com.example.Blogging_Platform_IV.entity.Review;
import com.example.Blogging_Platform_IV.service.DiscussionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.MutationMapping;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
public class DiscussionGraphQlController {
    private final DiscussionService discussion;

    public DiscussionGraphQlController(DiscussionService discussion) {
        this.discussion = discussion;
    }

    /** One page of comments, oldest first. */
    @PreAuthorize("hasAnyRole('ADMIN', 'AUTHOR', 'READER')")
    @QueryMapping
    public Page<Comment> comments(@Argument Long postId, @Argument int page, @Argument int size) {
        return discussion.comments(postId, PageRequest.of(page, size, Sort.by("createdAt")));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AUTHOR', 'READER')")
    @QueryMapping
    public Comment comment(@Argument Long postId, @Argument String id) {
        return discussion.comment(postId, id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AUTHOR', 'READER')")
    @QueryMapping
    public List<Review> reviews(@Argument Long postId) {
        return discussion.reviews(postId);
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.isUser(#authorId, authentication.name)")
    @MutationMapping
    public Comment addComment(@Argument Long postId, @Argument Long authorId, @Argument @Valid CommentInput input) {
        return discussion.addComment(postId, authorId, input.getBody());
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.isCommentOwner(#id, authentication.name)")
    @MutationMapping
    public Comment updateComment(@Argument Long postId, @Argument String id, @Argument @Valid CommentInput input) {
        return discussion.editComment(postId, id, input.getBody());
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.isCommentOwner(#id, authentication.name)")
    @MutationMapping
    public boolean deleteComment(@Argument Long postId, @Argument String id) {
        discussion.deleteComment(postId, id);
        return true;
    }

    @PreAuthorize("hasRole('ADMIN') or @ownership.isUser(#reviewerId, authentication.name)")
    @MutationMapping
    public Review addReview(@Argument Long postId, @Argument Long reviewerId, @Argument @Valid ReviewInput input) {
        return discussion.addReview(postId, reviewerId, input.getRating(), input.getText());
    }
}
