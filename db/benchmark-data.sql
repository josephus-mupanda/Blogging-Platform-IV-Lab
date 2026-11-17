-- Blogging Platform III - benchmark data.
-- Run against a dedicated benchmark database after db/schema.sql:
--   psql -h localhost -p 5433 -U postgres -d blogging_platform_iii_bench -f db/benchmark-data.sql
-- Safe to run again: rows that already exist are skipped.

INSERT INTO users (username, email, display_name)
VALUES ('bench-author', 'bench-author@example.test', 'Benchmark author')
ON CONFLICT (username) DO NOTHING;

-- 20,000 posts. The word "springneedle" is in 200 of them (every 100th post).
INSERT INTO posts (author_id, title, body, created_at, updated_at)
SELECT u.user_id, 'Benchmark post ' || n,
       CASE WHEN n % 100 = 0 THEN 'springneedle ' ELSE '' END || repeat('Benchmark article content. ', 30),
       timestamp '2025-01-01' + n * interval '1 second',
       timestamp '2025-01-01' + n * interval '1 second'
FROM generate_series(1, 20000) n CROSS JOIN users u
WHERE u.username = 'bench-author'
ON CONFLICT (lower(btrim(title))) DO NOTHING;

-- 10 tags, and every post gets 2 of them.
INSERT INTO tags (name)
SELECT 'bench-tag-' || n FROM generate_series(0, 9) n
ON CONFLICT (name) DO NOTHING;

INSERT INTO post_tags (post_id, tag_id)
SELECT p.post_id, t.tag_id
FROM posts p JOIN tags t
  ON t.name IN ('bench-tag-' || (p.post_id % 10), 'bench-tag-' || ((p.post_id + 1) % 10))
WHERE p.title LIKE 'Benchmark post %'
ON CONFLICT DO NOTHING;

-- Refresh the planner statistics and flush the GIN pending lists before measuring.
VACUUM (ANALYZE) users, posts, tags, post_tags;
