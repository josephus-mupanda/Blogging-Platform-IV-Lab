
# Blogging Platform III

A Java 21 and Spring Boot 3.5 backend for the blogging platform. It continues Blogging Platform II: the same REST and GraphQL APIs, the same layers, the same profiles and the same databases. PostgreSQL stores users, posts and tags. MongoDB stores comments and reviews.

This phase replaces the hand-written `JdbcClient` and `MongoTemplate` code with **Spring Data repositories**. It adds pagination and sorting with `Pageable`, derived, JPQL and native queries, transaction management with `@Transactional`, and caching with Spring Cache.

## Lab requirements

| Epic | What to show |
|---|---|
| 1. Spring Data integration | `spring-boot-starter-data-jpa` in [pom.xml](pom.xml), the entities in `entity`, and `spring.jpa.hibernate.ddl-auto=validate` in [application.properties](src/main/resources/application.properties). The app starts on the existing tables without changing them. |
| 2. Repositories and queries | The five repositories in `repository`. Derived queries such as `findByAuthorUsernameAndDeletedAtIsNull` and `findByTagsNameAndDeletedAtIsNull`, JPQL in `PostRepository.search`, and native SQL in `PostRepository.countVisibleRecords`. Pagination and sorting through `GET /api/posts` and `GET /api/posts/{postId}/comments`. |
| 3. Transactions | `@Transactional` on every service method that writes. `PostService.update` uses `REPEATABLE_READ` isolation, and `TagService.create` and `PostService.create` use `REQUIRED` propagation. Run `TransactionRollbackTests` to see rollback. |
| 4. Caching | `CacheConfig` with `@EnableCaching`, and `@Cacheable` and `@CacheEvict` in `PostService`, `UserService` and `TagService`. |
| 5. Documentation | This file and the [performance report](docs/performance-report.md). |

## What changed from Platform II

| Platform II | Platform III |
|---|---|
| `JdbcClient` and hand-written SQL in classes | Spring Data interfaces that extend `JpaRepository` or `MongoRepository` |
| SQL text built with a `StringBuilder` for the search | One JPQL query with optional filters |
| `LIMIT` and `OFFSET` written by hand | `Pageable` and `PageRequest` |
| Comments returned as one full list | Comments returned one page at a time |
| No cache | Spring Cache for posts, users and tags |
| No tests | Tests that prove the transactions roll back |

The REST routes, the GraphQL schema and the JSON responses are the same as in Platform II. There are two exceptions: the comment list is now paginated, and there are two new post routes, `/api/posts/author/{username}` and `/api/posts/tag/{name}`.

## Code structure

The flow is the same as in Platform II: `Controller → Service → Repository`. For example, `PostController` and `PostGraphQlController` call `PostService`, and `PostService` calls `PostRepository`.

| Package | Responsibility |
|---|---|
| `controller.rest` and `controller.graphql` | Bind requests, validate DTOs, and format responses and errors. |
| `service` | Apply business rules, open transactions, use the cache, and call repositories. |
| `repository` | Spring Data interfaces. Spring writes the implementation when the app starts. |
| `entity` | The classes that are stored: `User`, `Post` and `Tag` (`@Entity`, PostgreSQL), and `Comment` and `Review` (`@Document`, MongoDB). |
| `model` | The classes that services return, such as `PostResponse`, `PostPage` and `CommentPage`. |
| `dto` | The input classes that requests are bound to, with Bean Validation. |
| `config` | `CacheConfig`, `OpenApiConfig`, `GraphQlConfig` and the AOP aspect `ServiceMonitoring`. |

Services never return entities. They copy each entity into a response class, such as `PostResponse.from(post)`, while the transaction is still open. Three reasons:

- The API does not show private columns such as the email.
- Nothing is loaded from the database after the transaction ends (`spring.jpa.open-in-view=false`).
- The cache stores plain objects.

### Entities and relationships

| Entity | Table | Relationships |
|---|---|---|
| `User` | `users` | None. The columns `password_hash`, `role` and `created_at` are not mapped, so PostgreSQL fills in their default values. |
| `Post` | `posts` | `@ManyToOne` to `User` (the author, `author_id`), and `@ManyToMany` to `Tag` through the `post_tags` table. |
| `Tag` | `tags` | None. |
| `Comment` | MongoDB `comments` | Holds `postId` and `authorId` as plain numbers. |
| `Review` | MongoDB `reviews` | Holds `postId` and `reviewerId` as plain numbers. |

Deletes are soft deletes, as in Platform II:

- **Users** get `active = false`.
- **Posts and tags** get a `deleted_at` value.
- **Comments** get a `deletedAt` value.

That is why many query names end in `DeletedAtIsNull` or `ActiveTrue`.

## Repositories and query logic

Every repository is an interface. `JpaRepository` and `MongoRepository` already provide `save`, `findById`, `findAll`, `count` and `delete`, and both extend `CrudRepository`. The methods below are the extra ones this project declares.

There are three kinds of query:

- **Derived:** Spring Data builds the query from the method name. For example, `findByIdAndActiveTrue` becomes `WHERE user_id = ? AND active`.
- **JPQL:** a query written with `@Query` against the entity classes and their fields (`Post`, `p.author.id`), not the tables.
- **Native:** plain PostgreSQL SQL written with `@Query(nativeQuery = true)`.

### UserRepository (`JpaRepository<User, Long>`)

| Method | Kind | Used for |
|---|---|---|
| `findByIdAndActiveTrue` | Derived | Read, update or delete an active user |
| `findByUsernameAndActiveTrue` | Derived | Check the author of `/api/posts/author/{username}` |
| `findByActiveTrueOrderByUsernameAscIdAsc` | Derived | `GET /api/users` |
| `existsByUsername`, `existsByEmail`, `existsByEmailAndIdNot` | Derived | The unique username and email checks |

### TagRepository (`JpaRepository<Tag, Long>`)

| Method | Kind | Used for |
|---|---|---|
| `findByDeletedAtIsNullOrderByNameAscIdAsc` | Derived | `GET /api/tags` |
| `findByIdAndDeletedAtIsNull` | Derived | Read, rename or delete a tag |
| `findByNameAndDeletedAtIsNull` | Derived | Check the tag of `/api/posts/tag/{name}` |
| `findByIdInAndDeletedAtIsNull` | Derived | Load all the tags of a new or edited post in one query |

### PostRepository (`JpaRepository<Post, Long>`)

| Method | Kind | Used for |
|---|---|---|
| `findByIdAndDeletedAtIsNull`, `existsByIdAndDeletedAtIsNull` | Derived | Check that a post is visible |
| `findByAuthorUsernameAndDeletedAtIsNull(username, pageable)` | Derived, paginated | `GET /api/posts/author/{username}` |
| `findByTagsNameAndDeletedAtIsNull(name, pageable)` | Derived, paginated | `GET /api/posts/tag/{name}` |
| `search(pattern, authorId, tagId, pageable)` | JPQL, paginated | `GET /api/posts` and the GraphQL `posts` query |
| `findVisibleWithTags(id)` | JPQL with `JOIN FETCH` | Read one post and its tags in one query |
| `findWithTags(ids)` | JPQL with `JOIN FETCH` | Load the tags of a whole page in one query |
| `findArchivedIds()` | Native SQL | The dashboard leaves out comments of deleted posts |
| `countVisibleRecords()` | Native SQL | The dashboard counts of users, posts and tags in one round trip |

How `search` works:

- **Optional filters.** Each filter is written as `(:authorId IS NULL OR p.author.id = :authorId)`. When the client leaves a filter out, the parameter is null, the condition is true for every row, and that filter does nothing.
- **Keyword.** `PostService` turns the keyword into a lowercase `LIKE` pattern such as `%spring%`. It escapes `%` and `_` with `!`, so they are searched as normal characters. The query compares `lower(p.title)` and `lower(p.body)`, and the trigram indexes in [db/schema.sql](db/schema.sql) are built on those same expressions so PostgreSQL can use them.
- **Tag filter.** It is an `IN (subquery)` that PostgreSQL runs once. The performance report shows why this is faster than an `EXISTS` subquery.

Why the two native queries are native:

- `countVisibleRecords` needs three `SELECT count(*)` subqueries in one `SELECT` with no `FROM`. JPQL cannot write that. The result is read through the `DashboardCounts` interface: Spring Data matches each getter to a column alias.
- `findArchivedIds` is a plain SQL query that matches the partial index `idx_posts_archived`.

### CommentRepository and ReviewRepository (`MongoRepository`)

| Method | Kind | Used for |
|---|---|---|
| `findByPostIdAndDeletedAtIsNull(postId, pageable)` | Derived, paginated | `GET /api/posts/{postId}/comments` |
| `findByIdAndPostIdAndDeletedAtIsNull` | Derived | Read one comment |
| `updateBody`, `archive` | `@Query` and `@Update` | Edit or hide a comment in one atomic MongoDB operation |
| `countByPostIdNotInAndDeletedAtIsNull`, `countByPostIdNotIn` | Derived | Dashboard counts |
| `findByPostIdOrderByCreatedAtAscIdAsc` | Derived | `GET /api/posts/{postId}/reviews` |

### Pagination and sorting

The services turn `page`, `size` and `sort` into a `PageRequest`, which is a `Pageable`, and pass it to the repository:

```java
PageRequest.of(page, size, sort.toSpringSort())
```

- **What Spring Data does with it.** It adds `ORDER BY`, `LIMIT` and `OFFSET` to the query, then runs a second `COUNT` query for `totalElements` and `totalPages`. The database does the paging, so only one page of rows reaches Java.
- **Sort values.** `NEWEST`, `OLDEST`, `TITLE_ASC` and `TITLE_DESC`. Each one is turned into a Spring `Sort` in the `Sort` enum.
- **Fixed order.** The post ID is always the last sort key, so posts with the same date or title keep a fixed order and two pages never show the same post.
- **Limits.** Pages start at 0, the default size is 10, and the size can be 1 to 100.
- **Comments** are sorted oldest first.

## Transaction handling

Every service method that writes has `@Transactional`. Every method that only reads has `@Transactional(readOnly = true)`. Spring opens the transaction when the method starts and commits it when the method returns. If the method throws a `RuntimeException`, Spring rolls back everything the method wrote. `BlogException` is a `RuntimeException`, so a failed business rule always means a rollback.

| Where | Setting | Why |
|---|---|---|
| All read methods | `readOnly = true` | Hibernate skips checking the loaded entities for changes, and the database knows nothing will be written. |
| `PostService.create` | `propagation = REQUIRED` | The post row and its `post_tags` rows are written in one transaction. If the title is already taken, nothing is saved. |
| `TagService.create` | `propagation = REQUIRED` | If a caller already has a transaction, the tag joins it and is committed or rolled back with it. `REQUIRED` is Spring's default; it is written out here to show the choice. |
| `PostService.update` | `isolation = REPEATABLE_READ` | Two requests that edit the same post at the same moment cannot overwrite each other without noticing. PostgreSQL refuses the second one ("could not serialize access due to concurrent update"), and the API answers **409** so the client can reload and retry. With PostgreSQL's default, `READ_COMMITTED`, the second edit would silently replace the first. |
| Other writes | Default (`REQUIRED`, `READ_COMMITTED`) | One write per request, so the defaults are enough. |

Unique values are checked by the database. The services call `saveAndFlush`, which sends the SQL immediately, so a duplicate title, tag name, username or email is caught inside the method. It is turned into a `BlogException` with HTTP 409, and the transaction is rolled back.

**MongoDB.** The lab's MongoDB runs as a single server, and MongoDB transactions need a replica set, so the comment and review writes are not part of the PostgreSQL transaction. Every one of them changes exactly one document, and MongoDB applies a single-document write atomically. Editing and hiding a comment use `@Update` queries: the filter (`deletedAt: null`) and the change happen in one operation. So an edit that arrives just after a delete cannot bring the comment back.

**Proof.** [TransactionRollbackTests](src/test/java/com/example/Blogging_Platform_III/TransactionRollbackTests.java) checks four error scenarios on the test database:

| Test | What it proves |
|---|---|
| `failedCreateSavesNothing` | A post with a duplicate title is not saved, and the post count does not change. |
| `failedUpdateKeepsTheOldPost` | A failed update leaves the title, body and tags as they were. |
| `joinedTransactionIsRolledBackTogether` | `TagService.create` joins the caller's transaction (`REQUIRED`). The tag is inserted, then rolled back when the caller fails. |
| `concurrentUpdateIsRefusedInsteadOfLost` | Transaction A reads a post, then transaction B (`REQUIRES_NEW`) edits it and commits. A's edit is refused under `REPEATABLE_READ`, and B's change stays. |

## Caching

Spring Cache keeps the result of a method in memory. The next call with the same key returns the saved result without running the method, so there is no database query.

**Configuration.**

- `CacheConfig` has `@EnableCaching`.
- `application.properties` uses the simple in-memory cache. Nothing else needs to be installed.

  ```properties
  spring.cache.type=simple
  spring.cache.cache-names=posts,users,userList,tags,tagList
  ```

**Strategy.** Only single-item reads and short lists are cached:

| Cache | Filled by (`@Cacheable`) | Key | Emptied by (`@CacheEvict`) |
|---|---|---|---|
| `posts` | `PostService.get` | post ID | Updating or deleting that post; renaming or deleting any tag (all posts, because posts show tag names) |
| `users` | `UserService.get` | user ID | Updating or deleting that user |
| `userList` | `UserService.list` | (one entry) | Creating, updating or deleting any user |
| `tags` | `TagService.get` | tag ID | Renaming or deleting that tag |
| `tagList` | `TagService.list` | (one entry) | Creating, renaming or deleting any tag |

- **Popular posts.** A post is cached the first time someone reads it, so posts that are read often are served from memory.
- **Searches are not cached.** Every combination of keyword, filters, page and sort would need its own entry, and any new post would make all of them wrong.
- **New posts evict nothing.** A new post cannot be in the cache yet.

**Check that it works.**

1. Start the app (the `dev` profile logs the service timings).
2. Call `GET /api/posts/1` twice.
3. In the console, the first call shows `PostService.get(..) success in` about 3 ms, and the second about 1 ms.
4. To see the SQL, add `--logging.level.org.hibernate.SQL=DEBUG`. Only the first call prints a `select`.
5. To compare without the cache, start the app with `--spring.cache.type=none`.

## Run locally

Install JDK 21, PostgreSQL with `pg_trgm`, and MongoDB. The project includes a Maven wrapper. From the repository root, create the development database, run the setup script once, and start the app:

```powershell
$env:Path += ";C:\Program Files\PostgreSQL\18\bin"
$env:PGPASSWORD = 'postgres'
createdb -h localhost -p 5433 -U postgres blogging_platform_iii
psql -h localhost -p 5433 -U postgres -d blogging_platform_iii -f db/schema.sql
.\mvnw.cmd spring-boot:run
```

[db/schema.sql](db/schema.sql) contains the Platform II tables and indexes plus the Lab 3 indexes. It only creates what is missing, so it is safe to run again. Hibernate does not create tables: with `ddl-auto=validate` it only checks at startup that the entities match the tables, and stops the app if they do not.

To use the existing Platform II database instead, run `db/schema.sql` on it once to add the Lab 3 indexes, then set `DB_POSTGRES_URL` to that database. MongoDB needs no setup script: it creates its collections and indexes on first use. Use `./mvnw` on macOS or Linux.

## Environment profiles

| Profile | PostgreSQL | MongoDB | GraphiQL |
|---|---|---|---|
| `dev` (default) | `jdbc:postgresql://localhost:5433/blogging_platform_iii`, user and password `postgres` | `mongodb://localhost:27017/blogging_platform_iii` | Enabled |
| `test` | `jdbc:postgresql://localhost:5433/blogging_platform_iii_test`, user and password `postgres` | `mongodb://localhost:27017/blogging_platform_iii_test` | Enabled |
| `prod` | Explicit connection settings | Explicit connection settings | Disabled |

- **Choosing a profile.** Set `SPRING_PROFILES_ACTIVE`.
- **Connection settings.** For dev or prod, set `DB_POSTGRES_URL`, `DB_POSTGRES_USER`, `DB_POSTGRES_PASSWORD` and `DB_MONGO_URI`. For the test profile, use the `TEST_` prefix instead of `DB_`.
- **Access.** The API listens on `127.0.0.1` only and accepts requests without login.

## Testing

### Automated tests

The tests use the `test` profile. Create the test database once, then run them:

```powershell
createdb -h localhost -p 5433 -U postgres blogging_platform_iii_test
psql -h localhost -p 5433 -U postgres -d blogging_platform_iii_test -f db/schema.sql
.\mvnw.cmd test
```

There are two test classes:

- `BloggingPlatformIiiApplicationTests` starts the whole application. It fails if an entity does not match its table or if a JPQL query is wrong.
- `TransactionRollbackTests` runs the four rollback scenarios described above.

### Swagger and Postman

Open Swagger UI at [localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html), or import [docs/postman_collection.json](docs/postman_collection.json) into Postman. Run the Postman folders from top to bottom: the create requests save their IDs for the later requests. Every REST response has `status`, `message` and `data`.

| Routes | Operations |
|---|---|
| `/api/users`, `/api/users/{id}` | Create, list, read, update or delete user profiles |
| `/api/posts`, `/api/posts/{id}` | Search, read, create, edit and delete posts |
| `/api/posts/author/{username}` | Posts of one author, paginated (derived query) |
| `/api/posts/tag/{name}` | Posts with one tag, paginated (derived query) |
| `/api/tags`, `/api/tags/{id}` | List, read, create, rename or delete tags |
| `/api/posts/{postId}/comments`, `/api/posts/{postId}/comments/{id}` | List one page, read, add, edit and delete comments |
| `/api/posts/{postId}/reviews` | List reviews with the average rating, or submit a review |
| `/api/dashboard` | Count visible users, posts, tags, comments and reviews |

```powershell
curl.exe "http://127.0.0.1:8080/api/posts?keyword=spring&page=0&size=10&sort=NEWEST"
curl.exe "http://127.0.0.1:8080/api/posts/1/comments?page=0&size=5"
```

`GET /api/posts` also accepts `authorId` and `tagId`. The author and tag routes accept `page`, `size` and `sort`. The comments route accepts `page` and `size`.

Use `127.0.0.1` rather than `localhost` with `curl.exe`. On Windows, `localhost` is tried as IPv6 first, and each call waits about 200 ms before it falls back.

### GraphQL

Open [GraphiQL](http://localhost:8080/graphiql) in dev or test and run the operations in [docs/examples.graphql](docs/examples.graphql). REST and GraphQL call the same services, so they use the same repositories, transactions and cache. The `comments` query now takes optional `page` and `size` arguments and returns a `CommentPage`.

## Performance

The [performance report](docs/performance-report.md) measures:

- the query plans before and after the Lab 3 indexes;
- the tag filter before and after the JPQL rewrite;
- the N+1 queries against `JOIN FETCH`;
- pagination depth and sorting;
- the API with and without the cache.

The [Platform III lab brief](Blogging-Platform-III.md) describes the requirements.
