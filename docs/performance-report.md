# Performance report

This report measures the three optimizations of Blogging Platform III:

1. **Indexes** declared on the entities.
2. **`@BatchSize`**, which removes the "N+1 queries" problem when a page of posts loads its tags.
3. **Spring Cache**, used for posts, users and tags.

It also measures pagination and sorting.

The benchmark database holds **20,000 posts**, each with **2 of 10 tags**. Measured on 18 November 2026 on one Windows laptop (Java 21, Spring Boot 3.5, PostgreSQL 18, MongoDB), with the app, the databases and the test client on the same machine. Times change a little from run to run, so the database times are given as the range of 5 runs.

## Setup

```powershell
createdb -h localhost -p 5433 -U postgres blogging_platform_iii_bench
$env:DB_POSTGRES_URL = 'jdbc:postgresql://localhost:5433/blogging_platform_iii_bench'
$env:DB_MONGO_URI = 'mongodb://localhost:27017/blogging_platform_iii_bench'
.\mvnw.cmd spring-boot:run          # Hibernate creates the tables
psql -h localhost -p 5433 -U postgres -d blogging_platform_iii_bench -f db/benchmark-data.sql
psql -h localhost -p 5433 -U postgres -d blogging_platform_iii_bench -f db/benchmark.sql
```

- [db/benchmark-data.sql](../db/benchmark-data.sql) inserts the posts and tags.
- [db/benchmark.sql](../db/benchmark.sql) prints the query plans (`EXPLAIN ANALYZE`). To show the "before" plan, it drops an index inside a transaction and then runs `ROLLBACK`, so nothing changes.

---

## 1. Indexes (before and after)

Hibernate creates these indexes from `@Index` in `Post.java`:

- `idx_posts_created_at`: posts are listed newest first by default.
- `idx_posts_author_id`: for the posts of one author.
- `idx_post_tags_tag_id`: for the posts with one tag.

| Query | Without the index | With the index |
|---|---:|---:|
| First page, newest first (`ORDER BY created_at DESC LIMIT 10`) | 17 – 32 ms: reads all 20,000 posts, then sorts them | **0.06 – 0.11 ms**: reads the first 10 entries of `idx_posts_created_at` |
| Count the posts of one tag | 9.6 – 22 ms: reads all of `post_tags` | 7.2 – 19.5 ms: `idx_post_tags_tag_id` |

**What this means.**

- The default list of posts is about **300 times faster** with its index.
- The tag index helps only a little here, because each tag is on 1 post in 10, so PostgreSQL still visits many rows. It helps more for rare tags.

**Keyword search.** `GET /api/posts?keyword=...` runs `LOWER(title) LIKE '%word%'`. A normal index cannot be used when the word can be anywhere in the text, so PostgreSQL reads every post: 250 – 400 ms on 20,000 posts. That is acceptable for a lab-sized blog. A full-text index would be the next step for a large one.

---

## 2. N+1 queries and `@BatchSize`

Each post in a page shows its tags. Without help, Hibernate loads the tags with one extra query per post: 1 query for the page and N for the tags. This is the "N+1 queries" problem. `@BatchSize(size = 50)` on `Post.tags` makes Hibernate load the tags of up to 50 posts in **one** query.

Statements counted with `--logging.level.org.hibernate.SQL=DEBUG`, for one call of `GET /api/posts`:

| Page size | Without `@BatchSize` | With `@BatchSize` |
|---|---:|---:|
| 10 posts | 13 SQL statements | **4** |
| 50 posts | 53 SQL statements | **4** |

The 4 statements are the page, the count for `totalElements`, the author, and one query for all the tags.

---

## 3. Caching (before and after)

The same app was started twice on the same data: once with the cache (`spring.cache.type=simple`, the default), and once with `--spring.cache.type=none` (no cache). Each request was sent 20 times to warm up, then 100 times with timing.

**Inside the service** (median, logged by the `ServiceMonitoring` AOP aspect):

| Method | Without cache | With cache |
|---|---:|---:|
| `PostService.get` (read one post) | 4.68 ms | **0.57 ms** |
| `UserService.get` (read one user) | 2.32 ms | **0.38 ms** |
| `TagService.list` (list tags) | 3.22 ms | **0.25 ms** |

**Whole HTTP request** (typical of 100 calls):

| Request | Without cache | With cache |
|---|---:|---:|
| `GET /api/posts/100` | 10.72 ms | **5.61 ms** |
| `GET /api/users/1` | 6.03 ms | **4.15 ms** |
| `GET /api/tags` | 6.03 ms | **3.51 ms** |

With SQL logging on, calling `GET /api/posts/100` twice sends **one** SQL query: the second answer comes from the cache.

**What this means.** A cached read is 6 to 12 times faster inside the service and about twice as fast for the whole request. The rest of the request time is HTTP and JSON work, which the cache does not change.

---

## 4. Pagination and sorting

| Request (typical of 100 calls) | Time |
|---|---:|
| `GET /api/posts?size=10` (page 0, newest first) | 18.38 ms |
| `GET /api/posts?size=10&page=1000` | 19.95 ms |
| `GET /api/posts?size=10&sort=title,asc` | 12.71 ms |
| `GET /api/posts/tag/bench-tag-3?size=10` | 19.11 ms |

In the database alone, page 0 takes 0.06 – 0.11 ms and page 1000 (`OFFSET 10000`) takes 2.3 – 5.1 ms. `OFFSET` makes PostgreSQL walk past the rows it skips, so deep pages cost a bit more, but only one page of rows is ever sent to Java.

Sorting by title is fast because the unique constraint on `title` is also an index.
