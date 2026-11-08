-- Article now extends DateAudit, so the articles table gains created_at / updated_at.
-- The article detail page uses created_at as the publication date.
--
-- Statements are idempotent: Hibernate's ddl-auto=update also creates these columns,
-- whichever runs first.
--
-- Rows that predate this change keep created_at NULL on purpose — we have no record of
-- when they were published, and stamping them with the migration date would be a lie.
-- The frontend simply omits the date when it is absent.

ALTER TABLE articles ADD COLUMN IF NOT EXISTS created_at TIMESTAMP;
ALTER TABLE articles ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP;
