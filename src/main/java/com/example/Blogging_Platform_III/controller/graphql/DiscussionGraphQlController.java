package com.example.Blogging_Platform_III.controller.graphql;

import com.example.Blogging_Platform_III.dto.CommentInput;
import com.example.Blogging_Platform_III.dto.ReviewInput;
import com.example.Blogging_Platform_III.entity.Comment;
import com.example.Blogging_Platform_III.entity.Review;
import com.example.Blogging_Platform_III.model.CommentPage;
import com.example.Blogging_Platform_III.model.Discussion;
import com.example.Blogging_Platform_III.service.DiscussionService;
import jakarta.validation.Valid;
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

    @QueryMapping
    public CommentPage comments(@Argument long postId, @Argument Integer page, @Argument Integer size) {
        return discussion.comments(postId, page, size);
    }

    @QueryMapping
    public Comment comment(@Argument long postId, @Argument String id) {
        return discussion.comment(postId, id);
    }

    @QueryMapping
    public Discussion reviews(@Argument long postId) {
        return discussion.reviews(postId);
    }

    @MutationMapping
    public Comment addComment(@Argument long postId, @Argument long authorId, @Argument @Valid CommentInput input) {
        return discussion.addComment(postId, authorId, input.getBody());
    }

    @MutationMapping
    public Comment updateComment(@Argument long postId, @Argument String id, @Argument @Valid CommentInput input) {
        return discussion.editComment(postId, id, input.getBody());
    }

    @MutationMapping
    public boolean deleteComment(@Argument long postId, @Argument String id) {
        discussion.archiveComment(postId, id);
        return true;
    }

    @MutationMapping
    public Review addReview(@Argument long postId, @Argument long reviewerId, @Argument @Valid ReviewInput input) {
        return discussion.addReview(postId, reviewerId, input.getRating(), input.getText());
    }
}
