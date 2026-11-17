-- Blogging Platform III - PostgreSQL setup script.
-- Run this once against the database before starting the application:
--   psql -h localhost -p 5433 -U postgres -d blogging_platform_iii -f db/schema.sql
-- The tables are the same as in Platform II. Hibernate only validates them (ddl-auto=validate).
-- Comments and reviews are stored in MongoDB, which creates its collections on first use.

CREATE TABLE IF NOT EXISTS users (
    user_id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255),
    display_name VARCHAR(100) NOT NULL,
    role VARCHAR(10) NOT NULL DEFAULT 'READER' CHECK (role IN ('ADMIN', 'AUTHOR', 'READER')),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS posts (
    post_id BIGSERIAL PRIMARY KEY,
    author_id BIGINT NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL CHECK (trim(title) <> ''),
    body TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS tags (
    tag_id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    deleted_at TIMESTAMPTZ
);

CREATE TABLE IF NOT EXISTS post_tags (
    post_id BIGINT NOT NULL REFERENCES posts (post_id) ON DELETE CASCADE,
    tag_id BIGINT NOT NULL REFERENCES tags (tag_id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, tag_id)
);

-- Indexes that keep searching, filtering, and sorting fast.
-- pg_trgm lets PostgreSQL use an index for "contains" searches (LIKE '%word%').
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE UNIQUE INDEX IF NOT EXISTS uq_posts_normalized_title ON posts (lower(btrim(title)));
-- Lab 3: the JPQL keyword search compares lower(title) and lower(body), so the trigram
-- indexes are built on those same expressions. See docs/performance-report.md.
CREATE INDEX IF NOT EXISTS idx_posts_title_lower_trgm ON posts USING GIN (lower(title) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_posts_body_lower_trgm ON posts USING GIN (lower(body) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_posts_author_id ON posts (author_id);
CREATE INDEX IF NOT EXISTS idx_post_tags_tag_id ON post_tags (tag_id);
CREATE INDEX IF NOT EXISTS idx_posts_visible_created
    ON posts (created_at DESC, post_id DESC) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_posts_archived ON posts (deleted_at DESC) WHERE deleted_at IS NOT NULL;

-- Only needed when reusing an existing Platform I database. On a new database these do nothing.
ALTER TABLE users ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE users ALTER COLUMN role SET DEFAULT 'READER';
ALTER TABLE posts ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
ALTER TABLE tags ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ;
