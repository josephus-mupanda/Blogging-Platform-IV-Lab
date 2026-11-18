-- Blogging Platform III - query plans for the performance report.
-- Run after db/benchmark-data.sql:
--   psql -h localhost -p 5433 -U postgres -d blogging_platform_iii_bench -f db/benchmark.sql
-- Nothing is changed: the "without index" tests drop the index inside a transaction and
-- ROLLBACK puts it back.

-- 1. First page of posts, newest first (the default of GET /api/posts), WITH idx_posts_created_at.
EXPLAIN ANALYZE
SELECT * FROM posts ORDER BY created_at DESC LIMIT 10;

-- 2. The same query WITHOUT the index.
BEGIN;
DROP INDEX idx_posts_created_at;
EXPLAIN ANALYZE
SELECT * FROM posts ORDER BY created_at DESC LIMIT 10;
ROLLBACK;

-- 3. Page 1000 (OFFSET 10000), newest first, with the index.
EXPLAIN ANALYZE
SELECT * FROM posts ORDER BY created_at DESC LIMIT 10 OFFSET 10000;

-- 4. Posts of one author (GET /api/posts/author/{username}), WITH idx_posts_author_id.
EXPLAIN ANALYZE
SELECT p.* FROM posts p JOIN users u ON u.id = p.author_id
WHERE u.username = 'bench-author' ORDER BY p.created_at DESC LIMIT 10;

-- 5. Posts with one tag (GET /api/posts/tag/{name}), WITH idx_post_tags_tag_id.
EXPLAIN ANALYZE
SELECT count(*) FROM posts p JOIN post_tags pt ON pt.post_id = p.id JOIN tags t ON t.id = pt.tag_id
WHERE t.name = 'bench-tag-3';

-- 6. The same count WITHOUT idx_post_tags_tag_id.
BEGIN;
DROP INDEX idx_post_tags_tag_id;
EXPLAIN ANALYZE
SELECT count(*) FROM posts p JOIN post_tags pt ON pt.post_id = p.id JOIN tags t ON t.id = pt.tag_id
WHERE t.name = 'bench-tag-3';
ROLLBACK;

-- 7. Keyword search (the JPQL query). LIKE '%word%' cannot use a normal index, so PostgreSQL
--    reads every post.
EXPLAIN ANALYZE
SELECT count(*) FROM posts
WHERE lower(title) LIKE '%springneedle%' OR lower(body) LIKE '%springneedle%';
