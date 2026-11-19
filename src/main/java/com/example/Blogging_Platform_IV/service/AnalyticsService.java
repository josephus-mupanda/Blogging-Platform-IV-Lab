package com.example.Blogging_Platform_IV.service;

import com.example.Blogging_Platform_IV.repository.CommentRepository;
import com.example.Blogging_Platform_IV.repository.PostRepository;
import com.example.Blogging_Platform_IV.repository.ReviewRepository;
import com.example.Blogging_Platform_IV.repository.TagRepository;
import com.example.Blogging_Platform_IV.repository.UserRepository;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsService {
    private final UserRepository users;
    private final PostRepository posts;
    private final TagRepository tags;
    private final CommentRepository comments;
    private final ReviewRepository reviews;

    public AnalyticsService(UserRepository users, PostRepository posts, TagRepository tags,
                            CommentRepository comments, ReviewRepository reviews) {
        this.users = users;
        this.posts = posts;
        this.tags = tags;
        this.comments = comments;
        this.reviews = reviews;
    }

    /** How many records each repository holds (count() comes with every repository). */
    public Map<String, Long> dashboard() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("users", users.count());
        counts.put("posts", posts.count());
        counts.put("tags", tags.count());
        counts.put("comments", comments.count());
        counts.put("reviews", reviews.count());
        return counts;
    }
}
