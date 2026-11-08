package heritage.africca.accel_connect.repository;

import heritage.africca.accel_connect.entity.ArticleLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArticleLikeRepository extends JpaRepository<ArticleLike, Long> {

    boolean existsByArticleIdAndUserId(Long articleId, Long userId);

    long countByArticleId(Long articleId);

    void deleteByArticleIdAndUserId(Long articleId, Long userId);

    void deleteByArticleId(Long articleId);
}
