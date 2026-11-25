package com.example.Blogging_Platform_IV.service;

import com.example.Blogging_Platform_IV.entity.Post;
import com.example.Blogging_Platform_IV.entity.Tag;
import com.example.Blogging_Platform_IV.entity.User;
import com.example.Blogging_Platform_IV.exception.BlogException;
import com.example.Blogging_Platform_IV.repository.CommentRepository;
import com.example.Blogging_Platform_IV.repository.PostRepository;
import com.example.Blogging_Platform_IV.repository.ReviewRepository;
import com.example.Blogging_Platform_IV.repository.TagRepository;
import com.example.Blogging_Platform_IV.repository.UserRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    private final PostRepository posts;
    private final UserRepository users;
    private final TagRepository tags;
    private final CommentRepository comments;
    private final ReviewRepository reviews;

    public PostService(PostRepository posts, UserRepository users, TagRepository tags,
                       CommentRepository comments, ReviewRepository reviews) {
        this.posts = posts;
        this.users = users;
        this.tags = tags;
        this.comments = comments;
        this.reviews = reviews;
    }

    /** One page of posts. With a keyword, only the posts that contain it (JPQL query). */
    public Page<Post> list(String keyword, Pageable pageable) {
        if (keyword == null || keyword.isBlank()) {
            return posts.findAll(pageable);
        }
        return posts.searchByKeyword(keyword.trim(), pageable);
    }

    public Page<Post> byAuthor(String username, Pageable pageable) {
        return posts.findByAuthorUsername(username, pageable);
    }

    public Page<Post> byTag(String tagName, Pageable pageable) {
        return posts.findByTagsName(tagName.trim().toLowerCase(), pageable);
    }

    /** Cached: a post is read from the database once, then from the "posts" cache. */
    @Cacheable(value = "posts", key = "#id")
    public Post get(Long id) {
        return posts.findById(id).orElseThrow(() -> BlogException.notFound("Post"));
    }

    // Propagation REQUIRED (default): one transaction. The post is inserted, then its tags are added.
    // If a tag does not exist, everything is rolled back, including the post.
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public Post create(Long authorId, String title, String body, List<Long> tagIds) {
        User author = users.findById(authorId).orElseThrow(() -> BlogException.notFound("Author"));
        Post post = posts.save(new Post(author, title, body));
        addTags(post, tagIds);
        return post;
    }

    // Isolation READ_COMMITTED: only reads data that other transactions have committed.
    // @CachePut puts the updated post in the cache. saveAndFlush runs the UPDATE now, so a failure
    // happens before anything is cached.
    @Transactional(isolation = Isolation.READ_COMMITTED, rollbackFor = Exception.class)
    @CachePut(value = "posts", key = "#id")
    public Post update(Long id, String title, String body, List<Long> tagIds) {
        Post post = posts.findById(id).orElseThrow(() -> BlogException.notFound("Post"));
        post.setTitle(title);
        post.setBody(body);
        post.getTags().clear();
        addTags(post, tagIds);
        return posts.saveAndFlush(post);
    }

    // PostgreSQL first (flush runs the DELETE now), then the comments and reviews in MongoDB.
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = "posts", key = "#id")
    public void delete(Long id) {
        Post post = posts.findById(id).orElseThrow(() -> BlogException.notFound("Post"));
        posts.delete(post);
        posts.flush();
        comments.deleteByPostId(id);
        reviews.deleteByPostId(id);
    }

    private void addTags(Post post, List<Long> tagIds) {
        if (tagIds == null) {
            return;
        }
        for (Long tagId : tagIds) {
            Tag tag = tags.findById(tagId).orElseThrow(() -> BlogException.notFound("Tag " + tagId));
            post.getTags().add(tag);
        }
    }
}
