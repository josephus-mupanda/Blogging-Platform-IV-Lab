# Review notes: Blogging Platform III

A revision guide for the lab review. For every requirement it says **what it means**, **where it is in the code**, and **how to show it**.

Code links point to `src/main/java/com/example/Blogging_Platform_III/`.

---

## 1. Before the review (5 minutes)

1. Check that PostgreSQL (port 5433) and MongoDB (port 27017) are running.
2. If the database does not exist yet, create it once:
   ```powershell
   $env:Path += ";C:\Program Files\PostgreSQL\18\bin"
   createdb -h localhost -p 5433 -U postgres blogging_platform_iii
   ```
3. Start the app: `.\mvnw.cmd spring-boot:run`. Hibernate creates the tables at startup.
4. Open these in your browser and in Postman:
   - Swagger: http://localhost:8080/swagger-ui/index.html
   - GraphiQL: http://localhost:8080/graphiql
   - Postman: import `docs/postman_collection.json`, then run folders 1, 2 and 3 once so there is data.
5. Keep the terminal visible. In the `dev` profile, the AOP aspect prints the time of every service call there, which you need to show the cache.

---

## 2. The project in one minute (what to say first)

> "Platform III continues Platform II: same REST and GraphQL APIs, same layers (Controller → Service → Repository), same profiles. In this phase I replaced the hand-written JDBC code with **Spring Data JPA**. My entities are mapped with `@Entity`, and **Hibernate creates the tables and relationships** from them. The repositories extend `JpaRepository`, and I use **derived, JPQL and native queries**. Lists are **paginated and sorted with `Pageable`**. Writes are **`@Transactional`**, with propagation, isolation and rollback. Posts, users and tags are **cached with Spring Cache**. I measured the effect of the indexes, `@BatchSize` and the cache in the performance report. Comments and reviews stay in MongoDB, as designed in Platform I."

---

## 3. Requirement checklist

✅ = done and easy to show. ⚠️ = done, but know the weak point (see section 7).

### Epic 1: Spring Data integration

| Requirement | Where | How to show |
|---|---|---|
| ✅ Spring Data JPA dependency added and configured | [pom.xml](pom.xml) (`spring-boot-starter-data-jpa`), [application.properties](src/main/resources/application.properties#L10) (`ddl-auto=update`) | Open both files. |
| ✅ Entities with `@Entity`, `@Id`, relationships | [Post.java](src/main/java/com/example/Blogging_Platform_III/entity/Post.java#L31): `@ManyToOne author` (line 53), `@ManyToMany tags` (line 63). [User.java](src/main/java/com/example/Blogging_Platform_III/entity/User.java#L15), [Tag.java](src/main/java/com/example/Blogging_Platform_III/entity/Tag.java#L14) | Open `Post.java`. In pgAdmin or psql, `\d posts` shows the foreign key `author_id`, and `\d post_tags` shows the join table. |
| ✅ Repositories extend `JpaRepository` or `CrudRepository` | [repository/](src/main/java/com/example/Blogging_Platform_III/repository) | `UserRepository`, `PostRepository`, `TagRepository` extend `JpaRepository`. `CommentRepository` and `ReviewRepository` extend `MongoRepository`, which extends `CrudRepository`. |
| ✅ Connects to the existing database | `application-dev.properties` | The app starts. `ddl-auto=update` keeps existing data and only adds what is missing. |

### Epic 2: Repositories and queries

| Requirement | Where | How to show |
|---|---|---|
| ✅ Repositories for User, Post, Comment, Tag, Review | the 5 files in `repository/` | Open the folder. |
| ✅ Derived queries (`findByAuthorName`, `findByTagName`) | [PostRepository.java:19](src/main/java/com/example/Blogging_Platform_III/repository/PostRepository.java#L19) `findByAuthorUsername`, [line 22](src/main/java/com/example/Blogging_Platform_III/repository/PostRepository.java#L22) `findByTagsName` | Postman: "Posts of one author" and "Posts with one tag". |
| ✅ `@Query` with JPQL | [PostRepository.java:29](src/main/java/com/example/Blogging_Platform_III/repository/PostRepository.java#L29) `searchByKeyword` | Postman: "Search posts by keyword (JPQL)". |
| ✅ `@Query` with native SQL | [TagRepository.java:16-17](src/main/java/com/example/Blogging_Platform_III/repository/TagRepository.java#L16) `removeTagFromPosts` | Postman: delete a tag that is used by a post, then read the post: the tag is gone from it. |
| ✅ Pagination and sorting with `Pageable` | [PostController.java](src/main/java/com/example/Blogging_Platform_III/controller/rest/PostController.java#L44) (`@PageableDefault`), [DiscussionController.java](src/main/java/com/example/Blogging_Platform_III/controller/rest/DiscussionController.java#L42) | Postman: "List posts - page, size, sort". Change `page`, `size`, `sort=title,asc`. Show the `page` block in the response. |
| ✅ Paginated responses for posts **and** comments | `GET /api/posts`, `GET /api/posts/{id}/comments` | Postman: "List comments - one page". |
| ✅ Sorting and pagination performance tested and documented | [docs/performance-report.md](docs/performance-report.md), section 4 | Show the table: page 0 compared with page 1000. |

### Epic 3: Transactions and optimization

| Requirement | Where | How to show |
|---|---|---|
| ✅ `@Transactional` on create/update/delete | `UserService`, `TagService`, `PostService`, `DiscussionService` | Open `PostService`. |
| ✅ Propagation | [PostService.java:68](src/main/java/com/example/Blogging_Platform_III/service/PostService.java#L68) `propagation = Propagation.REQUIRED` | Explain (section 4.5). |
| ✅ Isolation | [PostService.java:83](src/main/java/com/example/Blogging_Platform_III/service/PostService.java#L83) `isolation = Isolation.READ_COMMITTED` | Explain (section 4.5). |
| ✅ No data loss during concurrent updates | [Post.java:83](src/main/java/com/example/Blogging_Platform_III/entity/Post.java#L83) `@Version` → 409 in [ApiErrors.java:49](src/main/java/com/example/Blogging_Platform_III/controller/rest/ApiErrors.java#L49) | Explain. In psql, `SELECT id, version FROM posts;` shows that the version goes up after each update. |
| ✅ Rollback verified in an error scenario | [PostService.java:71-72](src/main/java/com/example/Blogging_Platform_III/service/PostService.java#L71) | **Rollback demo** (section 5, step 4). |
| ✅ Rollback for any exception, and a clear answer when a transaction fails | `rollbackFor = Exception.class` on every write method. Handlers in [ApiErrors.java:49-83](src/main/java/com/example/Blogging_Platform_III/controller/rest/ApiErrors.java#L49) | Postman: "Same title again" gives 409 "The transaction was rolled back, nothing was saved." The console shows "Rolling back JPA transaction". |
| ✅ PostgreSQL and MongoDB stay consistent | `flush()` before the MongoDB writes: [PostService.java:104](src/main/java/com/example/Blogging_Platform_III/service/PostService.java#L104), [UserService.java:96](src/main/java/com/example/Blogging_Platform_III/service/UserService.java#L96) | Delete a post: its comments and reviews are deleted too. |
| ⚠️ Complex JPQL optimized | `searchByKeyword` and `@BatchSize` | See section 7. |
| ✅ Indexes validated for frequent queries | [Post.java:34-36, 68](src/main/java/com/example/Blogging_Platform_III/entity/Post.java#L34) (`@Index`) | `\d posts` in psql. Performance report, section 1. |
| ✅ Query times before and after optimization | [docs/performance-report.md](docs/performance-report.md), sections 1 and 2 | Index: 17–32 ms → 0.1 ms. `@BatchSize`: 13 or 53 SQL statements → 4. |

### Epic 4: Caching

| Requirement | Where | How to show |
|---|---|---|
| ✅ Cache for popular posts, users and tags | [PostService.java:57](src/main/java/com/example/Blogging_Platform_III/service/PostService.java#L57), [UserService.java:44](src/main/java/com/example/Blogging_Platform_III/service/UserService.java#L44), [TagService.java:22](src/main/java/com/example/Blogging_Platform_III/service/TagService.java#L22) (`@Cacheable`) | Send "Get post (cached)" twice and look at the terminal times (section 5, step 6). |
| ✅ `@EnableCaching` | [BloggingPlatformIiiApplication.java:9](src/main/java/com/example/Blogging_Platform_III/BloggingPlatformIiiApplication.java#L9) | Open the file. |
| ✅ Invalidation after create/update | `@CachePut` on update: [PostService.java:84](src/main/java/com/example/Blogging_Platform_III/service/PostService.java#L84), [UserService.java:64](src/main/java/com/example/Blogging_Platform_III/service/UserService.java#L64). `@CacheEvict` on create and delete: [TagService.java:33](src/main/java/com/example/Blogging_Platform_III/service/TagService.java#L33), [PostService.java:100](src/main/java/com/example/Blogging_Platform_III/service/PostService.java#L100) | Update a post, then read it: you see the new title, with no SQL. |
| ✅ No stale data: a cached post never shows an old author name | [UserService.java:64-65](src/main/java/com/example/Blogging_Platform_III/service/UserService.java#L64): `@Caching(put = users, evict = posts)`, plus the name copied into comments and reviews | Section 5, step 7. |
| ✅ Performance improvement measured | [docs/performance-report.md](docs/performance-report.md), section 3 | Reading a post: 4.7 ms → 0.6 ms in the service. |

### Epic 5: Documentation

| Requirement | Where |
|---|---|
| ✅ Repository structure and query logic documented | [README.md](README.md), "Repositories and queries", plus comments in each repository |
| ✅ Transaction strategies described | README, "Transactions" |
| ✅ README with caching configuration and testing steps | README, "Caching" and "Testing the API" |

### Deliverables

| Deliverable | Where |
|---|---|
| ✅ Spring Data integration | `entity/`, `repository/` |
| ✅ Custom queries (JPQL and native) | `PostRepository.searchByKeyword`, `TagRepository.removeTagFromPosts` |
| ✅ Transaction management, tested for rollback | `PostService` + the Postman "Rollback demo" (manual test; there are no automated tests) |
| ✅ Caching with a clear strategy and eviction | `@Cacheable`, `@CachePut`, `@CacheEvict`, and the README table |
| ✅ Performance report (before and after) | `docs/performance-report.md` |
| ✅ OpenAPI extended with the repository-driven endpoints | Swagger: `/api/posts`, `/api/posts/author/{username}`, `/api/posts/tag/{name}`, `/api/posts/{postId}/comments` have descriptions naming the query they run |
| ✅ README | `README.md` |

### Still there from Platform II

| Feature | Where |
|---|---|
| Profiles `dev`, `test`, `prod` | `application-*.properties` |
| Bean Validation and a custom validator | `dto/` (`@NotBlank`, `@Email`...), [UserCreate.java:9](src/main/java/com/example/Blogging_Platform_III/dto/UserCreate.java#L9) `@UniqueAccount` |
| Central error handling | `controller/rest/ApiErrors.java` (`@RestControllerAdvice`) |
| GraphQL | `controller/graphql/`, `resources/graphql/blog.graphqls` |
| AOP logging and timing | [ServiceMonitoring.java](src/main/java/com/example/Blogging_Platform_III/config/ServiceMonitoring.java#L15) (`@Before`, `@After`, `@Around`) |

---

## 4. The concepts in simple words

### 4.1 Spring Data JPA and Hibernate

- **JPA** is the Java standard for saving objects in a relational database.
- **Hibernate** is the library that does the work (it turns objects into SQL).
- **Spring Data JPA** writes the repository code for us.
- `ddl-auto=update`: at startup, Hibernate reads the entities and creates the tables, columns, foreign keys and indexes that are missing. It never deletes data.

### 4.2 Entities and relationships

- `@Entity` means "this class is a table". `@Id` + `@GeneratedValue` means "this is the primary key, and the database generates it".
- `@ManyToOne User author`: many posts belong to one user. Hibernate adds the column `author_id` with a foreign key to `users`.
- `@ManyToMany Set<Tag> tags`: a post has many tags, and a tag is on many posts. Hibernate creates the join table `post_tags (post_id, tag_id)`.
- `@CreationTimestamp` / `@UpdateTimestamp`: Hibernate fills the dates for us.
- `@JsonIgnore` on `email`: the email is never sent back in a response.
- Lombok's `@Getter`, `@Setter` and `@NoArgsConstructor` write the getters, setters and the empty constructor that JPA needs.

### 4.3 The three kinds of queries

| Kind | Example | Explanation |
|---|---|---|
| Derived | `findByAuthorUsername(String username, Pageable p)` | Spring reads the name: **find** posts **by** the **author**'s **username**. No SQL to write. |
| JPQL | `SELECT p FROM Post p WHERE LOWER(p.title) LIKE ...` | SQL-like, but on **classes and fields** (`Post`, `p.title`), not tables. It works on any database. |
| Native | `DELETE FROM post_tags WHERE tag_id = :tagId` | Real SQL on **tables**. Used because `post_tags` has no entity class, so JPQL cannot see it. `@Modifying` is required for DELETE and UPDATE. |

### 4.4 Pagination and sorting

- The URL `?page=2&size=10&sort=title,asc` becomes a `Pageable` object in the controller.
- The repository adds `ORDER BY title ASC LIMIT 10 OFFSET 20` to the SQL, plus a `COUNT` query for the total.
- So the database sends only one page; we never load all the posts into Java.
- `@PageableDefault` gives the default values: 10 newest posts.
- `max-page-size=100` stops anyone from asking for a million rows.

### 4.5 Transactions

- **Transaction:** a group of SQL statements that succeed together or are undone together.
- **`@Transactional`:** Spring starts a transaction when the method starts, commits it when the method ends, and **rolls back** if a `RuntimeException` is thrown. `BlogException` is a `RuntimeException`.
- **Propagation `REQUIRED`** (the default): if a transaction is already running, join it; otherwise start a new one. (`REQUIRES_NEW` would always start a separate one.)
- **Isolation `READ_COMMITTED`:** my transaction only sees data that other transactions have already **committed**, never their unfinished changes (no "dirty reads"). It is PostgreSQL's default; I wrote it to show the choice.
- **`@Version` (optimistic locking):** each post has a version number.
  1. Person A and person B both load version 3.
  2. A saves first, and the version becomes 4.
  3. B's save says "UPDATE ... WHERE version = 3", which finds no row, so Hibernate refuses it and the API answers **409**. Without it, B would silently erase A's change.
- **Rollback demo** (`PostService.create`):
  1. `posts.save(...)` inserts the post.
  2. `addTags` looks up each tag; tag 999999 does not exist, so a `BlogException` is thrown.
  3. Spring rolls back the transaction, and the INSERT from step 1 is undone.
- **`rollbackFor = Exception.class`:** by default Spring rolls back only for a `RuntimeException` (and `Error`). With `rollbackFor = Exception.class`, **every** exception rolls back, including checked ones. It is on every write method.
- **When a transaction fails, the client is told** (`ApiErrors`, and `GraphQlErrors` for GraphQL):

  | Exception | Example | Answer |
  |---|---|---|
  | `BlogException` | a tag does not exist | 404 / 409 / 400 |
  | `DataIntegrityViolationException` | title, tag name or email already used | 409, "The transaction was rolled back, nothing was saved." |
  | `ConcurrencyFailureException` | `@Version` conflict | 409, "Your change was rolled back." |
  | `TransactionException` | database down, commit failed | 500, "The transaction failed and was rolled back." |
  | `DataAccessException` | any other database error | 500, "The transaction was rolled back." |

- **Seeing the rollback:** the `dev` profile logs Spring's `JpaTransactionManager`. A failed write prints "Initiating transaction rollback" and then "Rolling back JPA transaction".
- **MongoDB** (comments and reviews) is not part of the PostgreSQL transaction. So a method that writes to both does **PostgreSQL first** and forces it with `flush()` / `saveAndFlush()`, then MongoDB. If PostgreSQL fails, the transaction is rolled back and MongoDB is never touched. For example, `PostService.delete` deletes the post, calls `flush()`, and only then deletes the comments.

### 4.6 Caching

- **Cache:** keeping a result in memory so the next request does not go to the database.
- `@EnableCaching` (on the application class) switches it on. `spring.cache.type=simple` means in memory, a `ConcurrentHashMap`.
- **`@Cacheable("posts", key = "#id")`:** before running `get(id)`, Spring looks in the cache. If the post is there, the method is skipped.
- **`@CachePut`:** the method **always runs**, then its result replaces the value in the cache. Used on `update`, so the cache holds the new version. `saveAndFlush` makes the SQL run inside the method, so if it fails, nothing goes into the cache.
- **`@CacheEvict`:** removes the value. Used on delete, and on tag changes (`allEntries = true` clears the whole cache, because posts show tag names).
- **Cache consistency:** a cached post **contains its author and its tags**. So when a user is updated, `UserService.update` does three things:
  1. `@CachePut` puts the new user in the `users` cache.
  2. `@CacheEvict(value = "posts", allEntries = true)` clears the posts cache, so no post shows the old name.
  3. It updates the name copied into that user's comments and reviews in MongoDB.

  `@Caching` groups the two annotations on one method. Tag changes clear the posts cache in the same way.

### 4.7 Optimization

- **Index:** like the index of a book. With `idx_posts_created_at`, PostgreSQL finds the 10 newest posts without reading all 20,000. Declared with `@Index` in `Post.java`.
- **N+1 problem:** loading a page of 10 posts costs 1 query, then 1 query per post for its tags, so 11 or more. `@BatchSize(size = 50)` makes Hibernate load the tags of the whole page in **one** query.

---

## 5. Demo script (about 10 minutes)

1. **Structure** (1 min): show the packages `controller → service → repository → entity`.
2. **Entities and Hibernate** (1 min): open `Post.java` and point at `@ManyToOne`, `@ManyToMany`, `@Index` and `@Version`. Then in psql: `\d posts` and `\d post_tags`.
3. **Queries** (2 min): open `PostRepository` and `TagRepository` and name the three kinds. In Postman, run "Posts of one author", "Search posts by keyword", and "List posts - page, size, sort" with `sort=title,asc`.
4. **Rollback** (2 min):
   1. Run "List posts" and note `totalElements`.
   2. Run "Rollback demo": you get a 404, "Tag 999999 was not found".
   3. Run "List posts" again: `totalElements` is the same, so the inserted post was rolled back.
   4. Show `PostService.create` and explain the three steps.
   5. Show the console: "Initiating transaction rollback" / "Rolling back JPA transaction".
   6. Run "Same title again" twice: the second gives 409, "The transaction was rolled back, nothing was saved."
5. **Concurrency**: show `@Version` and the 409 handler. In psql, `SELECT id, title, version FROM posts;` before and after an update shows the version going up.
6. **Cache** (2 min):
   1. Send "Get post (cached)" twice.
   2. In the terminal, the first `PostService.get(..) success in` shows a few ms, the second under 1 ms.
   3. Run "Update post", then "Get post": the new title appears with no SQL (`@CachePut`).
   4. Optional: restart with `--spring.cache.type=none` and show that the second call is slow again.
7. **Cache consistency** (1 min):
   1. Send "Get post (cached)": note the author's `displayName`.
   2. Run "Update user" with a new `displayName`.
   3. Send "Get post (cached)" again: the post shows the **new** name.
   4. "List comments" also shows the new `authorName`.
8. **Performance report** (1 min): show the before and after tables.
9. **GraphQL** (30 s): in GraphiQL, run block 7 of `docs/examples.graphql`.

---

## 6. Questions the coach may ask

| Question | Short answer |
|---|---|
| Difference between `JpaRepository` and `CrudRepository`? | `CrudRepository` has basic CRUD. `JpaRepository` extends it and adds paging and sorting, `saveAndFlush`, and batch methods. |
| Who creates the tables? | Hibernate, from the entities, because of `spring.jpa.hibernate.ddl-auto=update`. |
| How does `findByTagsName` work without SQL? | Spring Data parses the method name: `tags` is the collection field, `name` is a field of `Tag`. It builds the join and the WHERE clause. |
| Why a native query? | `post_tags` is a join table without an entity, so JPQL cannot reach it. Native SQL can. |
| What is `@Modifying`? | It tells Spring Data that the query changes data (DELETE or UPDATE) instead of reading. |
| What happens if an exception is thrown in a `@Transactional` method? | For a `RuntimeException`, everything done in the method is rolled back. For a checked exception, add `rollbackFor`. |
| `REQUIRED` vs `REQUIRES_NEW`? | `REQUIRED` joins the current transaction. `REQUIRES_NEW` suspends it and starts a separate one that commits on its own. |
| Isolation levels? | READ_UNCOMMITTED, READ_COMMITTED (PostgreSQL default), REPEATABLE_READ, SERIALIZABLE. The higher the level, the safer and the slower. |
| `@Cacheable` vs `@CachePut` vs `@CacheEvict`? | Read from the cache and skip the method / always run the method and update the cache / remove from the cache. |
| Where is the cache stored? | In the application's memory (`spring.cache.type=simple`). It is lost on restart, and it is not shared between several servers (Redis would be used for that). |
| Why EAGER for tags? | The tags are always shown with a post, and it avoids "lazy loading" errors when the post is turned into JSON. `@BatchSize` keeps it efficient. |
| What is `rollbackFor`? | By default only a `RuntimeException` rolls back. `rollbackFor = Exception.class` makes any exception roll back. |
| What happens if the database is down during a write? | Spring cannot start or commit the transaction and throws a `TransactionException`. `ApiErrors` answers 500, "The transaction failed and was rolled back". Nothing is saved. |
| How do you keep the cache consistent? | Every write that changes cached data refreshes it. A post in the cache contains its author, so updating a user also clears the posts cache (`@Caching` with `@CachePut` + `@CacheEvict`). |
| Why `flush()` before writing to MongoDB? | MongoDB is not in the PostgreSQL transaction. `flush()` runs the SQL now, so if PostgreSQL fails we stop before MongoDB is changed. |
| What does `@Version` do? | Optimistic locking: it refuses an update based on an old version of the row, so two people cannot erase each other's changes. |
| Why are comments in MongoDB? | That was the Platform I design (flexible, unstructured data for comments and reviews). Lab 3 says to keep the database "as implemented in Week 4". |

---

## 7. Weak points (know them before the coach finds them)

- **Keyword search is slow on big data.** `LIKE '%word%'` cannot use a normal index, so PostgreSQL reads every post: about 300 ms on 20,000 posts. It is honest in the report. The fix for a large site is PostgreSQL full-text search, which is out of scope for this lab.
- **"Complex JPQL queries optimized".** My JPQL query is simple on purpose. The optimizations I measured are the indexes and `@BatchSize`. If asked, explain the N+1 problem and show the numbers (13 or 53 statements → 4).
- **No comparison with the Week 5 (Platform II) version in the report.** The overview mentions it, but the acceptance criteria do not. If asked: Platform II used hand-written JDBC with no cache; Platform III adds the cache (reads 6 to 12 times faster) and repository paging.
- **No automated tests.** Rollback is shown by hand with Postman. The brief says "verified" and "tested for rollback scenarios", so do the demo live.
- **MongoDB writes are not in the PostgreSQL transaction.** Each comment or review save is atomic on its own, and a method that writes to both always does PostgreSQL first (`flush()`), then MongoDB. If MongoDB failed after PostgreSQL succeeded, the PostgreSQL part would still be rolled back (the exception happens before the commit), but MongoDB changes already made would stay. A full fix needs MongoDB transactions (replica set), which is out of scope.
- **`@CachePut` is an extra.** The brief only asks for `@Cacheable` and `@CacheEvict`; `@CachePut` is used on top for updates.
