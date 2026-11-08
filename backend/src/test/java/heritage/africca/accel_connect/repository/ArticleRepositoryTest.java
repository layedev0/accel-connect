package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.Article;
import heritage.africca.accel_connect.entity.enumeration.ArticleType;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Runs the query for real. A service test with a mocked repository cannot catch a query that
 * fails at execution — which is exactly how the Sort-based version of this ordering shipped
 * broken.
 */
@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ArticleRepository")
class ArticleRepositoryTest {

    @Autowired
    private ArticleRepository repository;

    @Autowired
    private EntityManager entityManager;

    private Article save(String title, LocalDateTime createdAt) {
        Article article = new Article();
        article.setTitle(title);
        article.setContent("Contenu");
        article.setArticleType(ArticleType.FORMATION);
        Article saved = repository.save(article);

        // DateAudit's @PrePersist fills createdAt when it is null, so an "undated" row can
        // only be produced by clearing it after the insert.
        saved.setCreatedAt(createdAt);
        repository.saveAndFlush(saved);
        entityManager.clear();
        return saved;
    }

    @Test
    @DisplayName("findAllNewestFirst returns newest first and puts undated articles last")
    void ordersNewestFirstThenUndated() {
        save("ancien", LocalDateTime.of(2026, 3, 1, 9, 0));
        save("sans date", null);
        save("recent", LocalDateTime.of(2026, 8, 24, 9, 0));

        List<Article> ordered = repository.findAllNewestFirst();

        assertEquals(List.of("recent", "ancien", "sans date"),
                ordered.stream().map(Article::getTitle).toList(),
                "PostgreSQL and H2 both default to NULLS FIRST on DESC: the query must say otherwise");
    }

    @Test
    @DisplayName("findAllNewestFirst returns an empty list when there is no article")
    void returnsEmptyListWhenNoArticle() {
        assertTrue(repository.findAllNewestFirst().isEmpty());
    }
}
