package com.example.Blogging_Platform_III.model;

import com.example.Blogging_Platform_III.entity.Comment;
import java.util.List;
import org.springframework.data.domain.Page;

/** One page of comments, in the same shape as PostPage. */
public class CommentPage {
    private List<Comment> items = List.of();
    private int page;
    private int size;
    private long totalElements;
    private long totalPages;

    public CommentPage() {
    }

    public CommentPage(Page<Comment> page) {
        this.items = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
    }

    public List<Comment> getItems() {
        return items;
    }

    public void setItems(List<Comment> items) {
        this.items = items;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public long getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(long totalPages) {
        this.totalPages = totalPages;
    }
}
