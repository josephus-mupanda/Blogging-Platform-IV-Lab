package com.example.Blogging_Platform_III.repository;

/**
 * The result row of PostRepository.countVisibleRecords().
 * Spring Data matches each getter to the column with the same alias in the native query.
 */
public interface DashboardCounts {
    long getUsers();

    long getPosts();

    long getTags();
}
