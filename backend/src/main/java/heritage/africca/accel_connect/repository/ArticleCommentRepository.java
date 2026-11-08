package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.ArticleComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ArticleCommentRepository extends JpaRepository<ArticleComment, Long> {

    /** JOIN FETCH is required: without it, mapping N comments triggers N queries on users. */
    @Query("SELECT c FROM ArticleComment c JOIN FETCH c.author WHERE c.article.id = :articleId ORDER BY c.createdAt DESC")
    List<ArticleComment> findByArticleIdWithAuthor(@Param("articleId") Long articleId);

    long countByArticleId(Long articleId);

    void deleteByArticleId(Long articleId);
}
