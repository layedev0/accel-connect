package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleRepository extends JpaRepository<Article, Long> {

    /**
     * Newest first, with the articles predating created_at at the end.
     * <p>
     * NULLS LAST has to live in the query: {@code findAll(Sort)} goes through the Criteria
     * API, which has no notion of null precedence and throws
     * {@code UnsupportedOperationException} as soon as a Sort.Order carries one.
     */
    @Query("SELECT a FROM Article a ORDER BY a.createdAt DESC NULLS LAST")
    List<Article> findAllNewestFirst();
}
