package com.example.Blogging_Platform_III.service;

import com.example.Blogging_Platform_III.entity.Post;
import com.example.Blogging_Platform_III.entity.Tag;
import com.example.Blogging_Platform_III.entity.User;
import com.example.Blogging_Platform_III.exception.BlogException;
import com.example.Blogging_Platform_III.repository.CommentRepository;
import com.example.Blogging_Platform_III.repository.PostRepository;
import com.example.Blogging_Platform_III.repository.ReviewRepository;
import com.example.Blogging_Platform_III.repository.TagRepository;
import com.example.Blogging_Platform_III.repository.UserRepository;
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

    /**
     * Propagation REQUIRED (the default): the method runs in one transaction.
     * The post is inserted first, then its tags are added. If a tag does not exist, the
     * exception rolls the transaction back and the post that was just inserted disappears.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public Post create(Long authorId, String title, String body, List<Long> tagIds) {
        User author = users.findById(authorId).orElseThrow(() -> BlogException.notFound("Author"));
        Post post = posts.save(new Post(author, title, body));
        addTags(post, tagIds);
        return post;
    }

    /**
     * Isolation READ_COMMITTED: this transaction only reads data that other transactions have
     * already committed. If someone else saves the same post at the same time, the @Version
     * column in Post makes Hibernate refuse the second save (HTTP 409).
     * @CachePut puts the updated post in the "posts" cache. saveAndFlush sends the UPDATE now,
     * so if it fails (title already used, concurrent change) nothing is put in the cache.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @CachePut(value = "posts", key = "#id")
    public Post update(Long id, String title, String body, List<Long> tagIds) {
        Post post = posts.findById(id).orElseThrow(() -> BlogException.notFound("Post"));
        post.setTitle(title);
        post.setBody(body);
        post.getTags().clear();
        addTags(post, tagIds);
        return posts.saveAndFlush(post);
    }

    /** Deletes the post (and its post_tags rows), then its comments and reviews in MongoDB. */
    @Transactional
    @CacheEvict(value = "posts", key = "#id")
    public void delete(Long id) {
        Post post = posts.findById(id).orElseThrow(() -> BlogException.notFound("Post"));
        posts.delete(post);
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
