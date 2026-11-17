package com.example.Blogging_Platform_III.service;

import com.example.Blogging_Platform_III.entity.Post;
import com.example.Blogging_Platform_III.entity.Tag;
import com.example.Blogging_Platform_III.entity.User;
import com.example.Blogging_Platform_III.exception.BlogException;
import com.example.Blogging_Platform_III.model.PostFilter;
import com.example.Blogging_Platform_III.model.PostPage;
import com.example.Blogging_Platform_III.model.PostResponse;
import com.example.Blogging_Platform_III.model.Sort;
import com.example.Blogging_Platform_III.repository.PostRepository;
import com.example.Blogging_Platform_III.repository.TagRepository;
import com.example.Blogging_Platform_III.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostService {
    private final PostRepository posts;
    private final TagRepository tags;
    private final UserRepository users;

    public PostService(PostRepository posts, TagRepository tags, UserRepository users) {
        this.posts = posts;
        this.tags = tags;
        this.users = users;
    }

    /** Search with optional keyword, author and tag filters, sorted and paginated. */
    @Transactional(readOnly = true)
    public PostPage search(PostFilter filter) {
        PostFilter given = filter == null ? new PostFilter() : filter;
        Pageable pageable = pageRequest(given.getPage(), given.getSize(), given.getSort());
        if (given.getKeyword() != null && given.getKeyword().length() > 200) {
            throw BlogException.badRequest("Search keywords must be at most 200 characters.");
        }
        if ((given.getAuthorId() != null && given.getAuthorId() <= 0)
                || (given.getTagId() != null && given.getTagId() <= 0)) {
            throw BlogException.badRequest("Author and tag filters must be positive IDs.");
        }
        return toPage(posts.search(likePattern(given.getKeyword()), given.getAuthorId(), given.getTagId(), pageable));
    }

    /** Posts of one author, by username. Uses the derived query findByAuthorUsername... */
    @Transactional(readOnly = true)
    public PostPage byAuthor(String username, Integer page, Integer size, Sort sort) {
        Pageable pageable = pageRequest(page, size, sort);
        users.findByUsernameAndActiveTrue(username).orElseThrow(() -> BlogException.notFound("Author"));
        return toPage(posts.findByAuthorUsernameAndDeletedAtIsNull(username, pageable));
    }

    /** Posts with one tag, by tag name. Uses the derived query findByTagsName... */
    @Transactional(readOnly = true)
    public PostPage byTag(String name, Integer page, Integer size, Sort sort) {
        Pageable pageable = pageRequest(page, size, sort);
        String tagName = name.trim().toLowerCase(Locale.ROOT);
        tags.findByNameAndDeletedAtIsNull(tagName).orElseThrow(() -> BlogException.notFound("Tag"));
        return toPage(posts.findByTagsNameAndDeletedAtIsNull(tagName, pageable));
    }

    @Transactional(readOnly = true)
    public PostResponse get(long id) {
        return PostResponse.from(posts.findVisibleWithTags(id).orElseThrow(() -> BlogException.notFound("Post")));
    }

    /**
     * The post row and its post_tags rows are written in one transaction (REQUIRED).
     * If any statement fails, for example because the title is already used, every statement
     * of the method is rolled back and nothing is saved.
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public PostResponse create(long authorId, String title, String body, List<Long> tagIds) {
        User author = users.findByIdAndActiveTrue(authorId).orElseThrow(() -> BlogException.notFound("Author"));
        Post post = new Post(author, title, body);
        post.getTags().addAll(visibleTags(tagIds));
        try {
            return PostResponse.from(posts.saveAndFlush(post));
        } catch (DataIntegrityViolationException exception) {
            throw BlogException.duplicateValue();
        }
    }

    /**
     * REPEATABLE_READ: the transaction keeps working with the post as it was when it read it.
     * If another request changes and commits the same post in the meantime, PostgreSQL refuses
     * this UPDATE instead of silently overwriting the other change (a "lost update").
     * ApiErrors turns that refusal into HTTP 409, and the whole transaction is rolled back.
     */
    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public PostResponse update(long id, String title, String body, List<Long> tagIds) {
        Post post = posts.findVisibleWithTags(id).orElseThrow(() -> BlogException.notFound("Post"));
        List<Tag> selected = visibleTags(tagIds);
        post.setTitle(title);
        post.setBody(body);
        post.setUpdatedAt(LocalDateTime.now());
        post.getTags().clear();
        post.getTags().addAll(selected);
        try {
            return PostResponse.from(posts.saveAndFlush(post));
        } catch (DataIntegrityViolationException exception) {
            throw BlogException.duplicateValue();
        }
    }

    /** A soft delete: deleted_at is set. */
    @Transactional
    public void archive(long id) {
        Post post = posts.findByIdAndDeletedAtIsNull(id).orElseThrow(() -> BlogException.notFound("Post"));
        post.setDeletedAt(Instant.now());
    }

    /** Checks the paging values and builds the Pageable that the repository methods receive. */
    private Pageable pageRequest(Integer page, Integer size, Sort sort) {
        int pageNumber = page == null ? 0 : page;
        int pageSize = size == null ? 10 : size;
        if (pageNumber < 0 || pageSize < 1 || pageSize > 100) {
            throw BlogException.badRequest("Use a nonnegative page and a page size between 1 and 100.");
        }
        Sort order = sort == null ? Sort.NEWEST : sort;
        return PageRequest.of(pageNumber, pageSize, order.toSpringSort());
    }

    /**
     * Converts a page of entities into the response. The tags of all the posts on the page are
     * loaded with one JOIN FETCH query, instead of one query per post.
     */
    private PostPage toPage(Page<Post> page) {
        if (!page.isEmpty()) {
            posts.findWithTags(page.getContent().stream().map(Post::getId).toList());
        }
        return PostPage.of(page, page.getContent().stream().map(PostResponse::from).toList());
    }

    /**
     * Turns a keyword into a LIKE pattern: lowercase, wrapped in %, and with the characters
     * % and _ escaped by '!' so they are searched as normal characters.
     */
    private String likePattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String escaped = keyword.strip().toLowerCase(Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return "%" + escaped + "%";
    }

    /** Loads the selected tags in one query and checks that every one of them is available. */
    private List<Tag> visibleTags(List<Long> tagIds) {
        List<Long> ids = tagIds == null ? List.of() : tagIds.stream().distinct().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        List<Tag> found = tags.findByIdInAndDeletedAtIsNull(ids);
        if (found.size() != ids.size()) {
            throw BlogException.badRequest("Select tags that are available.");
        }
        return found;
    }
}
