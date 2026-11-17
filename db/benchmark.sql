-- Blogging Platform III - query plans for the performance report.
-- Load the data first with db/benchmark-data.sql, then run:
--   psql -h localhost -p 5433 -U postgres -d blogging_platform_iii_bench -f db/benchmark.sql
-- Each query is the SQL that Hibernate generates for PostRepository.search, with the
-- parameter values written in. Nothing in the database is changed by this script.

-- 1. Keyword search, first page of 10 newest matches (Lab 3 indexes in place).
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.* FROM posts p
WHERE p.deleted_at IS NULL
  AND (lower(p.title) LIKE '%springneedle%' ESCAPE '!' OR lower(p.body) LIKE '%springneedle%' ESCAPE '!')
ORDER BY p.created_at DESC, p.post_id DESC
LIMIT 10;

-- 2. The COUNT query that Spring Data runs for the same page (Lab 3 indexes in place).
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(p.post_id) FROM posts p
WHERE p.deleted_at IS NULL
  AND (lower(p.title) LIKE '%springneedle%' ESCAPE '!' OR lower(p.body) LIKE '%springneedle%' ESCAPE '!');

-- 3. The same COUNT without the Lab 3 lower(...) trigram indexes.
-- The indexes are dropped inside a transaction and the transaction is rolled back,
-- so they are back as soon as the query has run.
BEGIN;
DROP INDEX idx_posts_title_lower_trgm, idx_posts_body_lower_trgm;
EXPLAIN (ANALYZE, BUFFERS)
SELECT count(p.post_id) FROM posts p
WHERE p.deleted_at IS NULL
  AND (lower(p.title) LIKE '%springneedle%' ESCAPE '!' OR lower(p.body) LIKE '%springneedle%' ESCAPE '!');
ROLLBACK;

-- 4. Pagination: page 0 of all posts, newest first (size 10).
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.* FROM posts p WHERE p.deleted_at IS NULL
ORDER BY p.created_at DESC, p.post_id DESC LIMIT 10 OFFSET 0;

-- 5. Pagination: page 1000 of all posts, newest first (size 10).
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.* FROM posts p WHERE p.deleted_at IS NULL
ORDER BY p.created_at DESC, p.post_id DESC LIMIT 10 OFFSET 10000;

-- 6. Sorting: page 0 of all posts sorted by title (TITLE_ASC), with idx_posts_visible_title.
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.* FROM posts p WHERE p.deleted_at IS NULL
ORDER BY lower(p.title), p.post_id LIMIT 10;

-- 6b. The same sort without idx_posts_visible_title (dropped and restored by ROLLBACK).
BEGIN;
DROP INDEX idx_posts_visible_title;
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.* FROM posts p WHERE p.deleted_at IS NULL
ORDER BY lower(p.title), p.post_id LIMIT 10;
ROLLBACK;

-- 7. Loading the tags of one page of 10 posts with JOIN FETCH (PostRepository.findWithTags).
EXPLAIN (ANALYZE, BUFFERS)
SELECT DISTINCT p.*, t.* FROM posts p
LEFT JOIN post_tags pt ON p.post_id = pt.post_id
LEFT JOIN tags t ON t.tag_id = pt.tag_id
WHERE p.post_id IN (1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
