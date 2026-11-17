package com.example.Blogging_Platform_III.model;

import java.util.List;
import org.springframework.data.domain.Page;

/** One page of posts with the numbers a client needs to show page links. */
public class PostPage {
    private List<PostResponse> items = List.of();
    private int page;
    private int size;
    private long totalElements;
    private long totalPages;

    public PostPage() {
    }

    public PostPage(List<PostResponse> items, int page, int size, long totalElements, long totalPages) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }

    /** Copies the page numbers from the Spring Data page and uses the converted items. */
    public static PostPage of(Page<?> page, List<PostResponse> items) {
        return new PostPage(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    public List<PostResponse> getItems() {
        return items;
    }

    public void setItems(List<PostResponse> items) {
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
