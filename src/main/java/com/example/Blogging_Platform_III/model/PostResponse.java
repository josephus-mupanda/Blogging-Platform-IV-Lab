package com.example.Blogging_Platform_III.model;

import com.example.Blogging_Platform_III.entity.Post;
import com.example.Blogging_Platform_III.entity.Tag;
import java.util.Comparator;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostResponse {
    private long id;
    private long authorId;
    private String title;
    private String body;
    private String createdAt;
    private String updatedAt;
    private List<TagResponse> tags = List.of();

    /**
     * Copies a post entity into the response. Deleted tags are left out and the others are
     * sorted by name. Reading the author ID does not load the user row.
     */
    public static PostResponse from(Post post) {
        List<TagResponse> visibleTags = post.getTags().stream()
                .filter(tag -> tag.getDeletedAt() == null)
                .sorted(Comparator.comparing(Tag::getName).thenComparing(Tag::getId))
                .map(TagResponse::from)
                .toList();
        return new PostResponse(post.getId(), post.getAuthor().getId(), post.getTitle(), post.getBody(),
                post.getCreatedAt().toString(), post.getUpdatedAt().toString(), visibleTags);
    }
}
