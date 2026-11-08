-- Who published an article.
--
-- Nullable on purpose: articles created before this column existed have no recorded author,
-- and stamping them with someone would be a fabrication. The frontend simply omits the name.
--
-- ON DELETE SET NULL rather than CASCADE: removing a user account must not take their
-- articles with it.

ALTER TABLE articles
    ADD COLUMN IF NOT EXISTS author_id BIGINT REFERENCES users (id) ON DELETE SET NULL;
