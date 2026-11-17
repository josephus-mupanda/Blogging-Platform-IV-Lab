package com.example.Blogging_Platform_III.model;

import com.example.Blogging_Platform_III.entity.Comment;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/** One page of comments, in the same shape as PostPage. */
@Data
@NoArgsConstructor
public class CommentPage {
    private List<Comment> items = List.of();
    private int page;
    private int size;
    private long totalElements;
    private long totalPages;

    public CommentPage(Page<Comment> page) {
        this.items = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
    }
}
