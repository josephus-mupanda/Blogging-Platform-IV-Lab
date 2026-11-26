package com.example.Blogging_Platform_IV.service;

import com.example.Blogging_Platform_IV.repository.CommentRepository;
import com.example.Blogging_Platform_IV.repository.PostRepository;
import com.example.Blogging_Platform_IV.repository.UserRepository;
import org.springframework.stereotype.Component;

// Used inside @PreAuthorize, for example: @ownership.isPostOwner(#id, authentication.name)
// It checks that a post, a comment or a user id belongs to the logged-in user (the username in the token).
// If the thing does not exist, it returns true so the service can answer 404 instead of 403.
@Component("ownership")
public class OwnershipChecker {
    private final PostRepository posts;
    private final CommentRepository comments;
    private final UserRepository users;

    public OwnershipChecker(PostRepository posts, CommentRepository comments, UserRepository users) {
        this.posts = posts;
        this.comments = comments;
        this.users = users;
    }

    public boolean isPostOwner(Long postId, String username) {
        return posts.findById(postId)
                .map(post -> post.getAuthor().getUsername().equals(username))
                .orElse(true);
    }

    public boolean isCommentOwner(String commentId, String username) {
        return comments.findById(commentId)
                .map(comment -> isUser(comment.getAuthorId(), username))
                .orElse(true);
    }

    // Is this user id the logged-in user? Used to stop someone from creating something in another user's name.
    public boolean isUser(Long userId, String username) {
        return users.findById(userId)
                .map(user -> user.getUsername().equals(username))
                .orElse(true);
    }
}
