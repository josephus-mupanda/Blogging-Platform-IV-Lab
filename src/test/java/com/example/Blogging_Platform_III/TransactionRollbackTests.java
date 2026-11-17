package com.example.Blogging_Platform_III;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.Blogging_Platform_III.exception.BlogException;
import com.example.Blogging_Platform_III.model.PostResponse;
import com.example.Blogging_Platform_III.model.TagResponse;
import com.example.Blogging_Platform_III.repository.PostRepository;
import com.example.Blogging_Platform_III.repository.TagRepository;
import com.example.Blogging_Platform_III.service.PostService;
import com.example.Blogging_Platform_III.service.TagService;
import com.example.Blogging_Platform_III.service.UserService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Checks that @Transactional really rolls back when something goes wrong.
 * Runs against the test database (profile "test"). Every test uses new, unique names, so the
 * tests can run again and again on the same database.
 */
@SpringBootTest
@ActiveProfiles("test")
class TransactionRollbackTests {
    @Autowired
    private PostService postService;
    @Autowired
    private TagService tagService;
    @Autowired
    private UserService userService;
    @Autowired
    private PostRepository postRepository;
    @Autowired
    private TagRepository tagRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private String suffix;
    private long authorId;

    @BeforeEach
    void createAuthor() {
        suffix = String.valueOf(System.nanoTime());
        authorId = userService.create("writer" + suffix, "writer" + suffix + "@example.test", "Writer").getId();
    }

    /** A post whose title is already used: the INSERT fails and nothing is saved. */
    @Test
    void failedCreateSavesNothing() {
        postService.create(authorId, "Rollback " + suffix, "First body", List.of());
        long postsBefore = postRepository.count();

        BlogException error = assertThrows(BlogException.class,
                () -> postService.create(authorId, "  ROLLBACK " + suffix + "  ", "Second body", List.of()));

        assertEquals(BlogException.Code.CONFLICT, error.getCode());
        assertEquals(postsBefore, postRepository.count());
    }

    /** An update that fails leaves the post exactly as it was: title, body and tags. */
    @Test
    void failedUpdateKeepsTheOldPost() {
        TagResponse tag = tagService.create("keep-" + suffix);
        PostResponse post = postService.create(authorId, "Keep me " + suffix, "Old body", List.of(tag.getId()));
        postService.create(authorId, "Taken " + suffix, "Other body", List.of());

        assertThrows(BlogException.class,
                () -> postService.update(post.getId(), "Taken " + suffix, "New body", List.of()));

        PostResponse after = postService.get(post.getId());
        assertEquals("Keep me " + suffix, after.getTitle());
        assertEquals("Old body", after.getBody());
        assertEquals(1, after.getTags().size());
    }

    /**
     * Propagation REQUIRED: TagService.create joins the caller's transaction. The tag INSERT
     * runs, but when the caller fails afterwards the whole transaction is rolled back, the tag too.
     */
    @Test
    void joinedTransactionIsRolledBackTogether() {
        String name = "rolled-back-" + suffix;
        TransactionTemplate caller = new TransactionTemplate(transactionManager);

        assertThrows(IllegalStateException.class, () -> caller.executeWithoutResult(status -> {
            tagService.create(name);
            assertTrue(tagRepository.findByNameAndDeletedAtIsNull(name).isPresent(), "saved inside the transaction");
            throw new IllegalStateException("Something failed after the tag was saved");
        }));

        assertTrue(tagRepository.findByNameAndDeletedAtIsNull(name).isEmpty(), "rolled back afterwards");
    }

    /**
     * Isolation REPEATABLE_READ: transaction A reads a post, transaction B changes it and commits,
     * then A tries to save its own version. PostgreSQL refuses A's UPDATE, so B's change is not
     * lost, and A is rolled back. With READ_COMMITTED, A would silently overwrite B.
     */
    @Test
    void concurrentUpdateIsRefusedInsteadOfLost() {
        PostResponse post = postService.create(authorId, "Shared " + suffix, "Original", List.of());
        TransactionTemplate transactionA = new TransactionTemplate(transactionManager);
        transactionA.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        TransactionTemplate transactionB = new TransactionTemplate(transactionManager);
        transactionB.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThrows(ConcurrencyFailureException.class, () -> transactionA.executeWithoutResult(status -> {
            postRepository.findById(post.getId());
            transactionB.executeWithoutResult(inner ->
                    postService.update(post.getId(), "Changed by B " + suffix, "B", List.of()));
            postService.update(post.getId(), "Changed by A " + suffix, "A", List.of());
        }));

        assertEquals("Changed by B " + suffix, postService.get(post.getId()).getTitle());
    }
}
