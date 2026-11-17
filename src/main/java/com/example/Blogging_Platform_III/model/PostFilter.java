package com.example.Blogging_Platform_III.model;

import lombok.Data;

/** The optional search values of GET /api/posts and the GraphQL posts query. */
@Data
public class PostFilter {
    private String keyword;
    private Long authorId;
    private Long tagId;
    private Integer page;
    private Integer size;
    private Sort sort;
}
