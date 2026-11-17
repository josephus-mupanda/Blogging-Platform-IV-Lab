package com.example.Blogging_Platform_III.model;

import com.example.Blogging_Platform_III.entity.Post;
import com.example.Blogging_Platform_III.entity.Tag;
import java.util.Comparator;
import java.util.List;

public class PostResponse {
    private long id;
    private long authorId;
    private String title;
    private String body;
    private String createdAt;
    private String updatedAt;
    private List<TagResponse> tags = List.of();

    public PostResponse() {
    }

    public PostResponse(long id, long authorId, String title, String body, String createdAt, String updatedAt,
                        List<TagResponse> tags) {
        this.id = id;
        this.authorId = authorId;
        this.title = title;
        this.body = body;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.tags = tags;
    }

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

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(long authorId) {
        this.authorId = authorId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<TagResponse> getTags() {
        return tags;
    }

    public void setTags(List<TagResponse> tags) {
        this.tags = tags;
    }
}
