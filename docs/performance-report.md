# Performance report

This report answers five questions:

1. **Do the indexes make the Lab 3 queries faster?** Yes. The keyword count is 50 to 100 times faster, and the first page sorted by title is several hundred times faster.
2. **Did rewriting the tag-filter JPQL help?** Yes. Its COUNT query is about 10 times faster.
3. **Does `JOIN FETCH` beat loading tags post by post?** Yes. A page always costs 3 SQL statements instead of 2 + one per post.
4. **How do pagination and sorting perform?** A first page takes well under a millisecond in the database. Page 1000 takes a few milliseconds.
5. **Does the cache help?** Yes. A cached read is about 3 times faster inside the service and about 35% faster over HTTP.

At the end, Lab 3 is compared with the Lab 2 (`JdbcClient`) version on the same data.

The test database holds **20,000 posts**, each with **2 of 10 tags**. The word `springneedle` appears in **200** of them.

---

## Setup (do this once)

```powershell
$env:Path += ";C:\Program Files\PostgreSQL\18\bin"
$env:PGPASSWORD = 'postgres'

createdb -h localhost -p 5433 -U postgres blogging_platform_iii_bench
psql -h localhost -p 5433 -U postgres -d blogging_platform_iii_bench -f db/schema.sql
psql -h localhost -p 5433 -U postgres -d blogging_platform_iii_bench -f db/benchmark-data.sql
```

[db/benchmark-data.sql](../db/benchmark-data.sql) creates the posts, tags and links. [db/benchmark.sql](../db/benchmark.sql) prints the query plans used in Tests 1 to 4. It changes nothing in the database: where it removes an index to show the "before" plan, it does so inside a transaction and then runs `ROLLBACK`, which puts the index back.

To start the app on the benchmark database:

```powershell
$env:DB_POSTGRES_URL = 'jdbc:postgresql://localhost:5433/blogging_platform_iii_bench'
$env:DB_MONGO_URI = 'mongodb://localhost:27017/blogging_platform_iii_bench'
.\mvnw.cmd spring-boot:run
```

The API timings in this report come from the block below. It sends each request 20 times to warm up, then 100 times with timing:

- **typical** is the middle value of the 100 calls.
- **slow** is the value that only 5 of the 100 calls were slower than.

Use `127.0.0.1`, not `localhost`. On Windows, `localhost` is tried as IPv6 first, and every call waits about 200 ms before it falls back.

```powershell
$base = 'http://127.0.0.1:8080'
$paths = [ordered]@{
  'Keyword search, page of 10' = '/api/posts?keyword=springneedle&size=10'
  'Newest posts, page 0'       = '/api/posts?size=10'
  'Newest posts, page 1000'    = '/api/posts?size=10&page=1000'
  'Sorted by title, page 0'    = '/api/posts?size=10&sort=TITLE_ASC'
  'Filter by tag, page 0'      = '/api/posts?tagId=3&size=10'
  'Read one post'              = '/api/posts/100'
  'List tags'                  = '/api/tags'
  'Dashboard'                  = '/api/dashboard'
}
$paths.GetEnumerator() | ForEach-Object {
  $url = $base + $_.Value
  1..20 | ForEach-Object { curl.exe -s -o NUL $url }
  $t = 1..100 | ForEach-Object { [double](curl.exe -s -o NUL -w '%{time_total}' $url) * 1000 } | Sort-Object
  [pscustomobject]@{ Request = $_.Key; 'typical ms' = [math]::Round($t[49], 2); 'slow ms' = [math]::Round($t[94], 2) }
} | Format-Table -AutoSize
```

The database timings come from `EXPLAIN ANALYZE` in `db/benchmark.sql`, run five times:

```powershell
$out = psql -h localhost -p 5433 -U postgres -d blogging_platform_iii_bench -f db/benchmark.sql 2>&1 | Out-String
[regex]::Matches($out, 'Execution Time: ([\d.]+) ms') | ForEach-Object { $_.Groups[1].Value }
```

The numbers move from run to run, because the laptop is busier or quieter and PostgreSQL keeps recent data in memory. This report therefore gives ranges: the lowest and highest of five runs. What does not change between runs is which plan PostgreSQL chooses, and which version is faster.

---

## Test 1: indexes for the queries Lab 3 runs

### Keyword search

`PostRepository.search` is JPQL, and JPQL has no `ILIKE`. So the query compares `lower(p.title) LIKE '%springneedle%'`. An index helps only if it is built on the same expression, so `db/schema.sql` creates two trigram indexes on `lower(title)` and `lower(body)`. The test is the COUNT query that Spring Data runs for every page (`benchmark.sql` queries 2 and 3):

| | How PostgreSQL searched | Time (5 runs) |
|---|---|---:|
| **Before** (Lab 3 indexes removed) | Read all 20,000 posts: `Seq Scan` | 256 – 673 ms |
| **After** | Went straight to the 200 matches: `Bitmap Index Scan` on `idx_posts_title_lower_trgm` and `idx_posts_body_lower_trgm` | 3.8 – 9.1 ms |

**The keyword count is 50 to 100 times faster with the indexes.**

### Sorting by title

The `TITLE_ASC` and `TITLE_DESC` sorts order by `lower(title), post_id`, because `Sort.Order.ignoreCase()` makes Spring Data write `lower(...)`. Lab 2 had no index for this order, so PostgreSQL had to sort all 20,000 posts to return 10. Lab 3 adds `idx_posts_visible_title` (`benchmark.sql` queries 6 and 6b):

| | How PostgreSQL sorted | Time (5 runs) |
|---|---|---:|
| **Before** (index removed) | `Seq Scan`, then `Sort` of every post | 32 – 73 ms |
| **After** | Read the first 10 entries of `idx_posts_visible_title` in order | 0.04 – 0.08 ms |

The other indexes that the Lab 3 queries use were checked in the same plans:

- `idx_posts_visible_created` for the `NEWEST` and `OLDEST` pages.
- `idx_post_tags_tag_id` for the tag filter.
- `posts_pkey` and `post_tags_pkey` for `JOIN FETCH`.
- The unique index on `users.username` for `/api/posts/author/{username}`.
- The unique index on `tags.name` for `/api/posts/tag/{name}`.

---

## Test 2: rewriting the tag-filter JPQL

The first version of the tag filter in `PostRepository.search` was:

```sql
AND (:tagId IS NULL OR EXISTS (SELECT t FROM Tag t
     WHERE t.id = :tagId AND t.deletedAt IS NULL AND t MEMBER OF p.tags))
```

Hibernate turns this into an `EXISTS` inside another `EXISTS`, both referring to the outer post. PostgreSQL ran the subquery once for every one of the 20,000 posts.

The final version selects the IDs of the tagged posts in a subquery that does not refer to the outer post:

```sql
AND (:tagId IS NULL OR p.id IN (SELECT tagged.id FROM Post tagged JOIN tagged.tags t
     WHERE t.id = :tagId AND t.deletedAt IS NULL))
```

PostgreSQL runs it once, uses `idx_post_tags_tag_id`, and keeps the 4,000 IDs in a hash table (`hashed SubPlan`). The COUNT query is `benchmark.sql` 7 and 7b, and the API request is `GET /api/posts?tagId=3&size=10`:

| | COUNT query (5 runs) | Whole request, typical |
|---|---:|---:|
| **Before** (`EXISTS`) | 184 – 334 ms | 190.97 ms |
| **After** (`IN`) | 12.6 – 30 ms | 40.72 ms |

**About 10 times faster in the database, and 4 to 5 times faster for the whole request.**

---

## Test 3: N+1 queries against JOIN FETCH

Each post in a page shows its tags. `Post.tags` is a lazy collection: Hibernate reads it only when the code first touches it. Without care, converting a page of posts to `PostResponse` sends one extra query per post. This is the "N+1 queries" problem: 1 query for the page and N for the tags.

`PostService.toPage` calls `PostRepository.findWithTags(ids)` first. That is one JPQL `JOIN FETCH` query that loads the tags of the whole page at once.

**Method.**

1. Start the app with `--logging.level.org.hibernate.SQL=DEBUG` and count the `select` lines per request.
2. For the "before" numbers, comment out the `findWithTags` call in `PostService.toPage`, then put it back.
3. Time the requests with the timing block above.

| `GET /api/posts?size=…` | SQL statements, N+1 | SQL statements, JOIN FETCH | Typical time, N+1 | Typical time, JOIN FETCH |
|---|---:|---:|---:|---:|
| size 10 | 12 | **3** | 19.72 ms | 19.86 ms |
| size 50 | 52 | **3** | 35.79 ms | 24.67 ms |

The 3 statements are the page query, the COUNT query that Spring Data adds, and the `JOIN FETCH`.

**What this means.** With `JOIN FETCH` the number of statements no longer depends on the page size. At size 10 the time is about the same, because ten small primary-key lookups are cheap on the same machine. At size 50 the difference shows: **about 30% faster**. The gain is bigger when the database is on another machine, because each extra statement then costs a network round trip.

---

## Test 4: pagination and sorting

All post lists use `Pageable`. Spring Data adds `ORDER BY`, `LIMIT` and `OFFSET`, and runs a COUNT query for `totalElements`.

In the database (`benchmark.sql` queries 4 to 6):

| Query | Plan | Time (5 runs) |
|---|---|---:|
| Newest posts, page 0 | `Index Scan` on `idx_posts_visible_created`, stops after 10 rows | 0.03 – 0.05 ms |
| Newest posts, page 1000 | Same index, walks past 10,000 entries first | 2.6 – 4.8 ms |
| Sorted by title, page 0 | `Index Scan` on `idx_posts_visible_title` | 0.04 – 0.08 ms |

Through the API (typical of 100 calls):

| Request | Typical | Slow |
|---|---:|---:|
| `GET /api/posts?size=10` (newest, page 0) | 17.15 ms | 26.35 ms |
| `GET /api/posts?size=10&page=1000` | 22.45 ms | 34.04 ms |
| `GET /api/posts?size=10&sort=TITLE_ASC` | 15.00 ms | 29.88 ms |
| `GET /api/posts/author/bench-author?size=10` (derived query) | 23.20 ms | 34.87 ms |
| `GET /api/posts/author/bench-author?size=10&page=1000` | 32.95 ms | 46.49 ms |
| `GET /api/posts/tag/bench-tag-3?size=10` (derived query) | 22.33 ms | 35.43 ms |
| `GET /api/posts/100/comments?size=10` (MongoDB, derived query) | 7.68 ms | 25.80 ms |

**What this means.**

- **Paging happens in the database.** Only 10 rows travel to Java, whatever the page number.
- **The sort is stable.** Every sort ends with the post ID, so two posts with the same date or title keep a fixed order and never appear on two pages.
- **Deep pages cost a little more.** `OFFSET` makes PostgreSQL walk past every row it skips, so page 1000 costs a few milliseconds more than page 0. This is the same known limit as in Lab 2. For a blog, readers rarely go that deep, so this report accepts it.
- **Most of each request's time is outside the page query.** It goes to the COUNT query, the tag loading and the HTTP and JSON work.

---

## Test 5: caching

The cached methods are `PostService.get`, `UserService.get` and `list`, and `TagService.get` and `list`. To compare, the same app was started twice on the same data:

- with the cache: `spring.cache.type=simple`, the default;
- without the cache: `--spring.cache.type=none`, which makes `@Cacheable` do nothing.

**Inside the service.** This is the median time logged by the `ServiceMonitoring` `@Around` aspect over about 120 calls each. It leaves out the HTTP and JSON cost:

| Method | Without cache | With cache | p95 without | p95 with |
|---|---:|---:|---:|---:|
| `PostService.get` (read one post) | 3.07 ms | **0.93 ms** | 5.77 ms | 2.00 ms |
| `TagService.list` (list tags) | 2.72 ms | **0.82 ms** | 4.13 ms | 2.62 ms |
| `PostService.search` (not cached) | 19.40 ms | 19.28 ms | 51.65 ms | 52.22 ms |

**Through the API** (typical of 100 calls):

| Request | Without cache | With cache |
|---|---:|---:|
| `GET /api/posts/100` | 8.31 ms | **5.45 ms** |
| `GET /api/tags` | 6.58 ms | **5.08 ms** |
| `GET /api/posts?keyword=springneedle` (not cached) | 42.66 ms | 44.28 ms |

With `--logging.level.org.hibernate.SQL=DEBUG`, the first `GET /api/posts/100` prints one `select`. The repeated calls print none: the answer comes from memory.

**What this means.**

- **Cached reads are about 3 times faster inside the service.**
- **Over HTTP they are about 35% faster for a post and 25% faster for the tag list.** The rest of each request (HTTP, JSON, logging) stays the same with or without the cache.
- **Searches are not cached, so they do not change.** That is expected: each combination of filters and pages would need its own entry, and every new post would make the entries out of date.

**Cache invalidation was checked by hand on the dev database:**

- Renaming a tag updates the tag name inside a post that was already cached, because the tag update clears the `posts` cache.
- Deleting a tag removes it from the cached post.
- Editing or deleting a post removes that post's entry.

---

## Comparison with Lab 2

Both versions ran on the same benchmark database, on the same laptop, one after the other, and were measured with the same `curl` loop:

- Lab 2 is the `JdbcClient` version, started from the `Blogging-Platform-II` folder with `--server.port=8081`.
- For a fair test, Lab 2's own trigram indexes on `title` and `body` were added to the benchmark database before measuring.

| Request | Lab 2 (JdbcClient), typical | Lab 3 (Spring Data JPA), typical |
|---|---:|---:|
| Keyword search, page of 10 | 524.59 ms | **48.31 ms** |
| Newest posts, page 0 | **12.97 ms** | 18.11 ms |
| Newest posts, page 1000 | **16.74 ms** | 24.93 ms |
| Sorted by title, page 0 | **10.80 ms** | 16.64 ms |
| Filter by tag, page 0 | **21.30 ms** | 43.46 ms |
| Read one post | 5.56 ms | **4.47 ms** (cached) |
| List tags | 4.99 ms | **4.66 ms** (cached) |
| Dashboard | **16.10 ms** | 17.89 ms |

**What this means, honestly.**

- **Keyword search is about 10 times faster in Lab 3.** On this database, PostgreSQL chose a full table scan for Lab 2's `ILIKE` count query (`Seq Scan`, about 350 ms in `EXPLAIN ANALYZE`). For Lab 3's `lower(...) LIKE` count it used the `lower(...)` trigram indexes (about 4 ms).
- **Plain lists are 5 to 8 ms slower in Lab 3.** Both versions send the same three statements per page. The difference is the work Hibernate does around them: building entities, tracking them in the persistence context, and copying them into response objects. This is the price of not writing SQL and row mappers by hand.
- **The tag filter on `/api/posts` is about 20 ms slower than in Lab 2.** Lab 2 built the SQL text for each request, so its tag condition was a plain `EXISTS` that PostgreSQL turns into a join. Lab 3 uses one fixed JPQL query for every filter combination. The `:tagId IS NULL OR ...` form stops PostgreSQL from making that join, which is why the `IN` rewrite in Test 2 was needed. The dedicated route `/api/posts/tag/{name}` uses a plain join from a derived query and takes 22 ms, the same as Lab 2.
- **Cached reads are faster than Lab 2**, where every read went to the database.

---

## Test conditions

Measured on 12 November 2026 with Java 21, Spring Boot 3.5.16, Hibernate 6, PostgreSQL 18 and MongoDB on Windows 11. The application, both databases and the test client all ran on the same laptop. The `dev` profile was active, so the AOP timing logs were written for every call in both Lab 2 and Lab 3.
