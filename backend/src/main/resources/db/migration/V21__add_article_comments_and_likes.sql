-- Comments and likes on news articles.
--
-- Statements are idempotent: Hibernate's ddl-auto=update also creates these tables from the
-- ArticleComment / ArticleLike entities, whichever runs first.
--
-- Note the ON DELETE CASCADE below: Hibernate creates the foreign keys WITHOUT it, which is
-- why ArticleService.deleteArticle purges both child tables explicitly before deleting the
-- article. Applying this script makes the database enforce it as well.

CREATE TABLE IF NOT EXISTS article_comments (
    id          BIGSERIAL PRIMARY KEY,
    article_id  BIGINT       NOT NULL REFERENCES articles (id) ON DELETE CASCADE,
    author_id   BIGINT       NOT NULL REFERENCES users (id)    ON DELETE CASCADE,
    content     TEXT         NOT NULL,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_article_comment_article ON article_comments (article_id);

CREATE TABLE IF NOT EXISTS article_likes (
    id          BIGSERIAL PRIMARY KEY,
    article_id  BIGINT       NOT NULL REFERENCES articles (id) ON DELETE CASCADE,
    user_id     BIGINT       NOT NULL REFERENCES users (id)    ON DELETE CASCADE,
    created_at  TIMESTAMP,
    updated_at  TIMESTAMP,
    CONSTRAINT uk_article_like UNIQUE (article_id, user_id)
);

CREATE INDEX IF NOT EXISTS idx_article_like_article ON article_likes (article_id);
