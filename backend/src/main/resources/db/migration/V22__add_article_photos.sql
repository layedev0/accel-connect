-- Photo gallery attached to a news article.
--
-- One row per photo, ordered by `position` (the upload order — there is no reorder UI).
-- Statements are idempotent so the script is safe to replay.
--
-- The foreign key carries ON DELETE CASCADE, which Hibernate would not create on its own;
-- ArticleService.deleteArticle also purges this table explicitly so the delete works on
-- databases where only Hibernate built the constraint.

CREATE TABLE IF NOT EXISTS article_photos (
    id           BIGSERIAL PRIMARY KEY,
    article_id   BIGINT       NOT NULL REFERENCES articles (id) ON DELETE CASCADE,
    data         BYTEA        NOT NULL,
    content_type VARCHAR(255) NOT NULL,
    file_name    VARCHAR(255),
    size         BIGINT,
    position     INTEGER      NOT NULL,
    created_at   TIMESTAMP,
    updated_at   TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_article_photo_article ON article_photos (article_id);
