-- Blogging Platform III - benchmark data (20,000 posts, 10 tags, 2 tags per post).
-- 1. Start the application once on the benchmark database, so Hibernate creates the tables.
-- 2. Run: psql -h localhost -p 5433 -U postgres -d blogging_platform_iii_bench -f db/benchmark-data.sql

INSERT INTO users (username, email, display_name)
VALUES ('bench-author', 'bench-author@example.test', 'Benchmark author')
ON CONFLICT (username) DO NOTHING;

INSERT INTO posts (author_id, title, body, created_at, updated_at, version)
SELECT u.id, 'Benchmark post ' || n,
       CASE WHEN n % 100 = 0 THEN 'springneedle ' ELSE '' END || repeat('Benchmark article content. ', 30),
       timestamp '2025-01-01' + n * interval '1 second',
       timestamp '2025-01-01' + n * interval '1 second',
       0
FROM generate_series(1, 20000) n CROSS JOIN users u
WHERE u.username = 'bench-author'
ON CONFLICT (title) DO NOTHING;

INSERT INTO tags (name)
SELECT 'bench-tag-' || n FROM generate_series(0, 9) n
ON CONFLICT (name) DO NOTHING;

INSERT INTO post_tags (post_id, tag_id)
SELECT p.id, t.id
FROM posts p JOIN tags t
  ON t.name IN ('bench-tag-' || (p.id % 10), 'bench-tag-' || ((p.id + 1) % 10))
WHERE p.title LIKE 'Benchmark post %'
ON CONFLICT DO NOTHING;

VACUUM (ANALYZE) users, posts, tags, post_tags;
