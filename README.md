# Blogging Platform III

A Java 21 and Spring Boot 3.5 backend for a blogging platform. It continues Blogging Platform II (same REST and GraphQL APIs, same layers, same profiles) and moves the data access to **Spring Data JPA**, with pagination, sorting, transactions and caching.

- **PostgreSQL** stores users, posts and tags. **Hibernate creates the tables** from the entity classes.
- **MongoDB** stores comments and reviews, as in Platform II.

## How the requirements are met

| Requirement | Where to look |
|---|---|
| Spring Data JPA configured | `spring-boot-starter-data-jpa` in [pom.xml](pom.xml), `spring.jpa.hibernate.ddl-auto=update` in [application.properties](src/main/resources/application.properties) |
| Entities with `@Entity`, `@Id` and relationships | `entity/User`, `entity/Post`, `entity/Tag` (`@ManyToOne`, `@ManyToMany`) |
| Repositories extend `JpaRepository` / `CrudRepository` | `repository/` (`MongoRepository` extends `CrudRepository`) |
| Derived queries | `PostRepository.findByAuthorUsername`, `PostRepository.findByTagsName` |
| JPQL query | `PostRepository.searchByKeyword` |
| Native SQL query | `TagRepository.removeTagFromPosts` |
| Pagination and sorting with `Pageable` | `GET /api/posts`, `GET /api/posts/{postId}/comments` |
| `@Transactional`, propagation, isolation, rollback | `service/PostService` (`create`, `update`), `entity/Post` (`@Version`) |
| Caching with `@EnableCaching`, `@Cacheable`, `@CachePut`, `@CacheEvict` | `BloggingPlatformIiiApplication`, `PostService`, `UserService`, `TagService` |
| Performance measured | [docs/performance-report.md](docs/performance-report.md) |

## Code structure

`Controller → Service → Repository`, as in Platform II.

| Package | What it contains |
|---|---|
| `controller.rest`, `controller.graphql` | REST and GraphQL endpoints. They call the services. |
| `service` | Business rules, `@Transactional` and the cache annotations. |
| `repository` | Spring Data interfaces. Spring writes their implementation at startup. |
| `entity` | The classes stored in the databases. The API returns them directly. |
| `dto` | The request bodies, with Bean Validation (`@NotBlank`, `@Email`...). |
| `config` | OpenAPI, GraphQL and the AOP logging aspect (`ServiceMonitoring`). |

Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`) writes the getters, setters and empty constructors of the entities.

## Entities and relationships

| Entity | Stored in | Relationships |
|---|---|---|
| `User` | table `users` | — |
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
- `spring.data.web.pageable.max-page-size=100` limits the page size.
- The repository adds `ORDER BY`, `LIMIT` and `OFFSET` to the SQL, so only one page leaves the database.
- The response contains the posts and the page information:

```json
{ "content": [ ... ], "page": { "size": 10, "number": 0, "totalElements": 42, "totalPages": 5 } }
```

## Transactions

| Where | What it does |
|---|---|
| `@Transactional` on every service method that writes | All the SQL of the method is one unit: it is committed at the end, or rolled back if an exception is thrown. |
| `PostService.create`: `propagation = Propagation.REQUIRED` | Joins the caller's transaction if there is one, otherwise starts a new one (this is the default). |
| `PostService.update`: `isolation = Isolation.READ_COMMITTED` | The transaction only reads data that other transactions have already committed (no "dirty reads"). |
| `@Version` in `Post` | If two people save the same post at the same moment, Hibernate refuses the second save, so one change cannot silently erase the other. The API answers **409**. |

**Rollback demo (Postman: "Rollback demo").** `PostService.create` first inserts the post, then adds its tags. Create a post with a tag ID that does not exist, for example `"tagIds": [1, 999999]`:

1. The INSERT of the post runs.
2. Tag 999999 is not found, so a `BlogException` is thrown.
3. Spring rolls the transaction back, and the post that was just inserted disappears.

The API answers 404, and `GET /api/posts` shows that no post was added.

**MongoDB.** Comments and reviews are not covered by the PostgreSQL transaction. Each comment or review save changes one document, and MongoDB saves one document atomically.

## Caching

`@EnableCaching` on `BloggingPlatformIiiApplication` turns caching on. `spring.cache.type=simple` keeps the values in memory.

| Cache | Filled by `@Cacheable` | Replaced by `@CachePut` | Emptied by `@CacheEvict` |
|---|---|---|---|
| `posts` | `PostService.get(id)` (frequently read posts) | `PostService.update` | deleting the post; renaming or deleting a tag |
| `users` | `UserService.get(id)` | `UserService.update` | deleting the user |
| `tags` | `TagService.list()` | — | creating, renaming or deleting a tag |

- `@Cacheable`: if the value is in the cache, the method is skipped.
- `@CachePut`: the method always runs, and its result replaces the cached value.
- `@CacheEvict`: removes the cached value.

**To see it work:** call `GET /api/posts/1` twice. The console (dev profile) shows `PostService.get(..) success in` about 5 ms the first time and under 1 ms the second time. Start the app with `--spring.cache.type=none` to compare without the cache.

## Run locally

Install JDK 21, PostgreSQL and MongoDB. Create an empty database once, then start the app; Hibernate creates the tables.

```powershell
$env:Path += ";C:\Program Files\PostgreSQL\18\bin"
createdb -h localhost -p 5433 -U postgres blogging_platform_iii
.\mvnw.cmd spring-boot:run
```

| Profile | PostgreSQL | MongoDB | GraphiQL |
|---|---|---|---|
| `dev` (default) | `localhost:5433/blogging_platform_iii` | `localhost:27017/blogging_platform_iii` | on |
| `test` | `localhost:5433/blogging_platform_iii_test` | `localhost:27017/blogging_platform_iii_test` | on |
| `prod` | from `DB_POSTGRES_URL`, `DB_POSTGRES_USER`, `DB_POSTGRES_PASSWORD`, `DB_MONGO_URI` | | off |

## Testing the API

- **Swagger UI:** [localhost:8080/swagger-ui/index.html](http://localhost:8080/swagger-ui/index.html) lists every endpoint, with the `page`, `size` and `sort` parameters.
- **Postman:** import [docs/postman_collection.json](docs/postman_collection.json) and run the folders from top to bottom.
- **GraphiQL:** [localhost:8080/graphiql](http://localhost:8080/graphiql), with the examples in [docs/examples.graphql](docs/examples.graphql).

| Routes | Operations |
|---|---|
| `/api/users`, `/api/users/{id}` | create, list, read, update, delete users |
| `/api/posts`, `/api/posts/{id}` | list (paginated, optional `keyword`), read, create, update, delete posts |
| `/api/posts/author/{username}` | posts of one author, paginated |
| `/api/posts/tag/{name}` | posts with one tag, paginated |
| `/api/tags`, `/api/tags/{id}` | create, list, read, rename, delete tags |
| `/api/posts/{postId}/comments` | list (paginated), add, edit, delete comments |
| `/api/posts/{postId}/reviews` | list and add reviews (one per user and post) |
| `/api/dashboard` | number of users, posts, tags, comments and reviews |

Every REST response has `status`, `message` and `data`.
