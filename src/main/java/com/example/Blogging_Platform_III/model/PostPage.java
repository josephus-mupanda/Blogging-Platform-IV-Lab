package com.example.Blogging_Platform_III.model;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Page;

/** One page of posts with the numbers a client needs to show page links. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostPage {
    private List<PostResponse> items = List.of();
    private int page;
    private int size;
    private long totalElements;
    private long totalPages;

    /** Copies the page numbers from the Spring Data page and uses the converted items. */
    public static PostPage of(Page<?> page, List<PostResponse> items) {
        return new PostPage(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
