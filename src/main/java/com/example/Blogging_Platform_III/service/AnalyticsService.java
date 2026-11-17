package com.example.Blogging_Platform_III.service;

import com.example.Blogging_Platform_III.model.Dashboard;
import com.example.Blogging_Platform_III.repository.CommentRepository;
import com.example.Blogging_Platform_III.repository.DashboardCounts;
import com.example.Blogging_Platform_III.repository.PostRepository;
import com.example.Blogging_Platform_III.repository.ReviewRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnalyticsService {
    private final PostRepository posts;
    private final CommentRepository comments;
    private final ReviewRepository reviews;

    public AnalyticsService(PostRepository posts, CommentRepository comments, ReviewRepository reviews) {
        this.posts = posts;
        this.comments = comments;
        this.reviews = reviews;
    }

    /**
     * Two native SQL queries give the PostgreSQL numbers. MongoDB then counts the comments and
     * reviews, leaving out those that belong to deleted posts.
     */
    @Transactional(readOnly = true)
    public Dashboard dashboard() {
        DashboardCounts counts = posts.countVisibleRecords();
        List<Long> archived = posts.findArchivedIds();
        return new Dashboard(counts.getUsers(), counts.getPosts(), counts.getTags(),
                comments.countByPostIdNotInAndDeletedAtIsNull(archived), reviews.countByPostIdNotIn(archived));
    }
}
