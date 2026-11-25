# Data layer (Lab 3)

PostgreSQL with Spring Data JPA, MongoDB, pagination, transactions and caching. This part is unchanged since Lab 3. The security part of Lab 4 is described in the [README](../README.md).

## Code structure

`Controller → Service → Repository`, as in Platform II.

| Package | What it contains |
|---|---|
| `controller.rest`, `controller.graphql` | REST and GraphQL endpoints. They call the services. |
| `service` | Business rules, `@Transactional` and the cache annotations. |
| `repository` | Spring Data interfaces. Spring writes their implementation at startup. |
| `entity` | The classes stored in the databases. The API returns them directly. |
| `dto` | The request bodies, with Bean Validation (`@NotBlank`, `@Email`...). |
| `config` | OpenAPI, GraphQL, the AOP logging aspect (`ServiceMonitoring`), and the Lab 4 security, CORS and admin setup. |
| `filter`, `handler` | Lab 4: the JWT filter, the 401/403 answers and the Google login handler. |

Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`) writes the getters, setters and empty constructors of the entities.

## Entities and relationships

| Entity | Stored in | Relationships |
|---|---|---|
| `User` | table `users` | has a BCrypt `password` and a `role` (`ADMIN`, `AUTHOR`, `READER`) |
| `Post` | table `posts` | `@ManyToOne author` (column `author_id`), `@ManyToMany tags` (join table `post_tags`) |
| `Tag` | table `tags` | — |
| `Comment` | MongoDB `comments` | keeps `postId` and `authorId` |
| `Review` | MongoDB `reviews` | keeps `postId` and `reviewerId` |

With `ddl-auto=update`, Hibernate reads these classes at startup and creates the tables, the foreign keys, the unique constraints and the indexes declared with `@Index`. Nothing has to be created by hand.

## Repositories and queries

The repositories are interfaces. `JpaRepository` already gives `save`, `findById`, `findAll`, `findAll(Pageable)`, `count` and `delete`. Each extra method is one of three kinds:

| Kind | Example | How it works |
|---|---|---|
| **Derived query** | `Page<Post> findByAuthorUsername(String username, Pageable pageable)` | Spring Data reads the method name and writes the SQL: posts whose `author.username` equals the value. |
| **JPQL** | `@Query("SELECT p FROM Post p WHERE LOWER(p.title) LIKE ...")` `searchByKeyword` | Written with the entity and field names (`Post`, `p.title`), not the table names. |
| **Native SQL** | `@Query(value = "DELETE FROM post_tags WHERE tag_id = :tagId", nativeQuery = true)` | Plain PostgreSQL SQL. Needed here because `post_tags` is a join table with no entity class, so JPQL cannot reach it. `@Modifying` marks it as a DELETE. |

The other derived queries are `findByTagsName`, `existsByAuthorId`, `existsByUsername`, `existsByEmail`, and for MongoDB `findByPostId`, `existsByPostIdAndReviewerId` and `deleteByPostId`.

## Pagination and sorting

The list endpoints receive a Spring `Pageable`, filled from the URL:

```
GET /api/posts?page=0&size=10&sort=createdAt,desc
GET /api/posts?keyword=spring&page=0&size=5&sort=title,asc
```

- Without parameters, `@PageableDefault` gives the first 10 posts, newest first.
- The page size has no upper limit: `spring.data.web.pageable.max-page-size` is commented out in `application.properties`.
- The repository adds `ORDER BY`, `LIMIT` and `OFFSET` to the SQL, so only one page leaves the database.
- The response contains the posts and the page information:

```json
{ "content": [ ... ], "page": { "size": 10, "number": 0, "totalElements": 42, "totalPages": 5 } }
```

## Transactions

| Where | What it does |
|---|---|
| `@Transactional(rollbackFor = Exception.class)` on every service method that writes | All the SQL of the method is one unit: it is committed at the end, or rolled back if an exception is thrown. By default Spring only rolls back for a `RuntimeException`; `rollbackFor = Exception.class` makes **any** exception roll back. |
| `PostService.create`: `propagation = Propagation.REQUIRED` | Joins the caller's transaction if there is one, otherwise starts a new one (this is the default). |
| `PostService.update`: `isolation = Isolation.READ_COMMITTED` | The transaction only reads data that other transactions have already committed (no "dirty reads"). |
| `@Version` in `Post` | If two people save the same post at the same moment, Hibernate refuses the second save, so one change cannot silently erase the other. The API answers **409**. |

### When a transaction fails

| What went wrong | Exception | Answer | Where |
|---|---|---|---|
| A business rule failed (for example, a tag does not exist) | `BlogException` | 404, 409 or 400 | `ApiErrors.businessError` |
| A unique value is already used (title, tag name, username, email) | `DataIntegrityViolationException` | 409, "The transaction was rolled back, nothing was saved." | `ApiErrors.duplicateValue` |
| Two people saved the same post at once (`@Version`) | `ConcurrencyFailureException` | 409, "Your change was rolled back." | `ApiErrors.concurrentChange` |
| The transaction could not start or commit (for example, the database is down) | `TransactionException` | 500, "The transaction failed and was rolled back." | `ApiErrors.transactionFailed` |
| Any other database error | `DataAccessException` | 500, "The transaction was rolled back." | `ApiErrors.databaseError` |

In every case Spring has already rolled the transaction back before the error handler answers. GraphQL answers with the same messages (`GraphQlErrors`).

**See the rollback in the console.** The `dev` profile logs Spring's transactions (`logging.level.org.springframework.orm.jpa.JpaTransactionManager=DEBUG`). A failed write prints:

```
Creating new transaction with name [...PostService.create]
Initiating transaction rollback
Rolling back JPA transaction on EntityManager [...]
```

**Rollback demo (Postman: "Rollback demo").** `PostService.create` first inserts the post, then adds its tags. Create a post with a tag ID that does not exist, for example `"tagIds": [1, 999999]`:

1. The INSERT of the post runs.
2. Tag 999999 is not found, so a `BlogException` is thrown.
3. Spring rolls the transaction back, and the post that was just inserted disappears.

The API answers 404, the console shows "Rolling back JPA transaction", and `GET /api/posts` shows that no post was added. Other rollbacks to try: give a post a title that is already used (409), or give a user an email that is already used (409).

**PostgreSQL first, then MongoDB.** Comments and reviews (MongoDB) are not part of the PostgreSQL transaction. So when a method writes to both, it does the PostgreSQL part first and forces it to run with `flush()` / `saveAndFlush()`. Only if that works does it touch MongoDB:

- `PostService.delete`: delete the post, `flush()`, then delete its comments and reviews.
- `UserService.delete`: delete the user, `flush()`, then delete their comments and reviews.
- `UserService.update`: `saveAndFlush` the user, then update the name copied into their comments and reviews.

If the PostgreSQL part fails, the transaction is rolled back and MongoDB is never touched. Each MongoDB save changes one document, which MongoDB saves atomically.

## Caching

`@EnableCaching` on `BloggingPlatformIVApplication` turns caching on. `spring.cache.type=simple` keeps the values in memory.

| Cache | Filled by `@Cacheable` | Replaced by `@CachePut` | Emptied by `@CacheEvict` |
|---|---|---|---|
| `posts` | `PostService.get(id)` (frequently read posts) | `PostService.update` | deleting the post; renaming or deleting a tag; **updating a user** |
| `users` | `UserService.get(id)` | `UserService.update` | deleting the user |
| `tags` | `TagService.list()` | — | creating, renaming or deleting a tag |

- `@Cacheable`: if the value is in the cache, the method is skipped.
- `@CachePut`: the method always runs, and its result replaces the cached value.
- `@CacheEvict`: removes the cached value.

**Keeping the cache consistent.** A cached post contains its author and its tags. So every change to a user or a tag must also clear the posts cache, otherwise a post would still show the old name:

| Change | What is refreshed |
|---|---|
| Update a user | `users` entry replaced (`@CachePut`), whole `posts` cache cleared (`@CacheEvict`), and the name copied into the user's comments and reviews (MongoDB) updated. All three in [UserService.update](../src/main/java/com/example/Blogging_Platform_IV/service/UserService.java). |
| Rename or delete a tag | `tags` and `posts` caches cleared |
| Update a post | `posts` entry replaced |
| Delete a post | `posts` entry removed, plus its comments and reviews |
| Delete a user | `users` entry removed, plus their comments and reviews |

**To see it work:** call `GET /api/posts/1` twice. The console (dev profile) shows `PostService.get(..) success in` about 5 ms the first time and under 1 ms the second time. Start the app with `--spring.cache.type=none` to compare without the cache.

